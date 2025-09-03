package AsteroidField.tether;

import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Supplier;

/**
 * Force-based tether with spring-damper mechanics and smooth reeling.
 * Uses parent/world-root coordinates throughout.
 */
public class Tether {

    public interface SpacecraftAdapter {
        Point3D getWorldPosition();
        Point3D getVelocity();
        double getMass();
        void applyForce(Point3D force);

        // Compatibility with older usage if needed:
        default void applyPull(Point3D acceleration, double dtSeconds) {
            applyForce(acceleration.multiply(getMass()));
        }
    }

    private final int id;
    private final Group parent3D;
    private final TetherView view;

    // Projectile/firing
    private double projectileSpeed = 250;
    private double maxRange = 4000;
    private double aabbInflation = 1.2;

    // Rope mechanics
    private double reelRate = 200;        // units/s
    private double restLength = 0;
    private final double minRestLength = 2.0;
    private double viewStartOffset = 0.5; // visual nudge along direction

    // Spring-damper params
    private double stiffness = 160.0;     // k (N/m)
    private double dampingRatio = 0.9;    // ζ (1 = critical)
    private double maxForce = 900.0;      // clamp per tether
    private double slackEps = 0.02;
    private double perpDampingRatio = 0.15; // 0.0–0.3 typical

    // State
    private final Supplier<List<Node>> collidablesSupplier;
    private final SpacecraftAdapter craft;
    private TetherState state = TetherState.IDLE;
    private boolean pulling = false;

    // Firing state
    private Point3D fireOrigin = Point3D.ZERO;
    private Point3D fireDir = new Point3D(0, 0, 1);
    private double tipDist = 0, prevTipDist = 0;

    // Anchor
    private Node attachedNode = null;
    private Point3D anchorLocal = null;
    private Point3D anchorWorld = null;

    // Where rope leaves the craft (parent/world space), sticks to “belly/wings”
    private Point3D emitterOffsetParent = Point3D.ZERO;

    // Debug
    private boolean debugPersistOnMiss = false;
    private boolean persistActive = false;
    private Point3D persistDir = null;

    public Tether(int id, Group parent3D, Supplier<List<Node>> collidablesSupplier,
                  SpacecraftAdapter craft, Color color) {
        this.id = id;
        this.parent3D = parent3D;
        this.collidablesSupplier = collidablesSupplier;
        this.craft = craft;

        this.view = new TetherView(0, /*radius placeholder*/ 3.0f, color);
        parent3D.getChildren().add(view);
        setVisible(false);
    }

    // --- config ---
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
    public double getStiffness()     { return stiffness; }
    public double getDampingRatio()  { return dampingRatio; }
    public double getPerpDampingRatio()  { return perpDampingRatio; }
    public double getReelRate()      { return reelRate; }
    public double getMaxForce()      { return maxForce; }
    public double getSlackEps()      { return slackEps; }

    public void setDebugPersistOnMiss(boolean v){ this.debugPersistOnMiss = v; if (!v) persistActive = false; }

    public TetherState getState(){ return state; }
    public boolean isAttached(){ return state == TetherState.ATTACHED; }
    public void setVisible(boolean v){ view.setVisibleAndPickOnBounds(v); }

    public void setShowStartMarker(boolean show) { view.setShowStartMarker(show); }
    public void setShowEndMarker(boolean show)   { view.setShowEndMarker(show); }

    // --- lifecycle ---
    public void fireFrom(Point3D origin, Point3D dir) {
        persistActive = false; persistDir = null;
        if (state == TetherState.FIRING || state == TetherState.ATTACHED) return;

        // remember where the emitter is relative to the craft
        Point3D craftPosNow = craft.getWorldPosition();
        this.emitterOffsetParent = origin.subtract(craftPosNow);

        this.fireOrigin = origin;
        this.fireDir = dir.normalize();
        this.prevTipDist = 0;
        this.tipDist = 0;
        this.restLength = 0;
        this.attachedNode = null;
        this.anchorLocal = null;
        this.anchorWorld = null;
        state = TetherState.FIRING;
        setVisible(true);
    }

    public void release() {
        state = TetherState.DETACHED;
        attachedNode = null;
        anchorLocal = null;
        anchorWorld = null;
        tipDist = 0;
        prevTipDist = 0;
        restLength = 0;
        emitterOffsetParent = Point3D.ZERO;
        persistActive = false;
        persistDir = null;
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
        // persisted debug beam on miss
        if (persistActive && state == TetherState.DETACHED && persistDir != null) {
            Point3D start = craft.getWorldPosition().add(emitterOffsetParent);
            Point3D end = start.add(persistDir.multiply(maxRange));
            Point3D startBase = start.add(persistDir.multiply(viewStartOffset));
            setVisible(true);
            view.setStartAndEnd(startBase, end);
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

        Point3D tipPrev = fireOrigin.add(fireDir.multiply(prevTipDist));
        Point3D tipNow  = fireOrigin.add(fireDir.multiply(tipDist));

        // segment vs AABB
        List<Node> collidables = collidablesSupplier.get();
        double bestT = Double.POSITIVE_INFINITY;
        Node bestNode = null;
        Point3D bestHit = null;

        for (Node n : collidables) {
            Bounds b = n.getBoundsInParent();
            Bounds ib = Intersections.inflate(b, aabbInflation);
            OptionalDouble hitT = Intersections.segmentAabbFirstHit(tipPrev, tipNow, ib);
            if (hitT.isPresent()) {
                double t = hitT.getAsDouble();
                if (t < bestT) {
                    bestT = t;
                    bestNode = n;
                    Point3D seg = tipNow.subtract(tipPrev);
                    bestHit = tipPrev.add(seg.multiply(t));
                }
            }
        }

        Point3D craftPos = craft.getWorldPosition();
        Point3D start    = craftPos.add(emitterOffsetParent);
        Point3D startBase = start.add(fireDir.multiply(viewStartOffset));

        if (bestNode != null) {
            attachedNode = bestNode;
            anchorLocal = attachedNode.parentToLocal(bestHit);
            anchorWorld = bestHit;
            restLength = Math.max(minRestLength, start.distance(bestHit));
            state = TetherState.ATTACHED;
            view.setStartAndEnd(startBase, bestHit);
        } else {
            view.setStartAndEnd(startBase, tipNow);
        }
    }

    private void updateAttached(double dt) {
        // start follows craft + emitter offset
        Point3D craftPos = craft.getWorldPosition();
        Point3D start = craftPos.add(emitterOffsetParent);

        // anchor follows target (if it moves)
        anchorWorld = (attachedNode != null)
                ? attachedNode.localToParent(anchorLocal)
                : anchorWorld;

        if (anchorWorld == null) { release(); return; }

        // reel
        if (pulling) restLength = Math.max(minRestLength, restLength - reelRate * dt);

        Point3D toAnchor = anchorWorld.subtract(start);
        double dist = toAnchor.magnitude();
        if (dist < 1e-6) {
            view.setStartAndEnd(start, anchorWorld);
            return;
        }

        Point3D dir = toAnchor.normalize();

        // Visual
        Point3D startBase = start.add(dir.multiply(viewStartOffset));
        view.setStartAndEnd(startBase, anchorWorld);

        // Spring-damper force if taut
        double stretch = dist - restLength;
        if (stretch > slackEps) {
            double m = Math.max(0.001, craft.getMass());
            double k = stiffness;
            double c = 2.0 * dampingRatio * Math.sqrt(k * m); // along-axis critical-ish

            double vAlong = craft.getVelocity().dotProduct(dir);
            double forceMag = (k * stretch) - (c * vAlong);
            if (forceMag < 0) forceMag = 0;
            if (forceMag > maxForce) forceMag = maxForce;
            craft.applyForce(dir.multiply(forceMag));

            // --- NEW: small perpendicular damping ---
            var v        = craft.getVelocity();
            var vParallel= dir.multiply(vAlong);
            var vPerp    = v.subtract(vParallel);
            double cPerp = perpDampingRatio * 2.0 * Math.sqrt(k * m); // scaled to “feel right”
            craft.applyForce(vPerp.multiply(-cPerp));
        }
    }
}
