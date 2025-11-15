package AsteroidField.spacecraft.collision;

import AsteroidField.events.AsteroidFieldEvent;
import AsteroidField.events.CollisionEvent;
import AsteroidField.events.GameEventBus;
import AsteroidField.physics.PhysicsContributor;
import AsteroidField.spacecraft.CameraKinematicAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.shape.MeshView;

/**
 * Continuous swept-sphere collision solver for the spacecraft, executed during the COLLISION physics phase.
 *
 * Optimizations:
 *  - Velocity/displacement gating (skip when effectively idle)
 *  - Frequency cap (~30Hz) with fast-motion override
 *  - Cached collidable MeshViews + cached SCENE-space bounding spheres (refresh on field attach/detach via markMeshesDirty())
 *  - Per-sweep broadphase using segment-to-sphere distance to prune candidates before firstHit()
 *  - Detailed perf logging (retained), incl. skip counters and candidate counts
 */
public final class SpacecraftCollisionContributor implements PhysicsContributor {

    // --- PERF LOGGING (enable with -Dperf.logs=true) ---
    private static final boolean PERF = true; //Boolean.getBoolean("perf.logs");
    private static final long PERF_WINDOW_NS = 1_000_000_000L; // 1 s
    private long perfWinStartNs = 0L;

    // Accumulators (1s window)
    private long perfGetListNsAcc = 0L;     // time in collidables.get() (only when cache refreshes)
    private long perfFlattenNsAcc = 0L;     // time flattening to MeshViews (only on refresh)
    private long perfBoundsBuildNsAcc = 0L; // time computing bounds (only on refresh)
    private long perfTransformsNsAcc = 0L;  // time for local<->scene transforms in the sweep loop
    private long perfBroadphaseNsAcc = 0L;  // time spent filtering candidates by sphere distance
    private long perfSweepNsAcc = 0L;       // time in per-mesh precise sweep tests (firstHit loop)
    private int  perfMeshesLast = 0;        // size of cachedMeshes
    private int  perfItersAcc = 0;          // outer iterations executed
    private int  perfHitsAcc = 0;           // total hits reported
    private double perfSpeedLast = 0.0;     // last speed magnitude
    private double perfSpeedMax = 0.0;      // max speed within window
    private int  perfSkippedIdleAcc = 0;    // times skipped due to idle gates
    private int  perfSkippedThrottleAcc = 0;// times skipped due to 30Hz throttle
    private int  perfCandidatesAcc = 0;     // total candidate count across sweeps
    private int  perfSweepsAcc = 0;         // number of sweep executions in window

    // --- Core state ---
    private final Node worldRoot;
    private final CameraKinematicAdapter craft;
    private final Supplier<List<Node>> collidables;
    private final double radius; // craft radius (WORLD/SCENE units)

    // --- Physics tuning ---
    private double restitution = 0.05;
    private double friction = 0.15;
    private int    maxIterations = 2;
    private boolean frontFaceOnly = true;

    private boolean enabled = true;

    // --- Optimization knobs ---
    /** Skip sweep when |v| < minSpeedGate (m/s). */
    private double minSpeedGate = 0.05;
    /** Skip sweep when displacement (|v|*dt) < radius * minDispRatio. */
    private double minDispRatio = 0.01;
    /** Run sweep every N physics substeps (~120Hz/N). */
    private int sweepIntervalSteps = 4; // ~30Hz if physics is 120Hz
    /** If |v| > fastSpeedBoost * radius / dt, override throttle (run every step). */
    private double fastSpeedBoost = 2.0;
    /** Safety padding added to asteroid bounds for broadphase (scene units). */
    private double broadphaseMargin = 0.25;

    // --- Throttle/caching runtime ---
    private int stepCounter = 0;

    /** Cached collidable mesh views (static while field attached). */
    private final List<MeshView> cachedMeshes = new ArrayList<>();

    /** Cached SCENE-space bounding spheres parallel to cachedMeshes (same indices). */
    private final List<SphereBound> cachedBounds = new ArrayList<>();

    /** Flag to rebuild caches on next sweep. */
    private volatile boolean meshesDirty = true;

    public SpacecraftCollisionContributor(Node worldRoot,
                                          CameraKinematicAdapter craft,
                                          Supplier<List<Node>> collidables,
                                          double craftRadius) {
        this.worldRoot = worldRoot;
        this.craft = craft;
        this.collidables = collidables;
        this.radius = craftRadius;

        markMeshesDirty(); // ensure initial cache refresh

        // Listen for world lifecycle changes and invalidate cache automatically
        GameEventBus.addHandler(AsteroidFieldEvent.ATTACHED,  e -> markMeshesDirty());
        GameEventBus.addHandler(AsteroidFieldEvent.DETACHED, e -> markMeshesDirty());
    }

    // --- Public controls / hooks ---

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isEnabled() { return enabled; }

    public void setRestitution(double r) { this.restitution = Math.max(0.0, r); }
    public void setFriction(double f) { this.friction = Math.max(0.0, f); }
    public void setMaxIterations(int n) { this.maxIterations = Math.max(1, n); }
    public void setFrontFaceOnly(boolean v) { this.frontFaceOnly = v; }

    /** Call this when the asteroid field attaches/detaches. */
    public void markMeshesDirty() { meshesDirty = true; }

    // Optional tuning exposure (e.g., debug UI)
    public void setMinSpeedGate(double v) { this.minSpeedGate = Math.max(0.0, v); }
    public void setMinDispRatio(double v) { this.minDispRatio = Math.max(0.0, v); }
    public void setSweepIntervalSteps(int n) { this.sweepIntervalSteps = Math.max(1, n); }
    public void setFastSpeedBoost(double v) { this.fastSpeedBoost = Math.max(0.0, v); }
    public void setBroadphaseMargin(double v) { this.broadphaseMargin = Math.max(0.0, v); }

    @Override
    public void step(double dt) {
        if (!enabled) {
            emitPerfIfDue();
            return;
        }

        Point3D p0 = craft.getWorldPosition();  // WORLD-ROOT space
        Point3D v  = craft.getVelocity();

        // --- PERF: track speed
        final double speed = Math.sqrt(v.getX()*v.getX() + v.getY()*v.getY() + v.getZ()*v.getZ());
        if (PERF) {
            perfSpeedLast = speed;
            if (speed > perfSpeedMax) perfSpeedMax = speed;
        }

        // Decide whether to run the sweep this substep
        boolean runSweep = true;

        // 1) Idle gate: skip when effectively not moving
        final double disp = speed * dt;
        if (speed < minSpeedGate || disp < (radius * minDispRatio)) {
            runSweep = false;
            if (PERF) perfSkippedIdleAcc++;
        }

        // 2) Frequency cap (~30Hz) unless moving fast (avoid tunneling)
        stepCounter++;
        final boolean fast = speed > (fastSpeedBoost * radius / Math.max(1e-9, dt));
        if (runSweep && !fast && (stepCounter % sweepIntervalSteps) != 0) {
            runSweep = false;
            if (PERF) perfSkippedThrottleAcc++;
        }

        // 3) Ensure cached collidables & bounds when we intend to sweep
        if (runSweep) {
            ensureMeshesAndBoundsCached();
            if (cachedMeshes.isEmpty()) {
                runSweep = false; // nothing to collide with
            }
        }

        // --- Sweep loop (only if runSweep) ---
        double remaining = dt;
        boolean any = false;
        Node colliderNode = null;

        if (runSweep) {
            // Prep SCENE-space endpoints once (firstHit expects SCENE)
            // We convert WORLD-ROOT (local) to SCENE here
            for (int iter = 0; iter < maxIterations && remaining > 1e-6; iter++) {
                final long tX0 = PERF ? System.nanoTime() : 0L;
                Point3D p1 = p0.add(v.multiply(remaining));
                Point3D p0Scene = worldRoot.localToScene(p0);
                Point3D p1Scene = worldRoot.localToScene(p1);
                if (PERF) perfTransformsNsAcc += System.nanoTime() - tX0;

                // --- Broadphase: prune by cached sphere bounds (SCENE space) ---
                final long tBp0 = PERF ? System.nanoTime() : 0L;
                int n = cachedMeshes.size();
                // Collect candidates by scanning bounds and performing segment-to-sphere distance test
                // Allocate on stack; keep indices for direct MeshView access
                List<Integer> candidates = new ArrayList<>(Math.min(64, n));
                double inflate = radius + broadphaseMargin;
                for (int i = 0; i < n; i++) {
                    SphereBound sb = cachedBounds.get(i);
                    double r = sb.radius + inflate;
                    if (segmentPointDistanceSq(p0Scene, p1Scene, sb.center) <= r * r) {
                        candidates.add(i);
                    }
                }
                if (PERF) {
                    perfBroadphaseNsAcc += System.nanoTime() - tBp0;
                    perfCandidatesAcc += candidates.size();
                    perfSweepsAcc++;
                }

                // --- Narrow phase: precise sweep only on candidates ---
                double bestT = Double.POSITIVE_INFINITY;
                SweepHit best = null;

                final long tSweep0 = PERF ? System.nanoTime() : 0L;
                for (int idx : candidates) {
                    MeshView mv = cachedMeshes.get(idx);
                    Optional<SweepSphereMesh.Hit> h = SweepSphereMesh.firstHit(
                            mv, p0Scene, p1Scene, radius, frontFaceOnly);
                    if (h.isPresent() && h.get().t < bestT) {
                        bestT = h.get().t;

                        // Convert hit back to WORLD
                        final long tX1 = PERF ? System.nanoTime() : 0L;
                        Point3D hitWorld = worldRoot.sceneToLocal(h.get().pointScene);
                        Point3D nWorld   = sceneVectorToWorldAt(h.get().normalScene, h.get().pointScene);
                        if (PERF) perfTransformsNsAcc += System.nanoTime() - tX1;

                        best = new SweepHit(hitWorld, normalize(nWorld));
                        colliderNode = mv;
                    }
                }
                if (PERF) {
                    perfSweepNsAcc += System.nanoTime() - tSweep0;
                    perfItersAcc++;
                }

                if (best == null) {
                    // No collision this slice: accept full step
                    p0 = p1;
                    break;
                }

                any = true;

                // Move to contact, separate a hair
                double tStep = clamp01(bestT);
                Point3D contactPos = p0.add(v.multiply(remaining * tStep));
                Point3D nWorld = best.normalWorld;
                contactPos = contactPos.add(nWorld.multiply(1e-4));

                // Slide/bounce
                double vn = v.dotProduct(nWorld);
                Point3D vN = nWorld.multiply(vn);
                double speedN = Math.abs(vn);

                GameEventBus.fire(new CollisionEvent(
                        CollisionEvent.SHIP_COLLISION,
                        contactPos,
                        nWorld,
                        speedN,
                        colliderNode
                ));
                if (PERF) {
                    perfHitsAcc++;
                }

                Point3D vT = v.subtract(vN);
                Point3D vAfter = vT.multiply(Math.max(0.0, 1.0 - friction))
                                   .subtract(vN.multiply(Math.max(0.0, 1.0 + restitution)));

                // Prepare remainder
                double consumed = remaining * tStep;
                remaining -= consumed;
                p0 = contactPos;
                v  = vAfter;
            }
        }

        if (any) {
            craft.setWorldPosition(p0);
            craft.setVelocity(v);
        }

        // Emit perf once/second (even on skip paths)
        emitPerfIfDue();
    }

    @Override
    public AsteroidField.physics.PhysicsPhase getPhase() {
        return AsteroidField.physics.PhysicsPhase.COLLISION;
    }

    @Override
    public int getPriority() { return 0; }

    // --- helpers ---

    private static final class SweepHit {
        final Point3D pointWorld;
        final Point3D normalWorld;
        SweepHit(Point3D pointWorld, Point3D normalWorld) {
            this.pointWorld = pointWorld; this.normalWorld = normalWorld;
        }
    }

    /** Simple sphere bound cached in SCENE space. */
    private static final class SphereBound {
        final Point3D center; // SCENE
        final double radius;  // SCENE units
        SphereBound(Point3D c, double r) { this.center = c; this.radius = r; }
    }

    /** Rebuild cachedMeshes + cachedBounds (SCENE-space spheres), only when dirty. */
    private void ensureMeshesAndBoundsCached() {
        if (!meshesDirty) return;

        final long tGet0 = PERF ? System.nanoTime() : 0L;
        final List<Node> nodes = collidables.get();
        if (PERF) perfGetListNsAcc += System.nanoTime() - tGet0;

        final long tFlat0 = PERF ? System.nanoTime() : 0L;
        cachedMeshes.clear();
        for (Node n : nodes) collectMeshViews(n, cachedMeshes);
        if (PERF) perfFlattenNsAcc += System.nanoTime() - tFlat0;

        final long tBnd0 = PERF ? System.nanoTime() : 0L;
        cachedBounds.clear();
//        cachedBounds.ensureCapacity(cachedMeshes.size());
        for (MeshView mv : cachedMeshes) {
            cachedBounds.add(computeSceneSphereBound(mv));
        }
        if (PERF) perfBoundsBuildNsAcc += System.nanoTime() - tBnd0;

        meshesDirty = false;
        perfMeshesLast = cachedMeshes.size();
    }

    /** Build a conservative SCENE-space bounding sphere from a node's local bounds. */
    private static SphereBound computeSceneSphereBound(MeshView mv) {
        // Use local bounds' center, then transform to SCENE.
        Bounds bl = mv.getBoundsInLocal();
        Point3D centerLocal = new Point3D(
                (bl.getMinX() + bl.getMaxX()) * 0.5,
                (bl.getMinY() + bl.getMaxY()) * 0.5,
                (bl.getMinZ() + bl.getMaxZ()) * 0.5
        );
        Point3D centerScene = mv.localToScene(centerLocal);

        // Approx radius: transform all 8 corners to scene and take max distance to center
        double maxR2 = 0.0;
        double[] xs = { bl.getMinX(), bl.getMaxX() };
        double[] ys = { bl.getMinY(), bl.getMaxY() };
        double[] zs = { bl.getMinZ(), bl.getMaxZ() };
        for (double x : xs) for (double y : ys) for (double z : zs) {
            Point3D pScene = mv.localToScene(x, y, z);
            double dx = pScene.getX() - centerScene.getX();
            double dy = pScene.getY() - centerScene.getY();
            double dz = pScene.getZ() - centerScene.getZ();
            double r2 = dx*dx + dy*dy + dz*dz;
            if (r2 > maxR2) maxR2 = r2;
        }
        double radiusScene = Math.sqrt(maxR2);
        // Fallback for degenerate bounds
        if (!(radiusScene > 0)) radiusScene = 0.5 * Math.max(
                Math.max(bl.getWidth(), bl.getHeight()), bl.getDepth());

        return new SphereBound(centerScene, radiusScene);
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

    /** Squared distance from a point to a segment p0->p1 (all in same space). */
    private static double segmentPointDistanceSq(Point3D p0, Point3D p1, Point3D c) {
        double vx = p1.getX() - p0.getX();
        double vy = p1.getY() - p0.getY();
        double vz = p1.getZ() - p0.getZ();
        double wx = c.getX() - p0.getX();
        double wy = c.getY() - p0.getY();
        double wz = c.getZ() - p0.getZ();

        double vv = vx*vx + vy*vy + vz*vz;
        if (vv <= 1e-12) {
            // Degenerate segment; use distance to p0
            return wx*wx + wy*wy + wz*wz;
        }
        double t = (wx*vx + wy*vy + wz*vz) / vv;
        if (t < 0) t = 0;
        else if (t > 1) t = 1;

        double px = p0.getX() + t * vx;
        double py = p0.getY() + t * vy;
        double pz = p0.getZ() + t * vz;

        double dx = c.getX() - px;
        double dy = c.getY() - py;
        double dz = c.getZ() - pz;
        return dx*dx + dy*dy + dz*dz;
    }

    // --- PERF emission ---

    private void emitPerfIfDue() {
        if (!PERF) return;

        if (perfWinStartNs == 0L) perfWinStartNs = System.nanoTime();
        long elapsed = System.nanoTime() - perfWinStartNs;
        if (elapsed >= PERF_WINDOW_NS) {
            double avgCandidates = (perfSweepsAcc > 0) ? (double) perfCandidatesAcc / perfSweepsAcc : 0.0;
            System.out.printf(
                "[PERF] t=%d, Collision, get_ms=%.3f, flatten_ms=%.3f, bounds_ms=%.3f, broad_ms=%.3f, sweep_ms=%.3f, xform_ms=%.3f, meshes=%d, iters=%d, hits=%d, sweeps=%d, cand_avg=%.1f, speed=%.3f, speed_max=%.3f, skipped_idle=%d, skipped_throttle=%d%n",
                System.currentTimeMillis(),
                perfGetListNsAcc / 1_000_000.0,
                perfFlattenNsAcc / 1_000_000.0,
                perfBoundsBuildNsAcc / 1_000_000.0,
                perfBroadphaseNsAcc / 1_000_000.0,
                perfSweepNsAcc / 1_000_000.0,
                perfTransformsNsAcc / 1_000_000.0,
                perfMeshesLast,
                perfItersAcc,
                perfHitsAcc,
                perfSweepsAcc,
                avgCandidates,
                perfSpeedLast,
                perfSpeedMax,
                perfSkippedIdleAcc,
                perfSkippedThrottleAcc
            );

            // reset window
            perfWinStartNs = System.nanoTime();
            perfGetListNsAcc = 0L;
            perfFlattenNsAcc = 0L;
            perfBoundsBuildNsAcc = 0L;
            perfTransformsNsAcc = 0L;
            perfBroadphaseNsAcc = 0L;
            perfSweepNsAcc = 0L;
            // perfMeshesLast persists (useful as a context indicator)
            perfItersAcc = 0;
            perfHitsAcc = 0;
            perfSpeedMax = 0.0;
            perfSkippedIdleAcc = 0;
            perfSkippedThrottleAcc = 0;
            perfCandidatesAcc = 0;
            perfSweepsAcc = 0;
        }
    }
}
