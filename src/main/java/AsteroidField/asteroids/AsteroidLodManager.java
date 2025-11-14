package AsteroidField.asteroids;

import AsteroidField.asteroids.field.AsteroidField;
import AsteroidField.asteroids.field.AsteroidInstance;
import AsteroidField.asteroids.parameters.AsteroidParameters;
import AsteroidField.asteroids.providers.AsteroidMeshProvider;
import javafx.animation.AnimationTimer;
import javafx.geometry.Point3D;
import javafx.scene.PerspectiveCamera;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Throttled, hysteresis-based runtime LOD for asteroid MeshViews.
 *
 * - 3 levels: LOD0(High), LOD1(Med), LOD2(Low)
 * - Hysteresis: enter/exit thresholds differ to reduce thrash
 * - Throttle: evaluate at most 'budgetPerFrame' items/frames
 * - Lazy mesh cache: meshes are generated the first time a level is actually needed
 * - Frustum-ish cone test to avoid swapping off-screen
 *
 * Wire-up:
 *   AsteroidLodManager lod = new AsteroidLodManager(camera);
 *   lod.setDistances(6_000, 18_000, 36_000, 1_500); // example distances in world units
 *   lod.setBudgetPerFrame(100);
 *   lod.registerField(field);     // after you build/spawn the field
 *   lod.start();                  // starts internal AnimationTimer
 *
 * You can stop() to pause, clear() to forget everything, and re-register later.
 */
public final class AsteroidLodManager {

    /** Small read-only view of what we need from AsteroidInstance. */
    public interface Source {
        MeshView view();
        String familyName();
        AsteroidParameters params();
        double approxRadius();
        Point3D position();
    }

    private static final class Entry {
        final MeshView view;
        final String family;
        final AsteroidParameters baseParams;
        final double approxR;
        final Point3D pos;

        final TriangleMesh[] lodMeshes = new TriangleMesh[3]; // 0..2
        int currentLevel = -1; // unknown

        Entry(Source src) {
            this.view = src.view();
            this.family = src.familyName();
            this.baseParams = src.params();
            this.approxR = src.approxRadius();
            this.pos = src.position();
        }
    }

    // Camera / update
    private final PerspectiveCamera camera;
    private final AnimationTimer timer;

    // LOD distances (world units), with hysteresis margin
    // We compute squared distances for comparisons.
    private double nearIn = 6_000, midIn = 18_000, farIn = 36_000;
    private double hysteresis = 1_500;

    private double nearIn2, midIn2, farIn2;
    private double nearOut2, midOut2, farOut2;

    // Budget
    private int budgetPerFrame = 120; // evaluate up to N entries/frame
    private final List<Entry> entries = new ArrayList<>();
    private int evalIndex = 0;

    // Randomized order to reduce visible patterns
    private final Random rng = ThreadLocalRandom.current();

    // Only swap if within a forward cone to the camera (very cheap "frustum-ish")
    private double cosHalfFov = Math.cos(Math.toRadians(45)); // ~90° cone; tweak as needed

    public AsteroidLodManager(PerspectiveCamera camera) {
        this.camera = camera;
        recomputeBands();

        this.timer = new AnimationTimer() {
            @Override public void handle(long now) { tick(); }
        };
    }

    // ----------------------------
    // Distances / Hysteresis API
    // ----------------------------

    /** Bulk-set the distance bands (world units). Example: (6000, 18000, 36000, 1500). */
    public void setDistances(double nearIn, double midIn, double farIn, double hysteresisMargin) {
        this.nearIn = Math.max(1, nearIn);
        this.midIn  = Math.max(this.nearIn + 1, midIn);
        this.farIn  = Math.max(this.midIn + 1, farIn);
        this.hysteresis = Math.max(0, hysteresisMargin);
        recomputeBands();
    }

    /** Set only the near (inner) distance band. Keeps ordering with mid/far and recomputes thresholds. */
    public void setNearDistance(double v) {
        this.nearIn = Math.max(1, v);
        // keep ordering: near < mid < far
        if (this.midIn <= this.nearIn) this.midIn = this.nearIn + 1;
        if (this.farIn <= this.midIn)  this.farIn = this.midIn + 1;
        recomputeBands();
    }

    /** Set only the mid distance band. Clamped to be > near and < far (adjusting far if needed). */
    public void setMidDistance(double v) {
        this.midIn = Math.max(this.nearIn + 1, v);
        if (this.farIn <= this.midIn) this.farIn = this.midIn + 1;
        recomputeBands();
    }

    /** Set only the far (outer) distance band. Clamped to be > mid. */
    public void setFarDistance(double v) {
        this.farIn = Math.max(this.midIn + 1, v);
        recomputeBands();
    }

    /** Set only the hysteresis margin (world units) applied to exit thresholds. */
    public void setHysteresis(double margin) {
        this.hysteresis = Math.max(0, margin);
        recomputeBands();
    }

    public double getNearDistance() { return nearIn; }
    public double getMidDistance()  { return midIn; }
    public double getFarDistance()  { return farIn; }
    public double getHysteresis()   { return hysteresis; }

    // ----------------------------
    // Other tuning knobs
    // ----------------------------

    /** Set the maximum number of asteroids to re-evaluate per frame. */
    public void setBudgetPerFrame(int n) {
        this.budgetPerFrame = Math.max(1, n);
    }

    public int getBudgetPerFrame() { return budgetPerFrame; }

    /** Tighten/loosen the forward cone gate (degrees from camera forward). */
    public void setForwardConeDegrees(double halfAngleDeg) {
        halfAngleDeg = Math.max(1, Math.min(89, halfAngleDeg));
        cosHalfFov = Math.cos(Math.toRadians(halfAngleDeg));
    }

    public double getForwardConeDeg() {
        // inverse of cosHalfFov to expose current half-angle
        return Math.toDegrees(Math.acos(cosHalfFov));
    }

    // ----------------------------
    // Registration / lifecycle
    // ----------------------------

    /** Register a whole field at once. */
    public void registerField(AsteroidField field) {
        for (AsteroidInstance ai : field.instances) {
            register(toSource(ai));
        }
        shuffle(); // randomized order to amortize cost
    }

    /** Register a single asteroid. You may call this any time. */
    public void register(Source src) {
        entries.add(new Entry(src));
    }

    /** Clear all registered asteroids and cached meshes. */
    public void clear() {
        entries.clear();
        evalIndex = 0;
    }

    /** Start/stop the internal update timer. */
    public void start() { timer.start(); }
    public void stop()  { timer.stop(); }

    // --- internals ---

    private void recomputeBands() {
        nearIn2 = nearIn * nearIn;
        midIn2  = midIn  * midIn;
        farIn2  = farIn  * farIn;

        // Exit thresholds (larger) for hysteresis: once in a tighter band,
        // we'll keep you there until you cross the larger "out" distance.
        nearOut2 = (nearIn + hysteresis) * (nearIn + hysteresis);
        midOut2  = (midIn  + hysteresis) * (midIn  + hysteresis);
        farOut2  = (farIn  + hysteresis) * (farIn  + hysteresis);
    }

    private void shuffle() {
        Collections.shuffle(entries, rng);
        evalIndex = 0;
    }

    private void tick() {
        if (entries.isEmpty()) return;

        final int n = entries.size();
        int budget = Math.min(budgetPerFrame, n);

        // Camera position & forward
        Point3D camWorld = camera.localToScene(Point3D.ZERO);
        Point3D fw = camera.getLocalToSceneTransform().deltaTransform(0, 0, 1).normalize();

        for (int i = 0; i < budget; i++) {
            if (evalIndex >= n) evalIndex = 0;
            Entry e = entries.get(evalIndex++);
            updateLod(e, camWorld, fw);
        }
    }

    private void updateLod(Entry e, Point3D camWorld, Point3D camForward) {
        // Distance^2
        double dx = e.pos.getX() - camWorld.getX();
        double dy = e.pos.getY() - camWorld.getY();
        double dz = e.pos.getZ() - camWorld.getZ();
        double d2 = dx*dx + dy*dy + dz*dz;

        int targetLevel = decideLevel(e.currentLevel, d2);

        if (targetLevel == e.currentLevel) return;

        // Gate by forward cone to avoid swapping off-screen (cheap)
        double dist = Math.sqrt(d2);
        if (dist > 1e-6) {
            double dot = (dx * camForward.getX() + dy * camForward.getY() + dz * camForward.getZ()) / dist;
            if (dot < cosHalfFov) return; // outside cone, delay swap
        }

        TriangleMesh mesh = getOrBuildMeshForLevel(e, targetLevel);
        if (mesh != null) {
            e.view.setMesh(mesh);
            e.currentLevel = targetLevel;
        }
    }

    /** Hysteresis-aware banding: once you've picked a level, you need to exit the larger band to change it. */
    private int decideLevel(int current, double d2) {
        if (current < 0) {
            // First time: simple non-hysteresis pick
            if (d2 <= nearIn2) return 0;
            if (d2 <= midIn2)  return 1;
            return 2;
        }
        switch (current) {
            case 0: // currently High; only drop when you exceed the "out" bound
                if (d2 > nearOut2) {
                    return (d2 <= midIn2 ? 1 : 2);
                }
                return 0;
            case 1: // currently Medium
                if (d2 <= nearIn2) return 0;               // move up when well inside near band
                if (d2 > midOut2)  return 2;               // drop when beyond mid OUT
                return 1;
            case 2: // currently Low
                if (d2 <= midIn2)  return (d2 <= nearIn2 ? 0 : 1); // move up only when inside tighter bands
                return 2;
            default:
                return 2;
        }
    }

    private TriangleMesh getOrBuildMeshForLevel(Entry e, int level) {
        TriangleMesh m = e.lodMeshes[level];
        if (m != null) return m;

        AsteroidMeshProvider provider = AsteroidMeshProvider.PROVIDERS.get(e.family);
        if (provider == null) return null;

        AsteroidParameters p = adjustedParamsForLevel(e.baseParams, level);
        m = provider.generateMesh(p);
        e.lodMeshes[level] = m;
        return m;
    }

    /** Map your base params → (High/Med/Low) by reducing subdivisions; keep other fields as-is. */
    private AsteroidParameters adjustedParamsForLevel(AsteroidParameters base, int level) {
        int baseSub = Math.max(0, base.getSubdivisions());
        int targetSub = switch (level) {
            case 0 -> baseSub;                  // High = original
            case 1 -> Math.max(1, baseSub - 1); // Medium
            default -> Math.max(0, baseSub - 2);// Low
        };
        return base.toBuilder()
                .subdivisions(targetSub)
                .build();
    }

    // Adapter from your existing AsteroidInstance (no changes required in your generator)
    private static Source toSource(AsteroidInstance ai) {
        return new Source() {
            @Override public MeshView view() { return ai.node(); }
            @Override public String familyName() { return ai.familyName(); }
            @Override public AsteroidParameters params() { return ai.params(); }
            @Override public double approxRadius() { return ai.approxRadius(); }
            @Override public Point3D position() { return ai.position(); }
        };
    }
}
