package AsteroidField.spacecraft.collision;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.shape.MeshView;

/**
 * Continuous swept-sphere collision for the spacecraft.
 * - Input/Output position and velocity are in the WORLD-ROOT (your 'world' Group) local space.
 * - Internally converts to SCENE space for accurate mesh tests, then converts back.
 */
public final class SpacecraftCollider {

    public static final class Result {
        public final Point3D position;
        public final Point3D velocity;
        public final boolean collided;
        public Result(Point3D position, Point3D velocity, boolean collided) {
            this.position = position;
            this.velocity = velocity;
            this.collided = collided;
        }
    }

    private final Node worldRoot;
    private double radius;

    private double restitution = 0.05; // small bounce
    private double friction = 0.15;    // tangential damping
    private int maxIterations = 2;     // TOI splits per frame
    private boolean frontFaceOnly = false; // start permissive; set true when windings are verified

    public SpacecraftCollider(Node worldRoot, double radius) {
        this.worldRoot = worldRoot;
        this.radius = radius;
    }

    public void setRestitution(double r) { this.restitution = Math.max(0.0, r); }
    public void setFriction(double f)    { this.friction = Math.max(0.0, f); }
    public void setMaxIterations(int n)  { this.maxIterations = Math.max(1, n); }
    public void setFrontFaceOnly(boolean v) { this.frontFaceOnly = v; }
    public double getRadius() { return radius; }

    public Result moveAndCollide(Point3D posWorld, Point3D velWorld, double dt, List<Node> collidables) {
        Point3D p0 = posWorld;
        Point3D v  = velWorld;
        double remaining = dt;
        boolean any = false;

        List<MeshView> meshes = new ArrayList<>();
        for (Node n : collidables) collectMeshViews(n, meshes);

        for (int iter = 0; iter < maxIterations && remaining > 1e-6; iter++) {
            Point3D p1 = p0.add(v.multiply(remaining));

            // Convert path to SCENE space for ray tests
            Point3D p0Scene = worldRoot.localToScene(p0);
            Point3D p1Scene = worldRoot.localToScene(p1);

            double bestT = Double.POSITIVE_INFINITY;
            SpacecraftColliderHit best = null;

            for (MeshView mv : meshes) {
                Optional<SweepSphereMesh.Hit> h = SweepSphereMesh.firstHit(
                        mv, p0Scene, p1Scene, radius, frontFaceOnly);
                if (h.isPresent() && h.get().t < bestT) {
                    bestT = h.get().t;
                    // Convert hit back to WORLD-ROOT space
                    Point3D hitScene = h.get().pointScene;
                    Point3D hitWorld = worldRoot.sceneToLocal(hitScene);
                    // Normal: scene -> world (two-point delta at the hit position)
                    Point3D nWorld = sceneVectorToWorldAt(h.get().normalScene, hitScene);
                    best = new SpacecraftColliderHit(mv, bestT, hitWorld, normalize(nWorld));
                }
            }

            if (best == null) {
                // No collision in this slice: accept full step
                p0 = p1;
                break;
            }

            any = true;

            // Move to contact
            double tStep = clamp01(best.t);
            Point3D contactPos = p0.add(v.multiply(remaining * tStep));
            Point3D n = best.normalWorld;

            // Separate a hair
            contactPos = contactPos.add(n.multiply(1e-4));

            // Reflect/slide velocity
            double vn = v.dotProduct(n);
            Point3D vN = n.multiply(vn);
            Point3D vT = v.subtract(vN);

            Point3D vAfter = vT.multiply(Math.max(0.0, 1.0 - friction))
                    .subtract(vN.multiply(Math.max(0.0, 1.0 + restitution)));

            // Prepare remainder
            double consumed = remaining * tStep;
            remaining -= consumed;
            p0 = contactPos;
            v  = vAfter;
        }

        return new Result(p0, v, any);
    }

    // --- helpers ---

    private static final class SpacecraftColliderHit {
        final MeshView mv;
        final double t;
        final Point3D pointWorld;
        final Point3D normalWorld;
        SpacecraftColliderHit(MeshView mv, double t, Point3D pointWorld, Point3D normalWorld) {
            this.mv = mv; this.t = t; this.pointWorld = pointWorld; this.normalWorld = normalWorld;
        }
    }

    private static void collectMeshViews(Node n, List<MeshView> out) {
        if (n instanceof MeshView mv) {
            out.add(mv);
        } else if (n instanceof Parent p) {
            for (Node c : p.getChildrenUnmodifiable()) collectMeshViews(c, out);
        }
    }

    /** Transform a SCENE-space vector to WORLD-ROOT space at a SCENE point (two-point delta). */
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
