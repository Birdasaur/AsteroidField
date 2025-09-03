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
 * Tether logic in parent/world-root space.
 * Adds "persist-on-miss" and uses a large Cylinder-based view with debug markers.
 */
public class Tether {

    public interface SpacecraftAdapter {
        Point3D getWorldPosition();
        void applyPull(Point3D acceleration, double dtSeconds);
    }

    private final int id;
    private final Group parent3D;
    private final TetherView view;

    // Config (tuned for visibility)
    private double projectileSpeed = 250;   // units/s
    private double maxRange = 4000;         // units
    private double aabbInflation = 1.2;     // margin around collidables
    private double reelRate = 200;          // units/s when pulling
    private double pullAccel = 60;          // baseline accel when taut
    private double tetherRadius = 1.0;      // BIG for visibility with Cylinder
    private double viewStartOffset = 1.0;   // push base forward to avoid near-plane clipping

    private boolean debugPersistOnMiss = true;
    private boolean persistActive = false;
    private Point3D persistDir = null;      // normalized, in parent/world space

    // Runtime
    private TetherState state = TetherState.IDLE;
    private Point3D fireOrigin = Point3D.ZERO;
    private Point3D fireDir = new Point3D(0, 0, 1);
    private double tipDist = 0;
    private double prevTipDist = 0;
    private double restLength = 0;

    private Node attachedNode = null;
    private Point3D anchorLocal = null;
    private Point3D anchorWorld = null;

    private final Supplier<List<Node>> collidablesSupplier;
    private final SpacecraftAdapter craft;
    private boolean pulling = false;

    public Tether(int id, Group parent3D, Supplier<List<Node>> collidablesSupplier,
                  SpacecraftAdapter craft, Color color) {
        this.id = id;
        this.parent3D = parent3D;
        this.collidablesSupplier = collidablesSupplier;
        this.craft = craft;

        // Use our Cylinder-based view; color controls the beam color
        this.view = new TetherView(0, (float) tetherRadius, color);
//        this.view.setShowMarkers(true); // show big start/end spheres (debug)
        parent3D.getChildren().add(view);
        setVisible(false);
    }

    // Exposed toggles
    public void setDebugPersistOnMiss(boolean v){
        this.debugPersistOnMiss = v;
        if (!v) persistActive = false;
    }
    public TetherState getState(){ return state; }
    public boolean isAttached(){ return state == TetherState.ATTACHED; }
    public void setVisible(boolean v){ view.setVisibleAndPickOnBounds(v); }
    public void setShowStartMarker(boolean show) { view.setShowStartMarker(show); }
    public void setShowEndMarker(boolean show)   { view.setShowEndMarker(show); }
    public void setMarkerVisibility(boolean showStart, boolean showEnd) {
        view.setMarkerVisibility(showStart, showEnd);
    }
    public boolean isStartMarkerVisible() { return view.isStartMarkerVisible(); }
    public boolean isEndMarkerVisible()   { return view.isEndMarkerVisible(); }
    
    public void fireFrom(Point3D origin, Point3D dir) {
        persistActive = false;
        persistDir = null;
        if (state == TetherState.FIRING || state == TetherState.ATTACHED) return;

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
        persistActive = false;
        persistDir = null;
        setVisible(false);
    }

    public void setPulling(boolean pulling) { this.pulling = pulling; }

    public void update(double dtSeconds) {
        switch (state) {
            case IDLE -> { /* no-op */ }
            case FIRING -> updateFiring(dtSeconds);
            case ATTACHED -> updateAttached(dtSeconds);
            case DETACHED -> { /* allow persisted debug line to render */ }
        }

        if (persistActive && state == TetherState.DETACHED && persistDir != null) {
            Point3D start = craft.getWorldPosition();
            Point3D dir = persistDir;
            Point3D end = start.add(dir.multiply(maxRange));
            Point3D startBase = start.add(dir.normalize().multiply(viewStartOffset));
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
                state = TetherState.DETACHED; // keep drawing via persist path
                return;
            } else {
                release();
                return;
            }
        }

        Point3D tipPrev = fireOrigin.add(fireDir.multiply(prevTipDist));
        Point3D tipNow  = fireOrigin.add(fireDir.multiply(tipDist));

        // Segment vs inflated AABB in parent/world space
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
        Point3D startBase = craftPos.add(fireDir.normalize().multiply(viewStartOffset));

        if (bestNode != null) {
            attachedNode = bestNode;
            anchorLocal = attachedNode.parentToLocal(bestHit);
            anchorWorld = bestHit;
            restLength = craftPos.distance(bestHit);
            state = TetherState.ATTACHED;
            persistActive = false;
            persistDir = null;
            view.setStartAndEnd(startBase, bestHit);
        } else {
            view.setStartAndEnd(startBase, tipNow);
        }
    }

    private void updateAttached(double dt) {
        Point3D start = craft.getWorldPosition();
        anchorWorld = (attachedNode != null)
                ? attachedNode.localToParent(anchorLocal)
                : anchorWorld;

        if (anchorWorld == null) { release(); return; }

        Point3D toAnchor = anchorWorld.subtract(start);
        double dist = toAnchor.magnitude();
        if (dist < 1e-6) return;

        Point3D dir = toAnchor.normalize();
        Point3D startBase = start.add(dir.multiply(viewStartOffset));
        view.setStartAndEnd(startBase, anchorWorld);

        if (pulling) {
            restLength = Math.max(0.0, restLength - reelRate * dt);
        }

        if (dist > restLength) {
            double stretch = dist - restLength;
            double k = pullAccel;
            Point3D accel = dir.multiply(k * (stretch / Math.max(dist, 1e-6)));
            craft.applyPull(accel, dt);
        }
    }
}
