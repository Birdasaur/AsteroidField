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
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;

/**
 * Skybox rendered as 6 textured quads arranged as a cube that follows the camera's position.
 *
 * Face mapping (atlas SINGLE path assumes 4x3 cross):
 *   front  = +Z  (row 1, col 1)
 *   back   = -Z  (row 1, col 3)
 *   right  = +X  (row 1, col 2)
 *   left   = -X  (row 1, col 0)
 *   top    = +Y  (row 0, col 1)
 *   bottom = -Y  (row 2, col 1)
 */
public class Skybox extends Group {

    public enum SkyboxImageType { MULTIPLE, SINGLE }

    // Follows the camera's translation so the sky stays "infinitely" far.
    private final Affine affine = new Affine();

    // Faces (ImageViews act as textured quads)
    private final ImageView
            top    = new ImageView(),
            bottom = new ImageView(),
            left   = new ImageView(),
            right  = new ImageView(),
            back   = new ImageView(),
            front  = new ImageView();

    // Convenient iteration order (doesn't affect rendering order with depth test on)
    private final ImageView[] views = new ImageView[]{ top, left, back, right, front, bottom };

    // MULTIPLE images
    private Image topImg, bottomImg, leftImg, rightImg, frontImg, backImg;

    // SINGLE atlas
    private Image singleImg;

    private final PerspectiveCamera camera;
    private AnimationTimer timer;
    private final SkyboxImageType imageType;

    /** SINGLE-image (4x3 cross) */
    public Skybox(Image singleImg, double size, PerspectiveCamera camera) {
        super();
        this.imageType = SkyboxImageType.SINGLE;
        this.singleImg = singleImg;
        this.size.set(size);
        this.camera = camera;

        getTransforms().add(affine);
        loadImageViews();
        getChildren().addAll(views);
        startTimer();
    }

    /** MULTIPLE-images */
    public Skybox(Image topImg, Image bottomImg, Image leftImg, Image rightImg,
                  Image frontImg, Image backImg, double size, PerspectiveCamera camera) {
        super();
        this.imageType = SkyboxImageType.MULTIPLE;

        this.topImg = topImg;
        this.bottomImg = bottomImg;
        this.leftImg = leftImg;
        this.rightImg = rightImg;
        this.frontImg = frontImg; // +Z
        this.backImg = backImg;   // -Z

        this.size.set(size);
        this.camera = camera;

        loadImageViews();
        getTransforms().add(affine);
        getChildren().addAll(views);
        startTimer();
    }

    // ---------------------------------------------------------------------
    // Setup
    // ---------------------------------------------------------------------

    private void loadImageViews() {
        for (ImageView iv : views) {
            iv.setSmooth(false);          // crisp sampling while we validate viewports
            iv.setPreserveRatio(false);   // exact SxS quads
            iv.setMouseTransparent(true);
            iv.setDepthTest(DepthTest.ENABLE);
        }
        validateImageType();
    }

    /**
     * Positions and orients each face so the cube is centered at the origin,
     * then moves each face ±S/2 along its inward normal (tiny inward EPS overlap).
     * Uses center-pivot rotations for precise alignment.
     */
    private void layoutViews() {
        final double S = getSize();

        // Size and center each quad so its center is at (0,0) in local space
        for (ImageView iv : views) {
            iv.setFitWidth(S);
            iv.setFitHeight(S);
            iv.setTranslateX(-S / 2.0);
            iv.setTranslateY(-S / 2.0);
            iv.getTransforms().setAll(); // clear prior transforms
        }

        final double px = S / 2.0, py = S / 2.0;
        final double EPS = 0.01; // tiny inward overlap to eliminate hairlines

        // ==== Orientation preset that worked for your build (Preset A) ====

        // back (-Z) : inward-facing (+Z) -> rotate 180° about Y, push -Z
        back.getTransforms().addAll(
            new Rotate(180, px, py, 0, Rotate.Y_AXIS),
            new javafx.scene.transform.Translate(0, 0, -S/2 + EPS)
        );

        // front (+Z) : inward-facing (-Z) -> no Y-rotation, push +Z
        front.getTransforms().addAll(
            new Rotate(0, px, py, 0, Rotate.Y_AXIS),
            new javafx.scene.transform.Translate(0, 0, +S/2 - EPS)
        );

        // left (-X) : inward-facing (+X) -> -90° about Y, push -X
        left.getTransforms().addAll(
            new Rotate(-90, px, py, 0, Rotate.Y_AXIS),
            new javafx.scene.transform.Translate(-S/2 + EPS, 0, 0)
        );

        // right (+X) : inward-facing (-X) -> +90° about Y, push +X
        right.getTransforms().addAll(
            new Rotate(90, px, py, 0, Rotate.Y_AXIS),
            new javafx.scene.transform.Translate(+S/2 - EPS, 0, 0)
        );

        // top (+Y) : inward-facing (-Y) -> -90° about X, push +Y
        top.getTransforms().addAll(
            new Rotate(-90, px, py, 0, Rotate.X_AXIS),
            new javafx.scene.transform.Translate(0, +S/2 - EPS, 0)
        );

        // bottom (-Y) : inward-facing (+Y) -> +90° about X, push -Y
        bottom.getTransforms().addAll(
            new Rotate(90, px, py, 0, Rotate.X_AXIS),
            new javafx.scene.transform.Translate(0, -S/2 + EPS, 0)
        );
    }

    /**
     * For SINGLE atlas: create viewports and assign the single image.
     * For MULTIPLE: assign one image per face.
     */
    private void validateImageType() {
        switch (imageType) {
            case SINGLE -> loadSingleImageViewports();
            case MULTIPLE -> setMultipleImages();
        }
    }

    /**
     * SINGLE (atlas) mode: exact, integer-aligned full-cell viewports.
     * Mapping:
     *   row 0: [ top ] at (1,0)
     *   row 1: [left][front][right][back] at (0..3,1)
     *   row 2: [bottom] at (1,2)
     */
    private void loadSingleImageViewports() {
        layoutViews();

        final double w = singleImg.getWidth();
        final double h = singleImg.getHeight();

        // integer cell sizes to avoid fractional sampling/cropping
        final int cellW = (int)Math.round(w / 4.0);
        final int cellH = (int)Math.round(h / 3.0);

        if (cellW <= 0 || cellH <= 0 || cellW != cellH) {
            throw new IllegalArgumentException(
                "Atlas must be a 4x3 grid of equal square cells. Got cellW=" + cellW + " cellH=" + cellH);
        }
        final int cell = cellW;

        // (optional) keep size a multiple of cell for nice round numbers
        recalculateSize(cell);

        // Full, symmetric rectangles (no inset/bleed in code)
        top.setViewport   (new Rectangle2D(1L * cell, 0L * cell, cell, cell));
        left.setViewport  (new Rectangle2D(0L * cell, 1L * cell, cell, cell));
        front.setViewport (new Rectangle2D(1L * cell, 1L * cell, cell, cell)); // +Z
        right.setViewport (new Rectangle2D(2L * cell, 1L * cell, cell, cell));
        back.setViewport  (new Rectangle2D(3L * cell, 1L * cell, cell, cell)); // -Z
        bottom.setViewport(new Rectangle2D(1L * cell, 2L * cell, cell, cell));

        for (ImageView v : views) v.setImage(singleImg);
    }

    private void recalculateSize(double cell) {
        double factor = Math.floor(getSize() / cell);
        if (factor < 1) factor = 1;
        setSize(cell * factor);
    }

    private void setMultipleImages() {
        layoutViews();

        front.setImage(frontImg);
        back.setImage(backImg);
        top.setImage(topImg);
        bottom.setImage(bottomImg);
        left.setImage(leftImg);
        right.setImage(rightImg);
    }

    // ---------------------------------------------------------------------
    // Camera follow
    // ---------------------------------------------------------------------

    private void startTimer() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                Transform ct = (camera != null) ? camera.getLocalToSceneTransform() : null;
                if (ct != null) {
                    affine.setTx(ct.getTx());
                    affine.setTy(ct.getTy());
                    affine.setTz(ct.getTz());
                }
            }
        };
        timer.start();
    }

    /** Stop the internal animation timer (call when tearing down). */
    public void stop() {
        if (timer != null) timer.stop();
    }

    // ---------------------------------------------------------------------
    // Properties
    // ---------------------------------------------------------------------

    private final DoubleProperty size = new SimpleDoubleProperty() {
        @Override
        protected void invalidated() { layoutViews(); }
    };

    public final double getSize() { return size.get(); }
    public final void setSize(double value) { size.set(value); }
    public DoubleProperty sizeProperty() { return size; }
}
