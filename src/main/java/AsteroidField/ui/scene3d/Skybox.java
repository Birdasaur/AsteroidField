package AsteroidField.ui.scene3d;

import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Rectangle2D;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Transform;

/**
 * Cube skybox that follows the camera position.
 *
 * SINGLE expects a 4x3 horizontal cross atlas:
 *
 *           [  top  ]      (row 0, col 1)
 * [ left ][ front ][ right ][ back ]  (row 1, col 0..3)
 *           [ bottom ]      (row 2, col 1)
 *
 * Face mapping here:
 *   front  = +Z (atlas row 1 col 1), inward normal -Z
 *   back   = -Z (atlas row 1 col 3), inward normal +Z
 *   right  = +X (atlas row 1 col 2), inward normal -X
 *   left   = -X (atlas row 1 col 0), inward normal +X
 *   top    = +Y (atlas row 0 col 1), inward normal -Y  (up points toward -Z)
 *   bottom = -Y (atlas row 2 col 1), inward normal +Y  (up points toward +Z)
 */
public class Skybox extends Group {

    public enum SkyboxImageType { MULTIPLE, SINGLE }

    private final Affine follow = new Affine(); // follows camera translation

    private final ImageView
            top    = new ImageView(),
            bottom = new ImageView(),
            left   = new ImageView(),
            right  = new ImageView(),
            back   = new ImageView(),
            front  = new ImageView();

    private final ImageView[] views = new ImageView[]{ top, left, back, right, front, bottom };

    // MULTIPLE images
    private Image topImg, bottomImg, leftImg, rightImg, frontImg, backImg;

    // SINGLE atlas
    private Image singleImg;

    private final PerspectiveCamera camera;
    private AnimationTimer timer;
    private final SkyboxImageType imageType;

    public Skybox(Image singleImg, double size, PerspectiveCamera camera) {
        this.imageType = SkyboxImageType.SINGLE;
        this.singleImg = singleImg;
        this.size.set(size);
        this.camera = camera;

        getTransforms().add(follow);
        loadImageViews();
        getChildren().addAll(views);
        startTimer();
    }

    public Skybox(Image topImg, Image bottomImg, Image leftImg, Image rightImg,
                  Image frontImg, Image backImg, double size, PerspectiveCamera camera) {
        this.imageType = SkyboxImageType.MULTIPLE;

        this.topImg = topImg;
        this.bottomImg = bottomImg;
        this.leftImg = leftImg;
        this.rightImg = rightImg;
        this.frontImg = frontImg;
        this.backImg = backImg;

        this.size.set(size);
        this.camera = camera;

        loadImageViews();
        getTransforms().add(follow);
        getChildren().addAll(views);
        startTimer();
    }

    // ---------------------------------------------------------------------
    // Setup
    // ---------------------------------------------------------------------

    private void loadImageViews() {
        for (ImageView iv : views) {
            iv.setSmooth(true);          // keep nearest while validating
            iv.setPreserveRatio(true);   // exact SxS faces
            iv.setMouseTransparent(true);
//            iv.setDepthTest(DepthTest.ENABLE);
        }
        validateImageType();
    }

    // ---------------------------------------------------------------------
    // Core layout/orientation
    // ---------------------------------------------------------------------

private void layoutViews() {
    final double S    = getSize();
    final double half = S / 2.0;
    final double EPS  = 0.01;

    for (ImageView iv : views) {
        iv.setFitWidth(S);
        iv.setFitHeight(S);
        iv.getTransforms().setAll();  // we'll install one Affine per face
    }

    // Canonical outward normals and ups (keep RIGHT and BACK as you have them)
    orientFace(front,   vec( 0,  0,  1), vec( 0,  1,  0), half, EPS); // center -> (0, 0, +half)
    orientFace(back,  vec( 0,  0, -1), vec( 0,  1,  0), half, EPS); // center -> (0, 0, -half)
    orientFace(right,  vec( 1,  0,  0), vec( 0,  1,  0), half, EPS); // center -> (+half, 0, 0)
    orientFace(left,   vec(-1,  0,  0), vec( 0,  1,  0), half, EPS); // center -> (-half, 0, 0)

    // For top/bottom, choose ups so seams align with front/back
//    orientFace(top,    vec( 0,  1,  0), vec( 0,  0, -1), half, EPS); // center -> (0, +half, 0)
//    orientFace(bottom, vec( 0, -1,  0), vec( 0,  0,  1), half, EPS); // center -> (0, -half, 0)
    
orientFace(bottom,    vec( 0, -1,  0), vec( 0,  0, -1), half, EPS);
orientFace(top, vec( 0,  1,  0), vec( 0,  0,  1), half, EPS);

}


private void debugFaceCenters() {
    System.out.println("— face centers (scene coords) —");
    logCenter("BACK  ", back);
    logCenter("FRONT ", front);
    logCenter("RIGHT ", right);
    logCenter("LEFT  ", left);
    logCenter("TOP   ", top);
    logCenter("BOTTOM", bottom);
}

private void logCenter(String name, ImageView iv) {
    // Center of the quad: because we first translate by (-half,-half,0), local (0,0,0) is face center
    var p = iv.localToScene(0, 0, 0);
    System.out.printf("%s : (%.2f, %.2f, %.2f)%n", name, p.getX(), p.getY(), p.getZ());
}

    /**
     * Apply a single Affine to 'face' that:
     *  - rotates the local XY plane (normal +Z, up +Y) so its normal = 'normal' and up = 'up'
     *  - then translates along 'normal' by 'push' units (push > 0)
     */
private void orientFace(ImageView face, Vec3 normal, Vec3 up, double half, double eps) {
    // Desired outward normal (from origin to face center) and a face-up
    Vec3 n = normal.normalized();
    Vec3 u = up.normalized();

    // Right-handed orthonormal basis: r, u', n
    Vec3 r = u.cross(n).normalized();
    u = n.cross(r).normalized(); // re-orthogonalize up

    // Our local quad coordinates range from (0..S, 0..S) in X/Y with Z=0.
    // We want the quad's CENTER (S/2,S/2,0) to land at world position n*(half - eps),
    // and local axes to align with r (X), u (Y), n (Z).
    // So world(P) = R * P + T, with columns of R = (r, u, n) and
    // T = n*(half - eps) - R * (S/2, S/2, 0)^T = n*(half - eps) - r*half - u*half
    Vec3 t = n.scale(half - eps)
             .minus(r.scale(half))
             .minus(u.scale(half));

    // JavaFX Affine (mxx,mxy,mxz,tx,  myx,myy,myz,ty,  mzx,mzy,mzz,tz)
    Affine A = new Affine(
        r.x, u.x, n.x, t.x,
        r.y, u.y, n.y, t.y,
        r.z, u.z, n.z, t.z
    );

    face.getTransforms().setAll(A);  // single, atomic transform = no order issues
}


    // ---------------------------------------------------------------------
    // SINGLE / MULTIPLE
    // ---------------------------------------------------------------------

    private void validateImageType() {
        switch (imageType) {
            case SINGLE -> loadSingleImageViewports();
            case MULTIPLE -> setMultipleImages();
        }
    }

    private void loadSingleImageViewports() {
        layoutViews();

        final int W = (int)Math.round(singleImg.getWidth());
        final int H = (int)Math.round(singleImg.getHeight());
        final int cell = W / 4;
        if (cell <= 0 || cell != H / 3 || W != cell * 4 || H != cell * 3) {
            throw new IllegalArgumentException("Atlas must be exact 4x3 grid of equal squares. Got: "
                    + W + "x" + H);
        }

        top.setViewport   (new Rectangle2D(1L * cell, 0L * cell, cell, cell));
        left.setViewport  (new Rectangle2D(0L * cell, 1L * cell, cell, cell));
        front.setViewport (new Rectangle2D(1L * cell, 1L * cell, cell, cell));
        right.setViewport (new Rectangle2D(2L * cell, 1L * cell, cell, cell));
        back.setViewport  (new Rectangle2D(3L * cell, 1L * cell, cell, cell));
        bottom.setViewport(new Rectangle2D(1L * cell, 2L * cell, cell, cell));

        for (ImageView v : views) v.setImage(singleImg);
    }

    private void setMultipleImages() {
        layoutViews();
        front.setImage(frontImg);
        back.setImage(backImg);
        top.setImage(topImg);
        bottom.setImage(bottomImg);
        left.setImage(leftImg);
        right.setImage(rightImg);
        debugFaceCenters();
    }

    // ---------------------------------------------------------------------
    // Follow camera
    // ---------------------------------------------------------------------

    private void startTimer() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                Transform ct = (camera != null) ? camera.getLocalToSceneTransform() : null;
                if (ct != null) {
                    follow.setTx(ct.getTx());
                    follow.setTy(ct.getTy());
                    follow.setTz(ct.getTz());
                }
            }
        };
        timer.start();
    }

    public void stop() { if (timer != null) timer.stop(); }

    // ---------------------------------------------------------------------
    // Size property
    // ---------------------------------------------------------------------

    private final DoubleProperty size = new SimpleDoubleProperty() {
        @Override protected void invalidated() { layoutViews(); }
    };

    public final double getSize() { return size.get(); }
    public final void setSize(double value) { size.set(value); }
    public DoubleProperty sizeProperty() { return size; }

    // ---------------------------------------------------------------------
    // Small vector helper
    // ---------------------------------------------------------------------

    private static Vec3 vec(double x, double y, double z) { return new Vec3(x, y, z); }

private static final class Vec3 {
    final double x, y, z;
    Vec3(double x, double y, double z) { this.x = x; this.y = y; this.z = z; }

    double len() { return Math.sqrt(x*x + y*y + z*z); }

    Vec3 normalized() {
        double L = len();
        return (L == 0) ? this : new Vec3(x / L, y / L, z / L);
    }

    Vec3 cross(Vec3 o) {
        return new Vec3(
            y * o.z - z * o.y,
            z * o.x - x * o.z,
            x * o.y - y * o.x
        );
    }

    // --- add these ---
    Vec3 scale(double s) { return new Vec3(x * s, y * s, z * s); }

    Vec3 minus(Vec3 o) { return new Vec3(x - o.x, y - o.y, z - o.z); }
}

}
