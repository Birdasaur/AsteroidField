package AsteroidField.tether;

import AsteroidField.tether.vfx.AnchorSparkVFX;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.shape.MeshView;
import javafx.util.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Supplier;

/**
 * Tether with scene-robust anchoring and triangle-precise ray hits.
 * Diagnostic-tolerant: can test both face modes and optionally fall back to AABB for MeshViews.
 */
public class Tether {

    public interface SpacecraftAdapter {
        Point3D getWorldPosition();
        Point3D getVelocity();
        double getMass();
        void applyForce(Point3D force);
        default void applyPull(Point3D acceleration, double dtSeconds) {
            applyForce(acceleration.multiply(getMass()));
        }
    }

    private final int id;
    private final Group parent3D;
    private final TetherView tetherView;
    private final Supplier<List<Node>> collidablesSupplier;
    private final SpacecraftAdapter craft;

    private final Color baseColor;
    private final Color lockColor = Color.LIMEGREEN;

    // --- Config ---
    // Track visual marker flags so we can report them back via getters.
    private boolean showStartMarker = false;
    private boolean showEndMarker = false;
    private double projectileSpeed = 250;
    private double maxRange = 4000;
    private double aabbInflation = 1.2;
    private double reelRate = 200;
    private double restLength = 0;
    private final double minRestLength = 2.0;
    private double viewStartOffset = 0.5;
    private double stiffness = 160.0;
    private double dampingRatio = 0.9;
    private double maxForce = 900.0;
    private double slackEps = 0.02;
    private double perpDampingRatio = 0.15;

    // NEW: diagnostic knobs
    private boolean rayFrontFaceOnly = false;            // be permissive until windings verified
    private boolean allowAabbFallbackOnMeshMiss = false; // use only while bringing things up

    // --- State ---
    private TetherState state = TetherState.IDLE;
    private boolean pulling = false;

    private Point3D fireOrigin = Point3D.ZERO; // parent3D space
    private Point3D fireDir = new Point3D(0, 0, 1);
    private double tipDist = 0, prevTipDist = 0;

    private Node attachedNode = null;
    private Point3D anchorLocal = null;       // attached node's LOCAL
    private Point3D anchorWorld = null;       // parent3D space
    private Point3D attachNormalWorld = null; // parent3D space

    private Point3D emitterOffsetParent = Point3D.ZERO;

    private boolean debugPersistOnMiss = false;
    private boolean persistActive = false;
    private Point3D persistDir = null;

    private boolean wasAttached = false;

    public Tether(int id, Group parent3D, Supplier<List<Node>> collidablesSupplier,
                  SpacecraftAdapter craft, Color color) {
        this.id = id;
        this.parent3D = parent3D;
        this.collidablesSupplier = collidablesSupplier;
        this.craft = craft;

        this.baseColor = (color != null) ? color : Color.CYAN;
        this.tetherView = new TetherView(0, 3.0f, this.baseColor);
        this.tetherView.setBeamColor(this.baseColor);
        parent3D.getChildren().add(tetherView);
        setVisible(false);
    }

    // --- Lifecycle ---
    public void fireFrom(Point3D origin, Point3D dir) {
        persistActive = false; persistDir = null;
        if (state == TetherState.FIRING || state == TetherState.ATTACHED) return;

        Point3D craftPosNow = craft.getWorldPosition();
        this.emitterOffsetParent = origin.subtract(craftPosNow);

        this.fireOrigin = origin;
        this.fireDir = dir.normalize();
        this.tipDist = 0;
        this.prevTipDist = 0;

        this.attachedNode = null;
        this.anchorLocal = null;
        this.anchorWorld = null;
        this.attachNormalWorld = null;
        this.wasAttached = false;

        state = TetherState.FIRING;
        setVisible(true);
        tetherView.setBeamColor(baseColor);
    }

    public void release() {
        state = TetherState.DETACHED;
        attachedNode = null;
        anchorLocal = null;
        anchorWorld = null;
        attachNormalWorld = null;
        tipDist = 0;
        prevTipDist = 0;
        restLength = 0;
        emitterOffsetParent = Point3D.ZERO;
        persistActive = false;
        persistDir = null;
        wasAttached = false;
        tetherView.setBeamColor(baseColor);
        setVisible(false);
    }

    public void setPulling(boolean pulling) { this.pulling = pulling; }

    public void update(double dt) {
        switch (state) {
            case IDLE -> {}
            case FIRING -> updateFiring(dt);
            case ATTACHED -> updateAttached(dt);
            case DETACHED -> {}
        }
        if (persistActive && state == TetherState.DETACHED && persistDir != null) {
            Point3D start = craft.getWorldPosition().add(emitterOffsetParent);
            Point3D end = start.add(persistDir.multiply(maxRange));
            Point3D startBase = start.add(persistDir.multiply(viewStartOffset));
            setVisible(true);
            tetherView.setStartAndEnd(startBase, end);
        }
    }

    private void updateFiring(double dt) {
        prevTipDist = tipDist;
        tipDist += projectileSpeed * dt;

        if (tipDist > maxRange) {
            if (debugPersistOnMiss) {
                persistActive = true;
                persistDir = fireDir;
                state = TetherState.DETACHED;
                return;
            } else { release(); return; }
        }

        // Segment in parent3D space
        Point3D tipPrev = fireOrigin.add(fireDir.multiply(prevTipDist));
        Point3D tipNow  = fireOrigin.add(fireDir.multiply(tipDist));

        // Same segment in SCENE space
        Point3D tipPrevScene = parent3D.localToScene(tipPrev);
        Point3D tipNowScene  = parent3D.localToScene(tipNow);

        List<Node> collidables = collidablesSupplier.get();

        double nearestT = Double.POSITIVE_INFINITY;
        Node   nearestNode = null;
        Point3D nearestHitWorld = null;   // parent3D space
        Point3D nearestNormalWorld = null;

        for (Node n : collidables) {
            // AABB gate in SCENE space
            Bounds bScene = n.localToScene(n.getBoundsInLocal());
            Bounds ibScene = inflateBounds(bScene, aabbInflation);

            OptionalDouble tAabb = segmentAabbFirstHitScene(tipPrevScene, tipNowScene, ibScene);
            if (tAabb.isEmpty()) continue;

            if (n instanceof MeshView mv) {
                // 1) Try strict front-face (if requested)
                Optional<MeshRaycast.TriHit> tri = MeshRaycast.segmentMeshFirstHit(
                        mv, tipPrevScene, tipNowScene, rayFrontFaceOnly);

                // 2) If strict failed and we care about robustness, try both-sides
                if ((!tri.isPresent()) && rayFrontFaceOnly) {
                    tri = MeshRaycast.segmentMeshFirstHit(mv, tipPrevScene, tipNowScene, false);
                }

                if (tri.isPresent()) {
                    MeshRaycast.TriHit h = tri.get();
                    if (h.t < nearestT) {
                        nearestT = h.t;
                        nearestNode = mv;

                        // Scene → parent3D
                        Point3D hitWorldRoot = parent3D.sceneToLocal(h.pointWorld);
                        Point3D nWorldRootVec = sceneVectorToParentLocalAt(h.normalWorld, h.pointWorld);
                        Point3D nWorldRoot = normalizeSafe(nWorldRootVec);

                        nearestHitWorld = hitWorldRoot;
                        nearestNormalWorld = nWorldRoot;
                    }
                } else if (allowAabbFallbackOnMeshMiss) {
                    // Fallback attach on the AABB intersection point (approximate but useful in bring-up)
                    double t = tAabb.getAsDouble();
                    if (t < nearestT) {
                        nearestT = t;
                        nearestNode = mv;
                        Point3D segScene = tipNowScene.subtract(tipPrevScene);
                        Point3D hitScene = tipPrevScene.add(segScene.multiply(t));
                        nearestHitWorld = parent3D.sceneToLocal(hitScene);
                        nearestNormalWorld = estimateOutwardNormalWorld(n, nearestHitWorld, fireDir);
                    }
                }

            } else {
                // Non-mesh collidable: use AABB intersection
                double t = tAabb.getAsDouble();
                if (t < nearestT) {
                    nearestT = t;
                    nearestNode = n;
                    Point3D segScene = tipNowScene.subtract(tipPrevScene);
                    Point3D hitScene = tipPrevScene.add(segScene.multiply(t));
                    nearestHitWorld = parent3D.sceneToLocal(hitScene);
                    nearestNormalWorld = estimateOutwardNormalWorld(n, nearestHitWorld, fireDir);
                }
            }
        }

        Point3D craftPos = craft.getWorldPosition();
        Point3D start    = craftPos.add(emitterOffsetParent);
        Point3D startBase = start.add(fireDir.multiply(viewStartOffset));

        if (nearestNode != null) {
            attachedNode = nearestNode;

            // Store anchor in attached node's LOCAL
            Point3D hitScene = parent3D.localToScene(nearestHitWorld);
            anchorLocal = attachedNode.sceneToLocal(hitScene);

            // Resolve anchor to parent space now
            anchorWorld = parent3D.sceneToLocal(attachedNode.localToScene(anchorLocal));

            attachNormalWorld = (nearestNormalWorld != null)
                    ? nearestNormalWorld
                    : estimateOutwardNormalWorld(attachedNode, nearestHitWorld, fireDir);

            restLength = Math.max(minRestLength, start.distance(anchorWorld));
            state = TetherState.ATTACHED;

            tetherView.setStartAndEnd(startBase, anchorWorld);
            return;
        }

        // No hit yet: extend the beam
        tetherView.setStartAndEnd(startBase, tipNow);
    }

    private void updateAttached(double dt) {
        Point3D craftPos = craft.getWorldPosition();
        Point3D start = craftPos.add(emitterOffsetParent);

        if (attachedNode != null && anchorLocal != null) {
            anchorWorld = parent3D.sceneToLocal(attachedNode.localToScene(anchorLocal));
        }
        if (anchorWorld == null) { release(); return; }

        if (!wasAttached) {
            onAttachedFirstTime(anchorWorld,
                    (attachNormalWorld != null)
                            ? attachNormalWorld
                            : estimateOutwardNormalWorld(attachedNode, anchorWorld, anchorWorld.subtract(start).normalize()));
        }

        if (pulling) restLength = Math.max(minRestLength, restLength - reelRate * dt);

        Point3D toAnchor = anchorWorld.subtract(start);
        double dist = toAnchor.magnitude();
        if (dist < 1e-6) {
            tetherView.setStartAndEnd(start, anchorWorld);
            return;
        }

        Point3D dir = toAnchor.normalize();
        Point3D startBase = start.add(dir.multiply(viewStartOffset));
        tetherView.setStartAndEnd(startBase, anchorWorld);

        double stretch = dist - restLength;
        if (stretch > slackEps) {
            double m = Math.max(0.001, craft.getMass());
            double k = stiffness;
            double c = 2.0 * dampingRatio * Math.sqrt(k * m);

            double vAlong = craft.getVelocity().dotProduct(dir);
            double forceMag = (k * stretch) - (c * vAlong);
            if (forceMag < 0) forceMag = 0;
            if (forceMag > maxForce) forceMag = maxForce;
            craft.applyForce(dir.multiply(forceMag));

            Point3D v = craft.getVelocity();
            Point3D vParallel = dir.multiply(vAlong);
            Point3D vPerp = v.subtract(vParallel);
            double cPerp = perpDampingRatio * 2.0 * Math.sqrt(k * m);
            craft.applyForce(vPerp.multiply(-cPerp));
        }
    }

    private void onAttachedFirstTime(Point3D hitWorld, Point3D outwardNormal) {
        if (wasAttached) return;
        wasAttached = true;
        AnchorSparkVFX.play(parent3D, hitWorld, outwardNormal, Color.YELLOW);
        tetherView.setBeamColor(lockColor);
        tetherView.pulseThickness(1.8, Duration.millis(160));
    }

    private Point3D estimateOutwardNormalWorld(Node node, Point3D hitWorldRoot, Point3D fallbackDir) {
        try {
            if (node != null) {
                Bounds bLocal = node.getBoundsInLocal();
                double cx = (bLocal.getMinX() + bLocal.getMaxX()) * 0.5;
                double cy = (bLocal.getMinY() + bLocal.getMaxY()) * 0.5;
                double cz = (bLocal.getMinZ() + bLocal.getMaxZ()) * 0.5;
                Point3D centerScene = node.localToScene(new Point3D(cx, cy, cz));
                Point3D centerWorldRoot = parent3D.sceneToLocal(centerScene);
                Point3D normal = hitWorldRoot.subtract(centerWorldRoot);
                if (normal.magnitude() > 1e-6) return normal.normalize();
            }
        } catch (Exception ignore) {}
        return (fallbackDir != null && fallbackDir.magnitude() > 1e-6)
                ? fallbackDir.normalize()
                : new Point3D(0, 0, 1);
    }

    // --- geometry helpers ---

    /** Convert a SCENE-space direction vector into parent3D-local at a given scene position. */
    private Point3D sceneVectorToParentLocalAt(Point3D vecScene, Point3D atScene) {
        Point3D p0 = parent3D.sceneToLocal(atScene);
        Point3D p1 = parent3D.sceneToLocal(atScene.add(vecScene));
        return p1.subtract(p0);
    }
    // --- Setters ---
    public void setProjectileSpeed(double v){ projectileSpeed = v; }
    public void setMaxRange(double v){ maxRange = v; }
    public void setAabbInflation(double v){ aabbInflation = v; }
    public void setReelRate(double v){ reelRate = v; }
    public void setViewStartOffset(double v){ viewStartOffset = Math.max(0, v); }
    public void setStiffness(double k){ stiffness = Math.max(0, k); }
    public void setDampingRatio(double z){ dampingRatio = Math.max(0, z); }
    public void setPerpDampingRatio(double r) { perpDampingRatio = Math.max(0, r); }
    public void setMaxForce(double f){ maxForce = Math.max(0, f); }
    public void setSlackEps(double s){ slackEps = Math.max(0, s); }
    public void setDebugPersistOnMiss(boolean v){ this.debugPersistOnMiss = v; if (!v) persistActive = false; }
    public void setRayFrontFaceOnly(boolean v){ this.rayFrontFaceOnly = v; }
    public void setAllowAabbFallbackOnMeshMiss(boolean v){ this.allowAabbFallbackOnMeshMiss = v; }
    public void setVisible(boolean v){ tetherView.setVisibleAndPickOnBounds(v); }
    public void setShowStartMarker(boolean show) {
        this.showStartMarker = show;
        tetherView.setShowStartMarker(show);
    }

    public void setShowEndMarker(boolean show) {
        this.showEndMarker = show;
        tetherView.setShowEndMarker(show);
    }
    // --- Getters ---
    public double getProjectileSpeed() { return projectileSpeed; }
    public double getMaxRange() { return maxRange; }
    public double getAabbInflation() { return aabbInflation; }
    public double getReelRate() { return reelRate; }
    public double getViewStartOffset() { return viewStartOffset; }
    public double getStiffness() { return stiffness; }
    public double getDampingRatio() { return dampingRatio; }
    public double getPerpDampingRatio() { return perpDampingRatio; }
    public double getMaxForce() { return maxForce; }
    public double getSlackEps() { return slackEps; }
    public boolean isDebugPersistOnMiss() { return debugPersistOnMiss; }
    public boolean isRayFrontFaceOnly() { return rayFrontFaceOnly; }
    public boolean isAllowAabbFallbackOnMeshMiss() { return allowAabbFallbackOnMeshMiss; }
    /** Mirrors setVisible(...). Delegates to the underlying Node visibility. */
    public boolean isVisible() { return tetherView.isVisible(); }
    /** Mirrors setShowStartMarker(...). */
    public boolean isShowStartMarker() { return showStartMarker; }
    /** Mirrors setShowEndMarker(...). */
    public boolean isShowEndMarker() { return showEndMarker; }
    public boolean isAttached(){ return state == TetherState.ATTACHED; }
    public TetherState getState(){ return state; }
    
    private static Bounds inflateBounds(Bounds b, double amount) {
        if (amount <= 0) return b;
        return new BoundingBox(
                b.getMinX() - amount, b.getMinY() - amount, b.getMinZ() - amount,
                b.getWidth() + amount * 2.0, b.getHeight() + amount * 2.0, b.getDepth() + amount * 2.0);
    }

    /** Slab test for segment vs AABB in SCENE space. Returns t in [0,1] if hit. */
    private static OptionalDouble segmentAabbFirstHitScene(Point3D a, Point3D b, Bounds box) {
        double tmin = 0.0, tmax = 1.0;

        double[] bbMin = { box.getMinX(), box.getMinY(), box.getMinZ() };
        double[] bbMax = { box.getMaxX(), box.getMaxY(), box.getMaxZ() };
        double[] A = { a.getX(), a.getY(), a.getZ() };
        double[] B = { b.getX(), b.getY(), b.getZ() };
        double[] d = { B[0] - A[0], B[1] - A[1], B[2] - A[2] };

        for (int i = 0; i < 3; i++) {
            double inv = (Math.abs(d[i]) < 1e-12) ? Double.POSITIVE_INFINITY : 1.0 / d[i];
            double t1 = (bbMin[i] - A[i]) * inv;
            double t2 = (bbMax[i] - A[i]) * inv;
            double tNear = Math.min(t1, t2);
            double tFar  = Math.max(t1, t2);
            tmin = Math.max(tmin, tNear);
            tmax = Math.min(tmax, tFar);
            if (tmin > tmax) return OptionalDouble.empty();
        }
        return OptionalDouble.of(tmin);
    }

    private static Point3D normalizeSafe(Point3D v) {
        if (v == null) return null;
        double m = Math.sqrt(v.getX()*v.getX() + v.getY()*v.getY() + v.getZ()*v.getZ());
        if (m < 1e-8) return new Point3D(0,1,0);
        return new Point3D(v.getX()/m, v.getY()/m, v.getZ()/m);
    }
}
