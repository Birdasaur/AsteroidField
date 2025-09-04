package AsteroidField.ui;

import javafx.animation.AnimationTimer;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.SubScene;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.PerspectiveCamera;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

/**
 * Dependency-free mini camera view:
 * - Snapshots the SubScene's root Group using an off-graph PerspectiveCamera.
 * - Uses depth buffer and a per-frame viewport matching fitWidth/fitHeight.
 * - Offers a small transform rig (t, rx, ry, rz) like CameraTransformer.
 */
public final class CameraViewLite extends ImageView {

    private final SnapshotParameters params = new SnapshotParameters();
    private WritableImage image = null;

    // Off-graph camera & simple rig
    private final PerspectiveCamera camera = new PerspectiveCamera(true);
    private final Translate t = new Translate(0, 0, 0);
    private final Rotate rx = new Rotate(0, 0, 0, 0, Rotate.X_AXIS);
    private final Rotate ry = new Rotate(0, 0, 0, 0, Rotate.Y_AXIS);
    private final Rotate rz = new Rotate(0, 0, 0, 0, Rotate.Z_AXIS);

    private final Group worldToView;
    private final Group rig = new Group();

    private AnimationTimer viewTimer;

    // For optional first-person navigation (if you enable it)
    private double mousePosX, mousePosY, mouseOldX, mouseOldY, mouseDeltaX, mouseDeltaY;
// CameraViewLite.java  (additions)
private final java.util.List<javafx.scene.Node> snapshotOnlyNodes = new java.util.ArrayList<>();

    public CameraViewLite(SubScene scene) {
        // Require root to be a Group (matches your original)
        if (!(scene.getRoot() instanceof Group)) {
            throw new IllegalArgumentException("CameraViewLite requires a Group as SubScene root");
        }
        this.worldToView = (Group) scene.getRoot();

        // Build off-graph rig: [t, rz, ry, rx] -> camera
        rig.getTransforms().addAll(t, rz, ry, rx);
        rig.getChildren().add(camera);

        camera.setNearClip(0.1);
        camera.setFarClip(15000.0);
        camera.setTranslateZ(-1500);   // default working distance
        // camera.setFieldOfView(60);  // optionally widen; can be set from outside

        params.setCamera(camera);
        params.setDepthBuffer(true);
        params.setFill(Color.rgb(0, 0, 0, 0)); // transparent HUD; use BLACK if you prefer

        viewTimer = new AnimationTimer() {
            @Override public void handle(long now) { redraw(); }
        };
    }

    /** Begin per-frame snapshotting. */
    public void startViewing() { viewTimer.start(); }

    /** Stop per-frame snapshotting. */
    public void pause() { viewTimer.stop(); }

    /** Optional: WASD + drag controls on this ImageView. */
    public void setFirstPersonNavigationEnabled(boolean enabled) {
        if (enabled) {
            setMouseTransparent(false);

            setOnKeyPressed(event -> {
                double change = event.isShiftDown() ? 50.0 : 10.0;
                KeyCode key = event.getCode();
                if (key == KeyCode.W) camera.setTranslateZ(camera.getTranslateZ() + change);
                if (key == KeyCode.S) camera.setTranslateZ(camera.getTranslateZ() - change);
                if (key == KeyCode.A) camera.setTranslateX(camera.getTranslateX() - change);
                if (key == KeyCode.D) camera.setTranslateX(camera.getTranslateX() + change);
            });

            setOnScroll((ScrollEvent e) -> {
                double change = (e.isShiftDown() ? 2.0 : 1.0) * e.getDeltaY();
                camera.setTranslateZ(camera.getTranslateZ() + change);
            });

            setOnMousePressed((MouseEvent me) -> {
                mousePosX = mouseOldX = me.getSceneX();
                mousePosY = mouseOldY = me.getSceneY();
                requestFocus();
            });

            setOnMouseDragged((MouseEvent me) -> {
                mouseOldX = mousePosX; mouseOldY = mousePosY;
                mousePosX = me.getSceneX(); mousePosY = me.getSceneY();
                mouseDeltaX = (mousePosX - mouseOldX);
                mouseDeltaY = (mousePosY - mouseOldY);

                double mod = me.isShiftDown() ? 50.0 : 10.0;
                double f   = 0.1;

                if (me.isPrimaryButtonDown()) {
                    ry.setAngle(wrapAngle(ry.getAngle() + mouseDeltaX * f * mod * 2.0));
                    rx.setAngle(wrapAngle(rx.getAngle() - mouseDeltaY * f * mod * 2.0));
                } else if (me.isSecondaryButtonDown()) {
                    camera.setTranslateZ(camera.getTranslateZ() + mouseDeltaX * f * mod);
                } else if (me.isMiddleButtonDown()) {
                    t.setX(t.getX() + mouseDeltaX * f * mod * 0.3);
                    t.setY(t.getY() + mouseDeltaY * f * mod * 0.3);
                }
            });
        } else {
            setOnMouseDragged(null);
            setOnScroll(null);
            setOnMousePressed(null);
            setOnKeyPressed(null);
            setMouseTransparent(true);
        }
    }

    private static double wrapAngle(double a) {
        return ((a % 360) + 540) % 360 - 180;
    }

public void setSnapshotOnlyNodes(javafx.scene.Node... nodes) {
    snapshotOnlyNodes.clear();
    if (nodes != null) java.util.Collections.addAll(snapshotOnlyNodes, nodes);
}

private void redraw() {
    double w = getFitWidth(), h = getFitHeight();
    if (w < 2 || h < 2) return;

    params.setViewport(new javafx.geometry.Rectangle2D(0, 0, w, h));

    // --- show craft only for this snapshot ---
    java.util.ArrayList<Boolean> prev = new java.util.ArrayList<>(snapshotOnlyNodes.size());
    for (var n : snapshotOnlyNodes) { prev.add(n.isVisible()); n.setVisible(true); }

    try {
        if (image == null || (int) image.getWidth() != (int) w || (int) image.getHeight() != (int) h) {
            image = worldToView.snapshot(params, null);
        } else {
            worldToView.snapshot(params, image);
        }
        setImage(image);
    } finally {
        // restore original visibilities
        for (int i = 0; i < snapshotOnlyNodes.size(); i++) {
            snapshotOnlyNodes.get(i).setVisible(prev.get(i));
        }
    }
}

    /**
     * Aim a CameraViewLite at a target from an eye position.
     * JavaFX camera looks along -Z by default; we convert to yaw(Y) + pitch(X) with world-space translate on the rig.
     */
public static void poseLookAt(CameraViewLite view,
                               double eyeX, double eyeY, double eyeZ,
                               double targetX, double targetY, double targetZ,
                               double vfovDeg) {
    double dx = targetX - eyeX;
    double dy = targetY - eyeY;      // NOTE: JavaFX Y+ is down
    double dz = targetZ - eyeZ;

    double yawDeg   = Math.toDegrees(Math.atan2(dx, -dz));   // around +Y
    double horiz    = Math.hypot(dx, dz);
    double pitchDeg = -Math.toDegrees(Math.atan2(dy, horiz)); // negate for Y-down

    view.getRy().setAngle(yawDeg);
    view.getRx().setAngle(pitchDeg);
    view.getRz().setAngle(0);

    view.getT().setX(eyeX);
    view.getT().setY(eyeY);
    view.getT().setZ(eyeZ);

    view.getCamera().setTranslateX(0);
    view.getCamera().setTranslateY(0);
    view.getCamera().setTranslateZ(0);
    view.getCamera().setFieldOfView(vfovDeg);
}

    // --- Expose the rig & camera (to set fixed poses) ---
    public PerspectiveCamera getCamera() { return camera; }
    public Translate getT() { return t; }
    public Rotate getRx() { return rx; }
    public Rotate getRy() { return ry; }
    public Rotate getRz() { return rz; }
}
