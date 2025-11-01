package AsteroidField.tether;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;

/**
 * Kinematic point-mass adapter around the camera.
 * Accumulates forces and integrates velocity/position each fixed step.
 */
public class CameraKinematicAdapter implements KinematicCraft, Tether.SpacecraftAdapter {

    private final PerspectiveCamera camera;
    private final Group rig;        // moved by physics
    private final Group worldRoot;  // parent/world space

    // Physics state
    private double mass = 1.0;
    private Point3D velocity = Point3D.ZERO;   // parent/world space
    private Point3D forceAccum = Point3D.ZERO;

    // Tuning
    private double linearDampingPerSecond = 0.15; // fraction/sec
    private double maxSpeed = 550;

    private CameraKinematicAdapter(PerspectiveCamera camera, Group rig, Group worldRoot) {
        this.camera = camera;
        this.rig = rig;
        this.worldRoot = worldRoot;
    }
    public Node getRigNode() { return rig; } 

    /** Wrap camera in a rig under worldRoot and return the adapter. */
    public static CameraKinematicAdapter attach(SubScene subScene, Group worldRoot) {
        PerspectiveCamera cam = (PerspectiveCamera) subScene.getCamera();
        if (cam == null) {
            cam = new PerspectiveCamera(true);
            subScene.setCamera(cam);
        }
        Parent prev = cam.getParent();
        if (prev instanceof Group g) g.getChildren().remove(cam);

        Group rig = new Group();
        // absorb camera's current transforms onto the rig
        rig.setTranslateX(cam.getTranslateX());
        rig.setTranslateY(cam.getTranslateY());
        rig.setTranslateZ(cam.getTranslateZ());
        rig.setScaleX(cam.getScaleX());
        rig.setScaleY(cam.getScaleY());
        rig.setScaleZ(cam.getScaleZ());
        rig.setRotationAxis(cam.getRotationAxis());
        rig.setRotate(cam.getRotate());
        rig.getTransforms().setAll(cam.getTransforms());

        // reset camera locals
        cam.getTransforms().clear();
        cam.setTranslateX(0); cam.setTranslateY(0); cam.setTranslateZ(0);
        cam.setScaleX(1); cam.setScaleY(1); cam.setScaleZ(1);
        cam.setRotationAxis(javafx.geometry.Point3D.ZERO.add(0,1,0));
        cam.setRotate(0);

        rig.getChildren().add(cam);
        if (!worldRoot.getChildren().contains(rig)) worldRoot.getChildren().add(rig);
        return new CameraKinematicAdapter(cam, rig, worldRoot);
    }

    // --- Controls / tuning ---
    public void setMass(double m) { this.mass = Math.max(0.001, m); }
    public double getMass() { return mass; }
    public void setMaxSpeed(double v) { this.maxSpeed = Math.max(1, v); }
    public void setLinearDampingPerSecond(double v) { this.linearDampingPerSecond = Math.max(0, Math.min(0.99, v)); }

    public void setPosition(double x, double y, double z) {
        rig.setTranslateX(x); rig.setTranslateY(y); rig.setTranslateZ(z);
    }

    public void resetPosition(double x, double y, double z) {
        setPosition(x, y, z);
        velocity = Point3D.ZERO;
        forceAccum = Point3D.ZERO;
    }

    // --- SpacecraftAdapter ---
    @Override public Point3D getWorldPosition() {
        // Convert camera local origin to worldRoot parent space
        Point3D camScene = camera.localToScene(Point3D.ZERO);
        return worldRoot.sceneToLocal(camScene);
    }

    @Override public Point3D getVelocity() { return velocity; }

    @Override public void applyForce(Point3D force) {
        if (force != null) forceAccum = forceAccum.add(force);
    }

    // --- Physics integration (fixed dt) ---
    @Override public void tick(double dt) {
        Point3D accel = (mass > 0) ? forceAccum.multiply(1.0 / mass) : Point3D.ZERO;
        velocity = velocity.add(accel.multiply(dt));

        // global damping
        double keep = Math.pow(1.0 - linearDampingPerSecond, dt);
        velocity = new Point3D(velocity.getX() * keep, velocity.getY() * keep, velocity.getZ() * keep);

        // speed clamp
        double speed = velocity.magnitude();
        if (speed > maxSpeed) velocity = velocity.normalize().multiply(maxSpeed);

        // integrate position
        rig.setTranslateX(rig.getTranslateX() + velocity.getX() * dt);
        rig.setTranslateY(rig.getTranslateY() + velocity.getY() * dt);
        rig.setTranslateZ(rig.getTranslateZ() + velocity.getZ() * dt);

        // clear forces
        forceAccum = Point3D.ZERO;
    }
    /** Set craft world position in the same (parent/world) space the rig uses. */
    public void setWorldPosition(Point3D p) {
        if (p == null) return;
        rig.setTranslateX(p.getX());
        rig.setTranslateY(p.getY());
        rig.setTranslateZ(p.getZ());
    }

    /** Convenience overload. */
    public void setWorldPosition(double x, double y, double z) {
        rig.setTranslateX(x);
        rig.setTranslateY(y);
        rig.setTranslateZ(z);
    }

    /** Set craft linear velocity in world (parent) space. */
    public void setVelocity(Point3D v) {
        this.velocity = (v != null) ? v : Point3D.ZERO;
    }  
    public void setWorldPositionAndStop(Point3D p) {
        setWorldPosition(p);
        this.velocity = Point3D.ZERO;
        this.forceAccum = Point3D.ZERO;
    }    
}
