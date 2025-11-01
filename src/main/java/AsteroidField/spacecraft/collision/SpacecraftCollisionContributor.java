package AsteroidField.spacecraft.collision;

import AsteroidField.events.CollisionEvent;
import AsteroidField.events.GameEventBus;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import AsteroidField.tether.PhysicsContributor;
import AsteroidField.tether.CameraKinematicAdapter;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.shape.MeshView;

/**
 * Runs a continuous swept-sphere collision solve for the spacecraft each fixed physics step.
 * Implements your PhysicsContributor interface so it plugs into TetherSystem like ThrusterController.
 */
public final class SpacecraftCollisionContributor implements PhysicsContributor {

    private final Node worldRoot;
    private final CameraKinematicAdapter craft;
    private final java.util.function.Supplier<java.util.List<javafx.scene.Node>> collidables;
    private final double radius;

    // tuning
    private double restitution = 0.05;
    private double friction = 0.15;
    private int maxIterations = 2;
    private boolean frontFaceOnly = false; // start permissive; set true once outer-winding is verified

    private boolean enabled = true;

    public SpacecraftCollisionContributor(Node worldRoot,
                                          CameraKinematicAdapter craft,
                                          java.util.function.Supplier<java.util.List<javafx.scene.Node>> collidables,
                                          double craftRadius) {
        this.worldRoot = worldRoot;
        this.craft = craft;
        this.collidables = collidables;
        this.radius = craftRadius;
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isEnabled() { return enabled; }

    public void setRestitution(double r) { this.restitution = Math.max(0.0, r); }
    public void setFriction(double f) { this.friction = Math.max(0.0, f); }
    public void setMaxIterations(int n) { this.maxIterations = Math.max(1, n); }
    public void setFrontFaceOnly(boolean v) { this.frontFaceOnly = v; }

    @Override
    public void step(double dt) {
        if (!enabled) return;

        Point3D p0 = craft.getWorldPosition(); // WORLD-ROOT (your 'world' Group) space
        Point3D v  = craft.getVelocity();

        double remaining = dt;
        boolean any = false;

        // Flatten to MeshViews once per step
        List<MeshView> meshes = new ArrayList<>();
        for (Node n : collidables.get()) collectMeshViews(n, meshes);

        for (int iter = 0; iter < maxIterations && remaining > 1e-6; iter++) {
            Point3D p1 = p0.add(v.multiply(remaining));

            // WORLD-ROOT → SCENE for the sweep
            Point3D p0Scene = worldRoot.localToScene(p0);
            Point3D p1Scene = worldRoot.localToScene(p1);

            double bestT = Double.POSITIVE_INFINITY;
            SweepHit best = null;

            Node colliderNode = null;
            for (MeshView mv : meshes) {
                Optional<SweepSphereMesh.Hit> h = SweepSphereMesh.firstHit(
                        mv, p0Scene, p1Scene, radius, frontFaceOnly);
                if (h.isPresent() && h.get().t < bestT) {
                    bestT = h.get().t;

                    // Hit point SCENE → WORLD-ROOT
                    Point3D hitWorld = worldRoot.sceneToLocal(h.get().pointScene);

                    // Normal: SCENE vector → WORLD-ROOT via two-point delta at the hit
                    Point3D nWorld = sceneVectorToWorldAt(h.get().normalScene, h.get().pointScene);

                    best = new SweepHit(hitWorld, normalize(nWorld));
                    
                    colliderNode = mv;
                }
            }

            if (best == null) {
                // No collision this slice: accept full step
                p0 = p1;
                break;
            }

            any = true;

            // Move to contact
            double tStep = clamp01(bestT);
            Point3D contactPos = p0.add(v.multiply(remaining * tStep));

            // Separate a hair along normal
            Point3D n = best.normalWorld;
            contactPos = contactPos.add(n.multiply(1e-4));

            // Slide/bounce
            double vn = v.dotProduct(n);
            Point3D vN = n.multiply(vn);
            
            // ... after computing: Point3D contactPos (world), Point3D n (world), and you still have 'v' (world)
            double speedN = Math.abs(vn);

            // Notify HUD (and any other listeners) via the bus
            GameEventBus.fire(new CollisionEvent(
                    CollisionEvent.SHIP_COLLISION,
                    contactPos,
                    n,
                    speedN,
                    colliderNode
            ));
            
            
            Point3D vT = v.subtract(vN);

            Point3D vAfter = vT.multiply(Math.max(0.0, 1.0 - friction))
                    .subtract(vN.multiply(Math.max(0.0, 1.0 + restitution)));

            // Prepare remainder
            double consumed = remaining * tStep;
            remaining -= consumed;
            p0 = contactPos;
            v  = vAfter;
        }

        if (any) {
            craft.setWorldPosition(p0);
            craft.setVelocity(v);
        } else {
            // even without collision we should advance position by dt to keep parity with a pure integrator,
            // but your integrator likely already advanced before contributors run.
            // If contributors are the integrator, uncomment:
            // craft.setWorldPosition(p0);
            // craft.setVelocity(v);
        }
    }

    // --- helpers ---

    private static final class SweepHit {
        final Point3D pointWorld;
        final Point3D normalWorld;
        SweepHit(Point3D pointWorld, Point3D normalWorld) {
            this.pointWorld = pointWorld; this.normalWorld = normalWorld;
        }
    }

    private static void collectMeshViews(Node n, List<MeshView> out) {
        if (n instanceof MeshView mv) {
            out.add(mv);
        } else if (n instanceof Parent p) {
            for (Node c : p.getChildrenUnmodifiable()) collectMeshViews(c, out);
        }
    }

    /** Transform a SCENE-space vector to WORLD-ROOT at a SCENE position (no explicit inverse). */
    private Point3D sceneVectorToWorldAt(Point3D vecScene, Point3D atScene) {
        Point3D p0 = worldRoot.sceneToLocal(atScene);
        Point3D p1 = worldRoot.sceneToLocal(atScene.add(vecScene));
        return p1.subtract(p0);
    }

    private static double clamp01(double x) { return x < 0 ? 0 : (x > 1 ? 1 : x); }
    private static Point3D normalize(Point3D v) {
        double m = Math.sqrt(v.getX()*v.getX() + v.getY()*v.getY() + v.getZ()*v.getZ());
        if (m < 1e-8) return new Point3D(0,1,0);
        return new Point3D(v.getX()/m, v.getY()/m, v.getZ()/m);
    }
}
