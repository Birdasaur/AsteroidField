package AsteroidField.tether;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Affine;
import javafx.util.Duration;

/**
 * Tether view that uses a single Affine to orient the cylinder.
 * - Cylinder is centered at its local origin, height runs along local +Y from -h/2..+h/2
 * - We set height = |end-start|
 * - Build an orthonormal basis where local Y maps to (end-start).normalize()
 * - Place the cylinder center at the midpoint
 *
 * Adds:
 *   - setBeamColor(Color)        : instant color change
 *   - fadeBeamColor(Color, dur)  : smooth color fade
 *   - pulseThickness(scale, dur) : temporary thickness bump (scaleX/Z only)
 *
 * Start/End markers remain independently togglable for debugging.
 */
public class TetherView extends Group {

    private final Cylinder beam;
    private final Affine   xform;

    // Material + color handling
    private final PhongMaterial beamMat;
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

    public TetherView(int radialDivisionsIgnored, float radius, Color color) {
        // Beam
        beam = new Cylinder(radius, 1.0);
        beamMat = new PhongMaterial(color != null ? color : Color.CYAN);
        beam.setMaterial(beamMat);
        beam.setCullFace(CullFace.NONE);
        beam.setDrawMode(DrawMode.FILL);      // max visibility
        beam.setMouseTransparent(true);       // never block picking

        // Keep diffuse color in sync with a property so we can animate it
        beamColor.addListener((obs, ov, nv) -> {
            if (nv != null) {
                beamMat.setDiffuseColor(nv);
                // Optional: specular pop for readability
                beamMat.setSpecularColor(Color.WHITE);
                beamMat.setSpecularPower(64);
            }
        });
        beamColor.set(beamMat.getDiffuseColor());

        // Transform used to orient/position the beam
        xform = new Affine();
        beam.getTransforms().add(xform);

        // Debug markers (mouseTransparent so they don't block ray casts)
        startMarker = new Sphere(radius * 1.8);
        startMarker.setMaterial(new PhongMaterial(Color.LIMEGREEN));
        startMarker.setCullFace(CullFace.NONE);
        startMarker.setDrawMode(DrawMode.FILL);
        startMarker.setMouseTransparent(true);

        endMarker = new Sphere(radius * 2.2);
        endMarker.setMaterial(new PhongMaterial(Color.RED));
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
    }

    // ---------------------------
    // Visual polish conveniences
    // ---------------------------

    /** Immediately set the beam’s diffuse color. */
    public void setBeamColor(Color c) {
        if (c == null) return;
        stopColorFade();
        beamColor.set(c);
    }

    /**
     * Smoothly fade the beam’s color to 'target' over 'dur'.
     * If another fade is in progress it will be replaced.
     */
    public void fadeBeamColor(Color target, Duration dur) {
        if (target == null) return;
        if (dur == null || dur.lessThanOrEqualTo(Duration.ZERO)) {
            setBeamColor(target);
            return;
        }
        stopColorFade();

        // Animate color by interpolating channels manually
        Color from = beamColor.get();
        colorFadeTL = new Timeline(
            new KeyFrame(Duration.ZERO,
                new KeyValue(new javafx.beans.property.SimpleDoubleProperty(0), 0, Interpolator.LINEAR)
            ),
            new KeyFrame(dur,
                new KeyValue(new javafx.beans.property.SimpleDoubleProperty(1), 1, Interpolator.LINEAR)
            )
        );
        // Use a pulse-driven onFinished + on every pulse update via a runnable KeyFrame
        colorFadeTL.getKeyFrames().clear();
        final int steps = Math.max(1, (int)Math.ceil(dur.toMillis() / 16.0)); // ~60fps
        for (int i = 0; i <= steps; i++) {
            double t = (double)i / steps;
            Color c = lerpColor(from, target, t);
            colorFadeTL.getKeyFrames().add(new KeyFrame(Duration.millis(dur.toMillis() * t),
                new KeyValue(beam.opacityProperty(), beam.getOpacity(), Interpolator.DISCRETE) // dummy kv to schedule frame
            ));
            final Color frameColor = c;
            colorFadeTL.currentTimeProperty().addListener((obs, ov, nv) -> {
                // This listener fires many times; keep it lightweight
                // To avoid redundant sets, only update when the timeline's current time equals this keyframe time.
            });
        }
        // Simpler approach: use a single timeline with a custom onFinished + AnimationTimer-like pulse via KeyFrames
        // but to keep it robust and simple, just use another small Timeline that calls set every 16ms:
        Timeline painter = new Timeline();
        painter.setCycleCount(steps + 1);
        for (int i = 0; i <= steps; i++) {
            double t = (double)i / steps;
            Color c = lerpColor(from, target, t);
            painter.getKeyFrames().add(new KeyFrame(Duration.millis(dur.toMillis() * t), e -> beamColor.set(c)));
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
