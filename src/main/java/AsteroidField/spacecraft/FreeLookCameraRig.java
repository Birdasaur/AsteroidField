package AsteroidField.spacecraft;

import javafx.animation.AnimationTimer;
import javafx.scene.Camera;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.transform.Rotate;
import javafx.scene.Group;

import java.util.HashSet;
import java.util.Set;

/**
 * Simple FPS free-look rig:
 *   yawGroup (Rotate Y) -> pitchGroup (Rotate X) -> camera
 * Mouse: hold RIGHT-BUTTON to look around (or press 'M' to toggle capture)
 * Keys:  WASD (strafe/forward), R/F (up/down), Shift (sprint)
 */
public class FreeLookCameraRig {

    // Rig graph
    private final Group yawGroup = new Group();
    private final Group pitchGroup = new Group();
    private Camera camera;

    // State
    private final Set<KeyCode> pressed = new HashSet<>();
    private boolean mouseCaptured = false;
    private double yawDeg = 0.0;      // around +Y
    private double pitchDeg = 0.0;    // around +X (clamped)

    // Tunables
    private double mouseSensitivity = 0.2;  // degrees per px
    private double pitchMin = -89.0, pitchMax = 89.0;
    private double moveSpeed = 600.0;       // units/sec
    private double sprintMul = 3.0;

    // Timer
    private AnimationTimer loop;

    public FreeLookCameraRig(Camera cam) {
        attachCamera(cam);
    }

    /** Build the rig graph: yaw -> pitch -> camera. */
    private void attachCamera(Camera cam) {
        this.camera = cam;
        pitchGroup.getChildren().setAll(cam);
        yawGroup.getChildren().setAll(pitchGroup);

        // origin pose
        yawGroup.setTranslateX(0);
        yawGroup.setTranslateY(0);
        yawGroup.setTranslateZ(0);
        setYawPitch(0, 0);
    }

    /** The root node to add to your 3D world instead of adding the camera directly. */
    public Group getRoot() { return yawGroup; }

    /** Place rig in world. */
    public void setTranslate(double x, double y, double z) {
        yawGroup.setTranslateX(x);
        yawGroup.setTranslateY(y);
        yawGroup.setTranslateZ(z);
    }

    /** Set yaw/pitch in degrees. */
    public void setYawPitch(double yaw, double pitch) {
        this.yawDeg = yaw;
        this.pitchDeg = clamp(pitch, pitchMin, pitchMax);
        yawGroup.setRotationAxis(Rotate.Y_AXIS);
        yawGroup.setRotate(yawDeg);
        pitchGroup.setRotationAxis(Rotate.X_AXIS);
        pitchGroup.setRotate(pitchDeg);
    }

    /** Wire input + start loop. Pass either a Scene or a SubScene for events. */
    public void start(Node eventTarget) {
        // Key input
        eventTarget.setOnKeyPressed(e -> pressed.add(e.getCode()));
        eventTarget.setOnKeyReleased(e -> pressed.remove(e.getCode()));

        // Mouse capture: hold RMB or toggle with 'M'
        eventTarget.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() == MouseButton.SECONDARY) capture(eventTarget, true);
        });
        eventTarget.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            if (e.getButton() == MouseButton.SECONDARY) capture(eventTarget, false);
        });

        // Toggle with M
        eventTarget.setOnKeyTyped(e -> {
            if ("m".equalsIgnoreCase(e.getCharacter())) {
                capture(eventTarget, !mouseCaptured);
            }
        });

        // Mouse look (delta-based)
        eventTarget.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!mouseCaptured) return;
            double dx = e.getSceneX() - lastX;
            double dy = e.getSceneY() - lastY;
            lastX = e.getSceneX();
            lastY = e.getSceneY();
            yawDeg   -= dx * mouseSensitivity;
            pitchDeg -= dy * mouseSensitivity;
            pitchDeg = clamp(pitchDeg, pitchMin, pitchMax);
            applyRotations();
        });

        // Track last mouse position when moving
        eventTarget.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            lastX = e.getSceneX();
            lastY = e.getSceneY();
        });

        // Main loop for movement
        loop = new AnimationTimer() {
            long last = 0;
            @Override public void handle(long now) {
                if (last == 0) { last = now; return; }
                double dt = (now - last) / 1_000_000_000.0;
                last = now;

                double speed = moveSpeed * (pressed.contains(KeyCode.SHIFT) ? sprintMul : 1.0);

                // Key axes
                double f = 0, r = 0, u = 0;
                if (pressed.contains(KeyCode.W)) f += 1;
                if (pressed.contains(KeyCode.S)) f -= 1;
                if (pressed.contains(KeyCode.D)) r += 1;
                if (pressed.contains(KeyCode.A)) r -= 1;
                if (pressed.contains(KeyCode.R)) u += 1;
                if (pressed.contains(KeyCode.F)) u -= 1;
                if (f == 0 && r == 0 && u == 0) return;

                // Move in rig space (respect yaw/pitch)
                // Forward vector from yaw/pitch:
                double cy = Math.cos(Math.toRadians(yawDeg));
                double sy = Math.sin(Math.toRadians(yawDeg));
                double cx = Math.cos(Math.toRadians(pitchDeg));
                double sx = Math.sin(Math.toRadians(pitchDeg));

                // Right = (cosYaw, 0, -sinYaw)
                double rx =  cy;
                double rz = -sy;

                // Forward = (sinYaw*cosPitch, -sinPitch, cosYaw*cosPitch)
                double fx =  sy * cx;
                double fy = -sx;
                double fz =  cy * cx;

                // Up (world-up bias): use (0,1,0) for R/F keys, not strictly camera-up
                double ux = 0, uy = 1, uz = 0;

                double dx = (fx * f + rx * r + ux * u) * speed * dt;
                double dy = (fy * f + uy * u) * speed * dt;
                double dz = (fz * f + rz * r + uz * u) * speed * dt;

                yawGroup.setTranslateX(yawGroup.getTranslateX() + dx);
                yawGroup.setTranslateY(yawGroup.getTranslateY() + dy);
                yawGroup.setTranslateZ(yawGroup.getTranslateZ() + dz);
            }
        };
        loop.start();

        // ensure we can get key events
        eventTarget.requestFocus();
    }

    public void stop() {
        if (loop != null) loop.stop();
    }

    // --- helpers ---
    private double lastX, lastY;

    private void capture(Node target, boolean on) {
        mouseCaptured = on;
        Scene s = target.getScene();
        if (s != null) s.setCursor(on ? Cursor.CROSSHAIR : Cursor.DEFAULT);
        // sync last mouse so the first drag delta isn't huge
        target.setOnMouseMoved(e -> { lastX = e.getSceneX(); lastY = e.getSceneY(); });
    }

    private void applyRotations() {
        yawGroup.setRotationAxis(Rotate.Y_AXIS);
        yawGroup.setRotate(yawDeg);
        pitchGroup.setRotationAxis(Rotate.X_AXIS);
        pitchGroup.setRotate(pitchDeg);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // Optional tuning:
    public void setMouseSensitivity(double degPerPx) { this.mouseSensitivity = degPerPx; }
    public void setMoveSpeed(double unitsPerSec)     { this.moveSpeed = unitsPerSec; }
    public void setSprintMultiplier(double m)        { this.sprintMul = m; }
    public void setPitchLimits(double minDeg, double maxDeg) { pitchMin = minDeg; pitchMax = maxDeg; }
}
