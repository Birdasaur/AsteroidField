package AsteroidField.util;

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
 * - Uses Robot to re-center the pointer ("infinite" mouselook) with a dead zone to minimize warps.
 *
 * Two optimizations:
 *  1) Dead zone: only recenter when |dx| or |dy| exceeds a small radius (adaptive by default).
 *  2) Edge safety: force recenter if the cursor is within a border margin of the SubScene edges.
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

    // Dead-zone & edge-safety
    private boolean useAdaptiveDeadZone = true;
    private int deadZonePixels = 8;              // used if adaptive is disabled
    private int edgeBorderPixels = 32;           // force recenter if cursor within this of any edge

    // Smoothed deltas
    private double smDX = 0;
    private double smDY = 0;

    // Current yaw/pitch (degrees)
    private double yawDeg = 0;
    private double pitchDeg = 0;

    // Center in screen coords (where we warp mouse back to)
    private double centerScreenX = 0;
    private double centerScreenY = 0;

    // Cached subscene screen-rect for quick edge checks
    private double ssMinX = 0, ssMinY = 0, ssMaxX = 0, ssMaxY = 0;

    public FpsLookController(SubScene subScene, PerspectiveCamera camera) {
        this.subScene = subScene;
        this.camera = camera;
        this.robot = new Robot();

        // Attach our rotation transforms to the camera (yaw then pitch)
        camera.getTransforms().addAll(yawRotate, pitchRotate);

        // Capture on press when enabled (do not auto-capture on startup)
        subScene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (!enabled) return;
            if (!pointerLocked) capturePointer();
        });

        // Per-event mouselook (screen-space)
        subScene.addEventFilter(MouseEvent.MOUSE_MOVED, this::onMouseMoved);
        subScene.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::onMouseMoved);

        // Release on ESC
        subScene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) releasePointer();
        });

        // Track geometry changes to keep center/bounds updated
        subScene.widthProperty().addListener((o, ov, nv) -> updateCenterAndBounds());
        subScene.heightProperty().addListener((o, ov, nv) -> updateCenterAndBounds());
        subScene.sceneProperty().addListener((o, ov, nv) -> updateCenterAndBounds());
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
        updateCenterAndBounds();
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

    // --- Dead-zone/edge-safety tuning ---

    /** Use an adaptive dead-zone based on sensitivity (default: true). */
    public void setAdaptiveDeadZone(boolean on) { this.useAdaptiveDeadZone = on; }

    /** Fixed dead-zone radius in pixels (used when adaptive is OFF). */
    public void setDeadZonePixels(int px) { this.deadZonePixels = Math.max(1, px); }

    /** Edge-safety border in pixels; force recenter when within this of any edge. */
    public void setEdgeBorderPixels(int px) { this.edgeBorderPixels = Math.max(0, px); }

    // --- Internals ---

    private void onMouseMoved(MouseEvent e) {
        if (!enabled || !pointerLocked) return;

        if (suppressNextEvent) {
            suppressNextEvent = false; // ignore synthetic event from Robot.mouseMove
            e.consume();
            return;
        }

        // Current event position (screen coords)
        final double mx = e.getScreenX();
        final double my = e.getScreenY();

        // Compute delta from current pointer to our center (screen coords)
        final double dx = mx - centerScreenX;
        final double dy = my - centerScreenY;

        // Low-pass on deltas
        final double alpha = smoothing <= 0 ? 1.0 : (1.0 - Math.exp(-smoothing));
        smDX += (dx - smDX) * alpha;
        smDY += (dy - smDY) * alpha;

        // Apply sensitivity; invertY if requested
        final double signY = invertY ? +1.0 : -1.0;
        yawDeg   += smDX * sensitivityDegPerPixel;
        pitchDeg += smDY * sensitivityDegPerPixel * signY;

        // Clamp pitch, apply rotates
        pitchDeg = clamp(pitchDeg, minPitchDeg, maxPitchDeg);
        yawRotate.setAngle(yawDeg);
        pitchRotate.setAngle(pitchDeg);

        // Decide whether to recenter (dead-zone and edge-safety)
        final int dz = effectiveDeadZonePixels();
        final boolean outsideDeadZone = (Math.abs(dx) > dz) || (Math.abs(dy) > dz);
        final boolean nearEdge = isNearEdge(mx, my, edgeBorderPixels);

        if (outsideDeadZone || nearEdge) {
            warpToCenter();
        }

        e.consume();
    }

    private int effectiveDeadZonePixels() {
        if (!useAdaptiveDeadZone) return deadZonePixels;
        // Adaptive: r_px ≈ 0.25 / sensitivity (deg/px), clamped to [6, 18]
        double r = 0.25 / sensitivityDegPerPixel;
        int rPx = (int) Math.round(r);
        if (rPx < 6) rPx = 6;
        if (rPx > 18) rPx = 18;
        return rPx;
    }

    private boolean isNearEdge(double mx, double my, int borderPx) {
        if (borderPx <= 0) return false;
        // If we haven't got bounds yet, be conservative and say not near edge.
        if (ssMaxX <= ssMinX || ssMaxY <= ssMinY) return false;
        return (mx - ssMinX) < borderPx
            || (ssMaxX - mx) < borderPx
            || (my - ssMinY) < borderPx
            || (ssMaxY - my) < borderPx;
    }

    private void warpToCenter() {
        updateCenterAndBounds(); // ensure center/bounds are current
        suppressNextEvent = true; // next move event is synthetic
        robot.mouseMove(centerScreenX, centerScreenY);
    }

    private void updateCenterAndBounds() {
        if (subScene.getScene() == null) return;
        Bounds b = subScene.localToScreen(subScene.getLayoutBounds());
        if (b != null) {
            ssMinX = b.getMinX();
            ssMinY = b.getMinY();
            ssMaxX = b.getMaxX();
            ssMaxY = b.getMaxY();
            centerScreenX = ssMinX + b.getWidth()  * 0.5;
            centerScreenY = ssMinY + b.getHeight() * 0.5;
        } else {
            // Fallback to window center if not yet laid out
            Point2D p = subScene.localToScreen(0, 0);
            if (p != null) {
                ssMinX = p.getX();
                ssMinY = p.getY();
                ssMaxX = ssMinX + subScene.getWidth();
                ssMaxY = ssMinY + subScene.getHeight();
                centerScreenX = ssMinX + subScene.getWidth()  * 0.5;
                centerScreenY = ssMinY + subScene.getHeight() * 0.5;
            }
        }
    }

    private static double clamp(double v, double lo, double hi) {
        return (v < lo) ? lo : (v > hi) ? hi : v;
    }
}
