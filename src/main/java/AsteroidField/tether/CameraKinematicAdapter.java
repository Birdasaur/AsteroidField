package AsteroidField.tether;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;

/**
 * Wraps the PerspectiveCamera in a "rig" Group that we move in world space.
 * Applies simple kinematic integration (velocity + damping) to enable
 * tether-driven motion without a full spacecraft controller.
 *
 * IMPORTANT: getWorldPosition() returns the camera's true world position converted
 * into the world-root (parent) space so the tether start matches what you see.
 */
public class CameraKinematicAdapter implements KinematicCraft {

    private final PerspectiveCamera camera;
    private final Group rig;       // parent of the camera; we move this via pulls
    private final Group worldRoot; // the 3D root (parent space the tether uses)
    private Point3D velocity = Point3D.ZERO;

    // Tuning knobs
    private double linearDampingPerSecond = 0.20;  // fraction of speed lost per second
    private double maxSpeed = 450;                 // units/s

    private CameraKinematicAdapter(PerspectiveCamera camera, Group rig, Group worldRoot) {
        this.camera = camera;
        this.rig = rig;
        this.worldRoot = worldRoot;
    }

    /**
     * Attach the subscene's camera into a rig that's parented by worldRoot.
     * Preserves the camera's current local transforms by moving them onto the rig,
     * then zeroes the camera's local transforms so the rig exclusively controls position.
     */
    public static CameraKinematicAdapter attach(SubScene subScene, Group worldRoot) {
        PerspectiveCamera cam = (PerspectiveCamera) subScene.getCamera();
        if (cam == null) {
            cam = new PerspectiveCamera(true);
            subScene.setCamera(cam);
        }

        // Remove from previous parent if present
        Parent prev = cam.getParent();
        if (prev instanceof Group g) g.getChildren().remove(cam);

        // Create rig and absorb camera's transforms
        Group rig = new Group();

        // Copy convenience transforms
        rig.setTranslateX(cam.getTranslateX());
        rig.setTranslateY(cam.getTranslateY());
        rig.setTranslateZ(cam.getTranslateZ());
        rig.setScaleX(cam.getScaleX());
        rig.setScaleY(cam.getScaleY());
        rig.setScaleZ(cam.getScaleZ());
        rig.setRotationAxis(cam.getRotationAxis());
        rig.setRotate(cam.getRotate());

        // Copy any custom transforms
        rig.getTransforms().setAll(cam.getTransforms());

        // Reset camera local transforms to identity
        cam.getTransforms().clear();
        cam.setTranslateX(0); cam.setTranslateY(0); cam.setTranslateZ(0);
        cam.setScaleX(1); cam.setScaleY(1); cam.setScaleZ(1);
        cam.setRotationAxis(javafx.geometry.Point3D.ZERO.add(0,1,0)); // default axis
        cam.setRotate(0);

        // Parent camera under rig; rig under world
        rig.getChildren().add(cam);
        if (!worldRoot.getChildren().contains(rig)) {
            worldRoot.getChildren().add(rig);
        }
        return new CameraKinematicAdapter(cam, rig, worldRoot);
    }

    /** Set initial world position (and leave velocity as-is). */
    public void setPosition(double x, double y, double z) {
        rig.setTranslateX(x);
        rig.setTranslateY(y);
        rig.setTranslateZ(z);
    }

    /** Reset position and zero velocity. */
    public void resetPosition(double x, double y, double z) {
        setPosition(x, y, z);
        velocity = Point3D.ZERO;
    }

    public void setMaxSpeed(double v) { this.maxSpeed = Math.max(1, v); }
    public void setLinearDampingPerSecond(double v) { this.linearDampingPerSecond = Math.max(0, Math.min(0.99, v)); }

    // ---- Tether.SpacecraftAdapter ----
    @Override
    public Point3D getWorldPosition() {
        // Return camera position in *worldRoot (parent)* coordinates.
        // This accounts for camera-local transforms (from your controllers) + rig transforms.
        Point3D cameraInScene = camera.localToScene(Point3D.ZERO);
        return worldRoot.sceneToLocal(cameraInScene);
    }

    @Override
    public void applyPull(Point3D acceleration, double dt) {
        // v = v + a*dt
        velocity = velocity.add(acceleration.multiply(dt));
        // Clamp speed
        double speed = velocity.magnitude();
        if (speed > maxSpeed) {
            velocity = velocity.normalize().multiply(maxSpeed);
        }
    }

    // ---- KinematicCraft ----
    @Override
    public void tick(double dt) {
        // Integrate position: p = p + v*dt
        rig.setTranslateX(rig.getTranslateX() + velocity.getX() * dt);
        rig.setTranslateY(rig.getTranslateY() + velocity.getY() * dt);
        rig.setTranslateZ(rig.getTranslateZ() + velocity.getZ() * dt);

        // Exponential damping
        double keep = Math.pow(1.0 - linearDampingPerSecond, dt);
        velocity = new Point3D(velocity.getX() * keep, velocity.getY() * keep, velocity.getZ() * keep);
    }
}
