package AsteroidField.spacecraft;

import javafx.geometry.Point3D;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;

import java.util.ArrayList;
import java.util.List;

/**
 * Dome-style cockpit frame:
 *  - Base octagon in XZ plane (y=0)
 *  - Meridian ribs from top pole (0,-R,0) to each base vertex, approximated via latitude rings
 *  - Optional latitude rings between top and base
 *  - Bars only (Boxes) -> big "glass" gaps
 *
 * Coordinates:
 *  - +X to the right, +Z "forward", -Y is "up" (top of dome at y = -R)
 */
public class CockpitDome3D extends Group {

    private final Group frame   = new Group();
    private final Group markers = new Group();

    // Sizing
    private double radius   = 200.0;   // dome radius
    private double barSize  = 8.0;     // thickness (Y) and depth (Z) of each bar

    // Structure
    private int meridians   = 8;       // 8 for octagon; can be 6..64
    private int latRings    = 2;       // number of latitude rings between top and base (0..6)

    // Options
    private boolean showMarkers = false;
    private boolean includeBaseRing = true; // outer rim at y=0

    // Materials
    private final PhongMaterial barMat = new PhongMaterial(Color.CYAN);

    public CockpitDome3D() {
        setDepthTest(DepthTest.ENABLE);
        barMat.setSpecularColor(Color.WHITE);
        getChildren().addAll(frame, markers);
        build();
    }

    // ------- Public API -------

    /** Rebuild entire dome with new radius & bar thickness. */
    public void rebuild(double newRadius, double newBarSize) {
        this.radius  = Math.max(1e-6, newRadius);
        this.barSize = Math.max(1e-6, newBarSize);
        build();
    }

    /** Set number of meridians (vertical ribs). Typical: 8..32. */
    public void setMeridians(int m) {
        this.meridians = Math.max(3, m);
        build();
    }

    /** Set count of latitude rings between top & base. */
    public void setLatitudeRings(int count) {
        this.latRings = Math.max(0, Math.min(12, count));
        build();
    }

    /** Show/hide debug markers at key points. */
    public void setShowMarkers(boolean on) {
        this.showMarkers = on;
        markers.setVisible(on);
        if (on && markers.getChildren().isEmpty()) build();
    }

    /** Toggle the base ring at y=0. */
    public void setIncludeBaseRing(boolean on) {
        this.includeBaseRing = on;
        build();
    }

    /** Change bar diffuse color (alpha respected). */
    public void setBarColor(Color c) {
        Color d = barMat.getDiffuseColor();
        barMat.setDiffuseColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), d.getOpacity()));
    }

    /** 0..1 per-node material opacity (DON'T set Group opacity). */
    public void setBarOpacity(double a) {
        a = Math.max(0.0, Math.min(1.0, a));
        Color d = barMat.getDiffuseColor();
        barMat.setDiffuseColor(new Color(d.getRed(), d.getGreen(), d.getBlue(), a));
    }

    // ------- Build -------

    private void build() {
        frame.getChildren().clear();
        markers.getChildren().clear();

        // Points on the dome
        Point3D top = new Point3D(0, -radius, 0); // top pole

        // Base ring vertices (octagon by default) in XZ plane at y=0
        List<Point3D> base = new ArrayList<>(meridians);
        for (int i = 0; i < meridians; i++) {
            double a = (i * 2.0 * Math.PI) / meridians;
            base.add(new Point3D(Math.cos(a) * radius, 0, Math.sin(a) * radius));
        }

        // Latitude rings between top and base (spherical positions)
        // We place rings at equal angular steps from theta=0 (top) to theta=pi/2 (equator)
        List<List<Point3D>> lat = new ArrayList<>();
        for (int r = 1; r <= latRings; r++) {
            double theta = (r * (Math.PI / 2.0)) / (latRings + 1); // exclude top & base
            double y     = -radius * Math.cos(theta);
            double ringR =  radius * Math.sin(theta);
            List<Point3D> ring = new ArrayList<>(meridians);
            for (int i = 0; i < meridians; i++) {
                double a = (i * 2.0 * Math.PI) / meridians;
                ring.add(new Point3D(Math.cos(a) * ringR, y, Math.sin(a) * ringR));
            }
            lat.add(ring);
        }

        // --- Base ring (optional)
        if (includeBaseRing) {
            for (int i = 0; i < meridians; i++) {
                Point3D a = base.get(i);
                Point3D b = base.get((i + 1) % meridians);
                addBar3D(a, b);
            }
        }

        // --- Latitude rings
        for (List<Point3D> ring : lat) {
            for (int i = 0; i < meridians; i++) {
                Point3D a = ring.get(i);
                Point3D b = ring.get((i + 1) % meridians);
                addBar3D(a, b);
            }
        }

        // --- Meridians (top -> ...lat rings... -> base) as polylines of bars
        for (int i = 0; i < meridians; i++) {
            Point3D prev = top;
            // down through each latitude ring vertex at this meridian
            for (List<Point3D> ring : lat) {
                Point3D next = ring.get(i);
                addBar3D(prev, next);
                prev = next;
            }
            // finally to base vertex
            addBar3D(prev, base.get(i));
        }

        // Debug markers
        if (showMarkers) {
            markers.getChildren().add(makeDot(top, Color.YELLOW));
            for (Point3D p : base) markers.getChildren().add(makeDot(p, Color.LIMEGREEN));
            for (List<Point3D> ring : lat)
                for (Point3D p : ring) markers.getChildren().add(makeDot(p, Color.ORANGERED));
        }

        markers.setVisible(showMarkers);
    }

    // ------- Primitives -------

    /**
     * Add a 3D bar (Box) from A to B.
     * Box local X axis is aligned to (B-A) via a single-axis Rotate whose axis is (1,0,0) x dir.
     */
    private void addBar3D(Point3D a, Point3D b) {
        Point3D d = b.subtract(a);
        double len = d.magnitude();
        if (len < 1e-6) return;

        Point3D dir = d.normalize();
        // Rotate from +X (1,0,0) to dir: axis = cross((1,0,0), dir), angle = acos(dot)
        Point3D from = new Point3D(1, 0, 0);
        double dot = clamp(from.dotProduct(dir), -1.0, 1.0);
        double angleDeg = Math.toDegrees(Math.acos(dot));
        Point3D axis = from.crossProduct(dir);
        // If from || dir (axis ~ 0), pick any perpendicular axis (use +Y)
        if (axis.magnitude() < 1e-8) axis = new Point3D(0, 1, 0);

        Point3D mid = a.midpoint(b);

        Box bar = new Box(len, barSize, barSize);
        bar.setCullFace(CullFace.NONE);
        bar.setMaterial(barMat);

        bar.setRotationAxis(axis);
        bar.setRotate(angleDeg);

        bar.setTranslateX(mid.getX());
        bar.setTranslateY(mid.getY());
        bar.setTranslateZ(mid.getZ());

        frame.getChildren().add(bar);
    }

    private Sphere makeDot(Point3D p, Color c) {
        Sphere s = new Sphere(Math.max(barSize * 0.4, 2.0));
        s.setCullFace(CullFace.NONE);
        s.setMaterial(new PhongMaterial(c));
        s.setTranslateX(p.getX());
        s.setTranslateY(p.getY());
        s.setTranslateZ(p.getZ());
        return s;
    }

    private static double clamp(double v, double lo, double hi) {
        return (v < lo) ? lo : (v > hi) ? hi : v;
    }
}
