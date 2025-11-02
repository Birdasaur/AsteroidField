package AsteroidField.spacecraft.control;

import AsteroidField.physics.PhysicsContributor;
import AsteroidField.tether.Tether;
import javafx.geometry.Point3D;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;

/** Featherable micro-thrusters + free-look camera, integrated with fixed-step physics. */
public class ThrusterController implements PhysicsContributor {

    private final SubScene subScene;
    private final PerspectiveCamera camera;
    private final Tether.SpacecraftAdapter craft;

    // Enable/disable
    private boolean enabled = true;

    // Inputs
    private boolean fwd, back, left, right, up, down;
    private boolean braking = false;
    private boolean dampeners = false;
    private boolean freeLook = false;

    // Look state
    private double yawDeg = 0.0;   // around +Y
    private double pitchDeg = 0.0; // around +X
    private double mousePrevX, mousePrevY;
    private final Rotate yaw = new Rotate(0, Rotate.Y_AXIS);
    private final Rotate pitch = new Rotate(0, Rotate.X_AXIS);

    // Tuning
    private double thrustPower     = 450.0; // N
    private double verticalPower   = 350.0; // N
    private double brakePower      = 1200.0;// N (hold X)
    private double dampenerPower   = 200.0; // N (toggle Z)
    private double lookSensitivity = 0.15;  // deg/pixel
    private double maxPitch        = 85.0;  // deg

    // How "aligned" forward must be with velocity to consider it "looking forward"
    private double quickLookAlignThreshold = 0.35; // cos(angle). ~0.35 ≈ 69°

    public ThrusterController(SubScene subScene, PerspectiveCamera camera, Tether.SpacecraftAdapter craft) {
        this.subScene = subScene;
        this.camera = camera;
        this.craft = craft;

        // Camera free-look transforms (rotate camera only; physics rig remains external)
        if (!camera.getTransforms().contains(yaw))   camera.getTransforms().add(yaw);
        if (!camera.getTransforms().contains(pitch)) camera.getTransforms().add(pitch);

        installInputHandlers();
        subScene.setFocusTraversable(true);
    }

    // -------- Enable / Disable --------

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            resetInputState();
        }
    }
    public boolean isEnabled() { return enabled; }

    private void resetInputState() {
        fwd = back = left = right = up = down = false;
        braking = false;
        freeLook = false;
        // leave dampeners as-is (acts like an assist toggle)
    }

    // -------- configuration ----------
    public void setThrustPower(double n)    { thrustPower = Math.max(0, n); }
    public void setVerticalPower(double n)  { verticalPower = Math.max(0, n); }
    public void setBrakePower(double n)     { brakePower = Math.max(0, n); }
    public void setDampenerPower(double n)  { dampenerPower = Math.max(0, n); }
    public void setLookSensitivity(double d){ lookSensitivity = Math.max(0.01, d); }
    public void setMaxPitch(double deg)     { maxPitch = Math.max(0, deg); }
    public void setQuickLookAlignThreshold(double cosAngle) { quickLookAlignThreshold = Math.max(-1, Math.min(1, cosAngle)); }

    // -------- PhysicsContributor --------
    @Override public void step(double dt) {
        if (!enabled) return;

        Basis basis = basisFromCamera();

        // Movement input (camera frame)
        double ix = (right ? 1 : 0) + (left ? -1 : 0);
        double iy = (up ? 1 : 0) + (down ? -1 : 0);
        double iz = (fwd ? 1 : 0) + (back ? -1 : 0);

        // Normalize so diagonals aren't stronger
        double len = Math.sqrt(ix*ix + iy*iy + iz*iz);
        if (len > 1e-6) { ix/=len; iy/=len; iz/=len; }

        // Apply thrust forces
        if (len > 0) {
            Point3D force =
                    basis.right.multiply(ix * thrustPower)
                 .add(basis.up.multiply(iy * verticalPower))
                 .add(basis.fwd.multiply(iz * thrustPower));
            craft.applyForce(force);
        }

        // Braking (strong, hold X): oppose current velocity
        if (braking) {
            Point3D v = craft.getVelocity();
            double speed = v.magnitude();
            if (speed > 1e-3) {
                craft.applyForce(v.normalize().multiply(-brakePower));
            }
        }

        // Dampeners (gentle, toggle Z): low continuous brake
        if (dampeners) {
            Point3D v = craft.getVelocity();
            double speed = v.magnitude();
            if (speed > 1e-3) {
                craft.applyForce(v.normalize().multiply(-dampenerPower));
            }
        }

        // Apply camera rotations from yaw/pitch each step
        yaw.setAngle(yawDeg);
        pitch.setAngle(pitchDeg);
    }

    // -------- input wiring --------
    private void installInputHandlers() {
        subScene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (!enabled) return;
            KeyCode c = e.getCode();
            switch (c) {
                case W -> fwd = true;
                case S -> back = true;
                case A -> left = true;
                case D -> right = true;
                case R -> up = true;
                case F -> down = true;
                case X -> braking = true;
                case Z -> { if (!e.isShortcutDown()) dampeners = !dampeners; } // toggle assist
                case V -> quickLookToggle(); // <— toggle between forward/backward along velocity
                default -> {}
            }
        });
        subScene.addEventFilter(KeyEvent.KEY_RELEASED, e -> {
            if (!enabled) return;
            KeyCode c = e.getCode();
            switch (c) {
                case W -> fwd = false;
                case S -> back = false;
                case A -> left = false;
                case D -> right = false;
                case R -> up = false;
                case F -> down = false;
                case X -> braking = false;
                default -> {}
            }
        });

        subScene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (!enabled) return;
            if (e.getButton() == MouseButton.MIDDLE) {
                freeLook = true;
                mousePrevX = e.getSceneX();
                mousePrevY = e.getSceneY();
            }
        });
        subScene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (!enabled) return;
            if (e.getButton() == MouseButton.MIDDLE) {
                freeLook = false;
            }
        });
        subScene.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!enabled || !freeLook) return;
            double dx = e.getSceneX() - mousePrevX;
            double dy = e.getSceneY() - mousePrevY;
            mousePrevX = e.getSceneX();
            mousePrevY = e.getSceneY();

            yawDeg   += dx * lookSensitivity;
            pitchDeg -= dy * lookSensitivity;
            if (pitchDeg >  maxPitch) pitchDeg =  maxPitch;
            if (pitchDeg < -maxPitch) pitchDeg = -maxPitch;
        });
    }

    /** Toggle between looking forward (along velocity) and backward (against it). */
    private void quickLookToggle() {
        Point3D v = craft.getVelocity();
        double speed = v.magnitude();
        if (speed < 1e-3) return; // nothing to align to

        Point3D vDir = v.normalize();
        Basis b = basisFromCamera();
        double dot = b.fwd.dotProduct(vDir); // +1 aligned with velocity, -1 opposite

        if (dot >= quickLookAlignThreshold) {
            // Currently looking forward along velocity → flip to look backward
            alignForwardToDirection(vDir.multiply(-1));
        } else if (dot <= -quickLookAlignThreshold) {
            // Currently looking backward → flip to look forward
            alignForwardToDirection(vDir);
        } else {
            // Ambiguous angle → choose "look back" first
            alignForwardToDirection(vDir.multiply(-1));
        }
    }

    private void alignForwardToDirection(Point3D dir) {
        if (dir.magnitude() < 1e-6) return;
        dir = dir.normalize();
        // Derive yaw/pitch from direction in parent coordinates
        double yaw = Math.toDegrees(Math.atan2(dir.getX(), dir.getZ())); // Yaw: +X right, +Z fwd
        double pitch = Math.toDegrees(Math.asin(dir.getY()));            // Pitch: up/down
        this.yawDeg = yaw;
        this.pitchDeg = Math.max(-maxPitch, Math.min(maxPitch, pitch));
    }

    // Basis vectors from camera orientation in parent/world coordinates
    private Basis basisFromCamera() {
        Affine camToScene = new Affine(camera.getLocalToSceneTransform());
        Point3D originScene = camToScene.transform(Point3D.ZERO);
        Point3D rightScene  = camToScene.transform(new Point3D(1, 0, 0));
        Point3D upScene     = camToScene.transform(new Point3D(0, 1, 0));
        Point3D fwdScene    = camToScene.transform(new Point3D(0, 0, 1));

        Point3D oP = originScene;
        Point3D rP = rightScene.subtract(oP).normalize();
        Point3D uP = upScene.subtract(oP).normalize();
        Point3D fP = fwdScene.subtract(oP).normalize();
        return new Basis(rP, uP, fP);
    }
    @Override
    public AsteroidField.physics.PhysicsPhase getPhase() {
        return AsteroidField.physics.PhysicsPhase.FORCE;
    }

    @Override
    public int getPriority() { return 0; } // gravity could be -10 if you add it later

    private record Basis(Point3D right, Point3D up, Point3D fwd) {}
}
