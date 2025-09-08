package AsteroidField;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Cursor;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.robot.Robot;
import javafx.scene.transform.Rotate;

/**
 * FPS-style mouse look controller for a JavaFX PerspectiveCamera.
 * - Click inside the SubScene to capture the pointer; ESC to release.
 * - While captured, mouse deltas rotate the camera (yaw then pitch).
 * - Uses Robot to re-center the pointer each event for "infinite" mouselook.
 *
 * This class attaches two Rotate transforms to the camera:
 *   yaw  = Rotate around +Y (left/right)
 *   pitch= Rotate around +X (up/down)
 */
public class FpsLookController {

    private final SubScene subScene;
    private final PerspectiveCamera camera;
    private final Robot robot;

    // Camera rotation nodes (order matters: yaw then pitch)
    private final Rotate yawRotate  = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate pitchRotate= new Rotate(0, Rotate.X_AXIS);

    // State
    private boolean enabled = true;        // master enable (default ON)
    private boolean pointerLocked = false; // captured
    private boolean suppressNextEvent = false; // ignore synthetic event after recenter

    // Tuning
    private double sensitivityDegPerPixel = 0.20; // effective degrees per pixel
    private double smoothing = 0.35;              // 0 = raw, 0.2..0.5 feels good
    private boolean invertY = false;
    private double minPitchDeg = -85;
    private double maxPitchDeg = +85;

    // Smoothed deltas
    private double smDX = 0;
    private double smDY = 0;

    // Current yaw/pitch (degrees)
    private double yawDeg = 0;
    private double pitchDeg = 0;

    // Center in screen coords (where we warp mouse back to)
    private double centerScreenX = 0;
    private double centerScreenY = 0;

    public FpsLookController(SubScene subScene, PerspectiveCamera camera) {
        this.subScene = subScene;
        this.camera = camera;
        this.robot = new Robot();

        // Attach our rotation transforms to the camera (yaw then pitch)
        // Keep existing transforms, just add ours up front to maintain predictable order.
        camera.getTransforms().addAll(yawRotate, pitchRotate);

        // Capture on press when enabled
        subScene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (!enabled) return;
            if (!pointerLocked) capturePointer();
        });

        // Per-event mouselook (screen-space)
        subScene.addEventFilter(MouseEvent.MOUSE_MOVED, this::onMouseMoved);
        subScene.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::onMouseMoved);

        // Release on ESC
        subScene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                releasePointer();
            }
        });

        // Track geometry changes to keep center updated
        subScene.widthProperty().addListener((o, ov, nv) -> updateCenterScreen());
        subScene.heightProperty().addListener((o, ov, nv) -> updateCenterScreen());
        subScene.sceneProperty().addListener((o, ov, nv) -> updateCenterScreen());
    }

    // --- Public API ---

    public void setEnabled(boolean v) {
        enabled = v;
        if (!enabled) releasePointer();
    }
    public boolean isEnabled() { return enabled; }

    public void setSensitivity(double degreesPerPixel) {
        sensitivityDegPerPixel = Math.max(0.01, degreesPerPixel);
    }
    public void setSmoothing(double s01) {
        smoothing = Math.max(0.0, Math.min(0.95, s01));
    }
    public void setInvertY(boolean invert) { invertY = invert; }

    public void setPitchLimits(double minDeg, double maxDeg) {
        minPitchDeg = Math.min(minDeg, maxDeg);
        maxPitchDeg = Math.max(minDeg, maxDeg);
        // clamp current pitch if needed
        setYawPitch(yawDeg, pitchDeg);
    }

    /** Programmatically set yaw/pitch (e.g., Reset Camera). */
    public void setYawPitch(double yawDeg, double pitchDeg) {
        this.yawDeg = yawDeg;
        this.pitchDeg = clamp(pitchDeg, minPitchDeg, maxPitchDeg);
        yawRotate.setAngle(this.yawDeg);
        pitchRotate.setAngle(this.pitchDeg);
    }

    /** Capture the pointer (hide cursor + warp to center). */
    public void capturePointer() {
        if (!enabled || pointerLocked) return;
        pointerLocked = true;
        subScene.setCursor(Cursor.NONE);
        updateCenterScreen();
        warpToCenter(); // initial recenter
    }

    /** Release the pointer (show cursor). */
    public void releasePointer() {
        if (!pointerLocked) return;
        pointerLocked = false;
        subScene.setCursor(Cursor.DEFAULT);
        smDX = smDY = 0;
    }

    /** Camera basis in world space (useful for thrusters/aim). */
    public Point3D getCameraForward() {
        var t = camera.getLocalToSceneTransform();
        return t.deltaTransform(0, 0, +1).normalize();
    }
    public Point3D getCameraRight() {
        var t = camera.getLocalToSceneTransform();
        return t.deltaTransform(+1, 0, 0).normalize();
    }
    public Point3D getCameraUp() {
        var t = camera.getLocalToSceneTransform();
        return t.deltaTransform(0, +1, 0).normalize();
    }

    // --- Internals ---

    private void onMouseMoved(MouseEvent e) {
        if (!enabled || !pointerLocked) return;

        if (suppressNextEvent) {
            // Ignore the synthetic event generated by our Robot.mouseMove(...)
            suppressNextEvent = false;
            e.consume();
            return;
        }

        // Compute delta from current pointer to our center (screen coords)
        double dx = e.getScreenX() - centerScreenX;
        double dy = e.getScreenY() - centerScreenY;

        // Low-pass on deltas: smoothed += (raw - smoothed) * alpha
        double alpha = 1.0 - Math.exp(-smoothing); // smooth factor from 0..~0.63
        smDX += (dx - smDX) * alpha;
        smDY += (dy - smDY) * alpha;

        // Apply sensitivity; invertY if requested
        double signY = invertY ? +1.0 : -1.0;
        yawDeg   += smDX * sensitivityDegPerPixel;
        pitchDeg += smDY * sensitivityDegPerPixel * signY;

        // Clamp pitch, apply rotates
        pitchDeg = clamp(pitchDeg, minPitchDeg, maxPitchDeg);
        yawRotate.setAngle(yawDeg);
        pitchRotate.setAngle(pitchDeg);

        // Recenter pointer to avoid hitting screen edges
        warpToCenter();

        e.consume();
    }

    private void warpToCenter() {
        updateCenterScreen(); // ensure center is current
        suppressNextEvent = true; // next move event is synthetic
        robot.mouseMove(centerScreenX, centerScreenY);
    }

    private void updateCenterScreen() {
        if (subScene.getScene() == null) return;
        Bounds b = subScene.localToScreen(subScene.getLayoutBounds());
        if (b != null) {
            centerScreenX = b.getMinX() + b.getWidth()  * 0.5;
            centerScreenY = b.getMinY() + b.getHeight() * 0.5;
        } else {
            // Fallback to window center if not yet laid out
            Point2D p = subScene.localToScreen(0, 0);
            if (p != null) {
                centerScreenX = p.getX() + subScene.getWidth()  * 0.5;
                centerScreenY = p.getY() + subScene.getHeight() * 0.5;
            }
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return (v < lo) ? lo : (v > hi) ? hi : v;
    }
}
