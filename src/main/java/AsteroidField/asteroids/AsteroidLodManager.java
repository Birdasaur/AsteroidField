package AsteroidField.asteroids;

import AsteroidField.asteroids.field.AsteroidField;
import AsteroidField.asteroids.field.AsteroidInstance;
import AsteroidField.asteroids.parameters.AsteroidParameters;
import AsteroidField.asteroids.providers.AsteroidMeshProvider;
import AsteroidField.events.AsteroidFieldEvent;
import javafx.animation.AnimationTimer;
import javafx.event.EventHandler;
import javafx.geometry.Point3D;
import javafx.scene.PerspectiveCamera;
import javafx.scene.paint.Color;
import javafx.scene.paint.Material;
import javafx.scene.paint.PhongMaterial;
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
 * - Optional forward-cone gating (on-screen only)
 * - Optional debug tinting by tier (preserves original material)
 *
 * Typical wiring:
 *   AsteroidLodManager lod = new AsteroidLodManager(camera);
 *   lod.setDistances(5_000, 10_000, 20_000, 800);
 *   lod.setBudgetPerFrame(100);
 *   // register lod as an event handler on the Scene/root for AsteroidFieldEvent.ANY
 *   lod.start();
 */
public final class AsteroidLodManager implements EventHandler<AsteroidFieldEvent> {

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
        final Material originalMaterial;                      // for debug tint restore
        int currentLevel = -1; // unknown

        Entry(Source src) {
            this.view = src.view();
            this.family = src.familyName();
            this.baseParams = src.params();
            this.approxR = src.approxRadius();
            this.pos = src.position();
            this.originalMaterial = src.view().getMaterial(); // may be null; we preserve it
        }
    }

    // Camera / update
    private final PerspectiveCamera camera;
    private final AnimationTimer timer;

    // Master enable (does not stop the timer; simply skips work)
    private boolean enabled = true;

    // Gating: only swap when within a forward cone (on-screen-only)
    private boolean onScreenOnly = true;
    private double cosHalfFov = Math.cos(Math.toRadians(45)); // ~90° cone by default

    // Debug: tint by tier (preserve & restore original materials)
    private boolean tintByTierEnabled = false;
    private final PhongMaterial[] tierTint = new PhongMaterial[] {
        new PhongMaterial(Color.color(0.25, 0.85, 1.0, 1.0)),  // LOD0: cyan-ish
        new PhongMaterial(Color.color(1.0, 0.85, 0.30, 1.0)),  // LOD1: amber-ish
        new PhongMaterial(Color.color(1.0, 0.45, 0.45, 1.0))   // LOD2: red-ish
    };

    // LOD distances (world units), with hysteresis margin; cached squared thresholds
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

    // Event-driven coordination (no field IDs needed)
    private AsteroidField lastAttachedField = null;

    public AsteroidLodManager(PerspectiveCamera camera) {
        this.camera = camera;
        recomputeBands();
        this.timer = new AnimationTimer() {
            @Override public void handle(long now) { tick(); }
        };
    }

    // ----------------------------
    // Lifecycle
    // ----------------------------

    public void start() { timer.start(); }
    public void stop()  { timer.stop();  }

    /** Pause/resume LOD updates without stopping the AnimationTimer. */
    public void setEnabled(boolean v) { this.enabled = v; }
    public boolean isEnabled() { return enabled; }

    // ----------------------------
    // Registration
    // ----------------------------

    /** Register a whole field at once. */
    public void registerField(AsteroidField field) {
        if (field == null) return;
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

    // ----------------------------
    // Tuning: bands & hysteresis
    // ----------------------------

    /** Bulk-set the distance bands (world units). Example: (6000, 18000, 36000, 1500). */
    public void setDistances(double nearIn, double midIn, double farIn, double hysteresisMargin) {
        this.nearIn = Math.max(1, nearIn);
        this.midIn  = Math.max(this.nearIn + 1, midIn);
        this.farIn  = Math.max(this.midIn + 1, farIn);
        this.hysteresis = Math.max(0, hysteresisMargin);
        recomputeBands();
    }

    public void setNearDistance(double v) {
        this.nearIn = Math.max(1, v);
        if (this.midIn <= this.nearIn) this.midIn = this.nearIn + 1;
        if (this.farIn <= this.midIn)  this.farIn = this.midIn + 1;
        recomputeBands();
    }
    public void setMidDistance(double v) {
        this.midIn = Math.max(this.nearIn + 1, v);
        if (this.farIn <= this.midIn) this.farIn = this.midIn + 1;
        recomputeBands();
    }
    public void setFarDistance(double v) {
        this.farIn = Math.max(this.midIn + 1, v);
        recomputeBands();
    }
    public void setHysteresis(double margin) {
        this.hysteresis = Math.max(0, margin);
        recomputeBands();
    }

    public double getNearDistance() { return nearIn; }
    public double getMidDistance()  { return midIn; }
    public double getFarDistance()  { return farIn; }
    public double getHysteresis()   { return hysteresis; }

    // ----------------------------
    // Tuning: budget & screen gating
    // ----------------------------

    /** Evaluate at most this many asteroids per frame. */
    public void setBudgetPerFrame(int n) { this.budgetPerFrame = Math.max(1, n); }
    public int  getBudgetPerFrame() { return budgetPerFrame; }

    /** If true, only swap LODs when within a forward cone (on-screen-ish). Default true. */
    public void setOnScreenOnly(boolean v) { this.onScreenOnly = v; }
    public boolean isOnScreenOnly() { return onScreenOnly; }

    /** Adjust the forward cone's half-angle in degrees (1..89). Default 45° (≈90° cone). */
    public void setForwardConeDegrees(double halfAngleDeg) {
        halfAngleDeg = Math.max(1, Math.min(89, halfAngleDeg));
        cosHalfFov = Math.cos(Math.toRadians(halfAngleDeg));
    }
    public double getForwardConeDegrees() {
        return Math.toDegrees(Math.acos(cosHalfFov));
    }

    // ----------------------------
    // Debug tinting
    // ----------------------------

    /** When enabled, tints asteroids by LOD tier (0 cyan, 1 amber, 2 red). Preserves original material. */
    public void setTintByTierEnabled(boolean v) {
        if (this.tintByTierEnabled == v) return;
        this.tintByTierEnabled = v;

        // If turning OFF, restore materials for all known entries
        if (!v) {
            for (Entry e : entries) {
                e.view.setMaterial(e.originalMaterial);
            }
        } else {
            // Turning ON: apply tint matching current level (if already assigned)
            for (Entry e : entries) {
                if (e.currentLevel >= 0 && e.currentLevel <= 2) {
                    e.view.setMaterial(tierTint[e.currentLevel]);
                }
            }
        }
    }
    public boolean isTintByTierEnabled() { return tintByTierEnabled; }

    // ----------------------------
    // Internals
    // ----------------------------

    private void recomputeBands() {
        nearIn2 = nearIn * nearIn;
        midIn2  = midIn  * midIn;
        farIn2  = farIn  * farIn;

        nearOut2 = sq(nearIn + hysteresis);
        midOut2  = sq(midIn  + hysteresis);
        farOut2  = sq(farIn  + hysteresis);
    }

    private static double sq(double v) { return v * v; }

    private void shuffle() {
        Collections.shuffle(entries, rng);
        evalIndex = 0;
    }

    private void tick() {
        if (!enabled || entries.isEmpty()) return;

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

        // Optional on-screen gating: if required, only swap when within forward cone
        if (onScreenOnly) {
            double dist = Math.sqrt(d2);
            if (dist > 1e-6) {
                double dot = (dx * camForward.getX() + dy * camForward.getY() + dz * camForward.getZ()) / dist;
                if (dot < cosHalfFov) return; // outside cone, delay swap
            }
        }

        TriangleMesh mesh = getOrBuildMeshForLevel(e, targetLevel);
        if (mesh != null) {
            applyLevelChange(e, targetLevel, mesh);
        }
    }

    private void applyLevelChange(Entry e, int targetLevel, TriangleMesh mesh) {
        e.view.setMesh(mesh);
        e.currentLevel = targetLevel;

        if (tintByTierEnabled) {
            e.view.setMaterial(tierTint[targetLevel]);
        } else {
            // keep original material
            e.view.setMaterial(e.originalMaterial);
        }
    }

    /** Hysteresis-aware banding: once you've picked a level, you need to exit the larger band to change it. */
    private int decideLevel(int current, double d2) {
        if (current < 0) {
            if (d2 <= nearIn2) return 0;
            if (d2 <= midIn2)  return 1;
            return 2;
        }
        switch (current) {
            case 0: // currently High
                if (d2 > nearOut2) return (d2 <= midIn2 ? 1 : 2);
                return 0;
            case 1: // currently Medium
                if (d2 <= nearIn2) return 0;
                if (d2 > midOut2)  return 2;
                return 1;
            case 2: // currently Low
                if (d2 <= midIn2)  return (d2 <= nearIn2 ? 0 : 1);
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

    public String debugSummary() {
        return String.format(
            "Entries=%d  Budget=%d  Near=%.0f  Mid=%.0f  Far=%.0f",
            entries.size(), budgetPerFrame, nearIn, midIn, farIn);
    }

    // Adapter from your existing AsteroidInstance
    private static Source toSource(AsteroidInstance ai) {
        return new Source() {
            @Override public MeshView view() { return ai.node(); }
            @Override public String familyName() { return ai.familyName(); }
            @Override public AsteroidParameters params() { return ai.params(); }
            @Override public double approxRadius() { return ai.approxRadius(); }
            @Override public Point3D position() { return ai.position(); }
        };
    }

    // ----------------------------
    // Event handling
    // ----------------------------

    @Override
    public void handle(AsteroidFieldEvent event) {
        final var type = event.getEventType();

        if (type == AsteroidFieldEvent.ATTACHED) {
            AsteroidField f = event.getField();
            // Remember and register the latest field
            lastAttachedField = f;
            clear();
            registerField(f);
        }
        else if (type == AsteroidFieldEvent.DETACHED) {
            // Clear everything and forget the field
            clear();
            lastAttachedField = null;
        }
        else if (type == AsteroidFieldEvent.RESET_REQUEST) {
            // Rebuild LOD state from the last known field (if any)
            clear();
            if (lastAttachedField != null) {
                registerField(lastAttachedField);
            }
        }
    }
}
