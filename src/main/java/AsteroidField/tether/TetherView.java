package AsteroidField.tether;

import AsteroidField.tether.materials.ProceduralTetherMaterial;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Affine;
import javafx.util.Duration;

/**
 * Beam between two points using a single Affine to orient a Cylinder.
 * Integrates a cached procedural material for high performance.
 */
public class TetherView extends Group {

    private final Cylinder beam;
    private final Affine   xform;

    // Procedural material (cached images; fast swaps)
    private final ProceduralTetherMaterial pmat;

    // Color animation façade (compatible with your existing API)
    private final ObjectProperty<Color> beamColor = new SimpleObjectProperty<>();
    private Timeline colorFadeTL;

    // Thickness pulse
    private Timeline thicknessTL;

    // Debug markers
    private final Sphere startMarker;
    private final Sphere endMarker;

    // Independent toggles (default off for debug-only visuals)
    private boolean showStartMarker = false;
    private boolean showEndMarker   = false;

    // Geometry + throttling
    private final double radius;
    private double lastLen = -1;
    private long   lastStaticEnsureNanos = 0;
    private static final long ENSURE_INTERVAL_NS = 50_000_000L; // 50ms min interval

    // Optional: lock visual state (not strictly needed for perf mode)
    private boolean lockedVisual = false;

    public TetherView(int radialDivisionsIgnored, float radius, Color color) {
        this.radius = radius;

        // Beam geometry
        beam = new Cylinder(radius, 1.0);
        beam.setCullFace(CullFace.NONE);
        beam.setDrawMode(DrawMode.FILL);
        beam.setMouseTransparent(true);

        xform = new Affine();
        beam.getTransforms().add(xform);

        // Procedural material
        pmat = new ProceduralTetherMaterial();
        pmat.ensureStaticMaps(radius); // set diffuse/bump once
        pmat.setBaseColor(color != null ? color : Color.CYAN);
        pmat.setEmissiveStrength(0.0);
        beam.setMaterial(pmat.material());

        // Expose a simple “beamColor” property to preserve your existing API;
        // It routes to pmat.setBaseColor (fast) and does NOT rebuild textures.
        beamColor.addListener((obs, ov, nv) -> {
            if (nv != null) {
                pmat.setBaseColor(nv);
            }
        });
        beamColor.set(color != null ? color : Color.CYAN);

        // Debug markers
        startMarker = new Sphere(radius * 1.8);
        startMarker.setMaterial(new javafx.scene.paint.PhongMaterial(Color.LIMEGREEN));
        startMarker.setCullFace(CullFace.NONE);
        startMarker.setDrawMode(DrawMode.FILL);
        startMarker.setMouseTransparent(true);

        endMarker = new Sphere(radius * 2.2);
        endMarker.setMaterial(new javafx.scene.paint.PhongMaterial(Color.RED));
        endMarker.setCullFace(CullFace.NONE);
        endMarker.setDrawMode(DrawMode.FILL);
        endMarker.setMouseTransparent(true);

        getChildren().addAll(beam, startMarker, endMarker);
        setVisible(false);
        updateMarkerVisibility();
    }

    // --- Independent marker controls ---
    public void setShowStartMarker(boolean show) {
        this.showStartMarker = show;
        updateMarkerVisibility();
    }
    public void setShowEndMarker(boolean show) {
        this.showEndMarker = show;
        updateMarkerVisibility();
    }
    public void setMarkerVisibility(boolean showStart, boolean showEnd) {
        this.showStartMarker = showStart;
        this.showEndMarker   = showEnd;
        updateMarkerVisibility();
    }
    public boolean isStartMarkerVisible() { return showStartMarker; }
    public boolean isEndMarkerVisible()   { return showEndMarker; }

    private void updateMarkerVisibility() {
        startMarker.setVisible(showStartMarker);
        endMarker.setVisible(showEndMarker);
    }

    public void setVisibleAndPickOnBounds(boolean visible) {
        setVisible(visible);
        setPickOnBounds(visible);
    }

    /** Main update: place beam (cylinder) between start and end, and move debug markers. */
    public void setStartAndEnd(Point3D start, Point3D end) {
        // Markers
        startMarker.setTranslateX(start.getX());
        startMarker.setTranslateY(start.getY());
        startMarker.setTranslateZ(start.getZ());

        endMarker.setTranslateX(end.getX());
        endMarker.setTranslateY(end.getY());
        endMarker.setTranslateZ(end.getZ());

        // Segment math
        Point3D dir = end.subtract(start);
        double len = dir.magnitude();
        if (len < 1e-6) len = 1e-6;
        Point3D v   = dir.normalize();         // target for local +Y
        Point3D mid = start.midpoint(end);     // cylinder center in world

        // Build an orthonormal frame: right (X), up (Z), v (Y)
        Point3D refUp = (Math.abs(v.getY()) < 0.99) ? new Point3D(0, 1, 0) : new Point3D(1, 0, 0);
        Point3D right = v.crossProduct(refUp);
        double rmag = right.magnitude();
        if (rmag < 1e-6) {
            refUp = new Point3D(0, 0, 1);
            right = v.crossProduct(refUp);
            rmag = right.magnitude();
            if (rmag < 1e-6) right = new Point3D(1, 0, 0);
        }
        right = right.normalize();
        Point3D up = right.crossProduct(v).normalize();

        // Set cylinder length (centered at origin in its local space)
        beam.setHeight(len);

        // Affine from basis vectors (columns) and translation (midpoint)
        xform.setMxx(right.getX()); xform.setMxy(v.getX()); xform.setMxz(up.getX()); xform.setTx(mid.getX());
        xform.setMyx(right.getY()); xform.setMyy(v.getY()); xform.setMyz(up.getY()); xform.setTy(mid.getY());
        xform.setMzx(right.getZ()); xform.setMzy(v.getZ()); xform.setMzz(up.getZ()); xform.setTz(mid.getZ());

        // Throttle static-map ensure (cheap, but avoid spamming)
        long now = System.nanoTime();
        if (Math.abs(len - lastLen) > 1e-5 && (now - lastStaticEnsureNanos) > ENSURE_INTERVAL_NS) {
            pmat.ensureStaticMaps(radius); // single cache lookup; no painting
            lastStaticEnsureNanos = now;
        }
        lastLen = len;
    }

    // ---------------------------
    // Visual polish conveniences
    // ---------------------------

    /** Immediately set the beam’s "base" color (routes to procedural material tint). */
    public void setBeamColor(Color c) {
        if (c == null) return;
        stopColorFade();
        beamColor.set(c);
    }

    /** Smooth color fade (updates tint; no texture rebuild). */
    public void fadeBeamColor(Color target, Duration dur) {
        if (target == null) return;
        if (dur == null || dur.lessThanOrEqualTo(Duration.ZERO)) {
            setBeamColor(target);
            return;
        }
        stopColorFade();

        Color from = beamColor.get();
        final int steps = Math.max(1, (int)Math.ceil(dur.toMillis() / 16.0)); // ~60fps
        Timeline painter = new Timeline();
        painter.setCycleCount(steps + 1);
        for (int i = 0; i <= steps; i++) {
            double t = (double)i / steps;
            Color c = lerpColor(from, target, t);
            painter.getKeyFrames().add(new KeyFrame(Duration.millis(dur.toMillis() * t),
                    e -> beamColor.set(c)));
        }
        colorFadeTL = painter;
        colorFadeTL.play();
    }

    private void stopColorFade() {
        if (colorFadeTL != null) {
            colorFadeTL.stop();
            colorFadeTL = null;
        }
    }

    /** Briefly scales thickness (X/Z only), then returns to 1.0. */
    public void pulseThickness(double maxScale, Duration dur) {
        if (dur == null || dur.lessThanOrEqualTo(Duration.ZERO)) return;
        double peak = Math.max(1.0, maxScale);
        Duration half = dur.divide(2.0);

        if (thicknessTL != null) thicknessTL.stop();
        thicknessTL = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(beam.scaleXProperty(), 1.0, Interpolator.EASE_OUT),
                new KeyValue(beam.scaleZProperty(), 1.0, Interpolator.EASE_OUT)
            ),
            new KeyFrame(half,
                new KeyValue(beam.scaleXProperty(), peak, Interpolator.EASE_OUT),
                new KeyValue(beam.scaleZProperty(), peak, Interpolator.EASE_OUT)
            ),
            new KeyFrame(dur,
                new KeyValue(beam.scaleXProperty(), 1.0, Interpolator.EASE_IN),
                new KeyValue(beam.scaleZProperty(), 1.0, Interpolator.EASE_IN)
            )
        );
        thicknessTL.play();
    }

    // ---- Optional helpers for emissive (fast; cached images) ----

    /** Set the emissive tint used for lock visuals (cached per color bucket). */
    public void setLockTint(Color c) {
        pmat.setLockTint(c);
        // Keep current strength; just ensure correct emissive map chosen
        pmat.setEmissiveStrength(currentEmissiveStrength);
    }

    private double currentEmissiveStrength = 0.0;

    /** Drive emissive intensity (0..1). Uses discrete cached steps internally. */
    public void setEmission(double strength01) {
        currentEmissiveStrength = Math.max(0, Math.min(1, strength01));
        pmat.setEmissiveStrength(currentEmissiveStrength);
    }

    /** Convenience for “locked” look on/off. */
    public void setLockedVisual(boolean locked) {
        this.lockedVisual = locked;
        setEmission(locked ? 0.45 : 0.0);
    }

    // --- utility ---
    private static Color lerpColor(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
            a.getRed()   + (b.getRed()   - a.getRed())   * t,
            a.getGreen() + (b.getGreen() - a.getGreen()) * t,
            a.getBlue()  + (b.getBlue()  - a.getBlue())  * t,
            a.getOpacity()+ (b.getOpacity()- a.getOpacity())* t
        );
    }
}
