package AsteroidField.spacecraft.cockpit;

import javafx.geometry.Point3D;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Sphere;

import java.util.ArrayList;
import java.util.List;

public class CockpitDome3D extends Group {

    private final Group frame   = new Group();
    private final Group markers = new Group();

    // --- sizing ---
    private double radius  = 200.0; // dome radius
    private double barSize = 8.0;   // base bar thickness (Y & Z of each box)

    // --- structure ---
    private int meridians    = 8;   // vertical ribs (8 = octagon)
    private int latRingsUp   = 2;   // latitude rings between top and equator (upper half)
    private int latRingsDown = 2;   // latitude rings between equator and bottom (lower half)
    private boolean full360  = true; // include lower hemisphere
    private boolean includeBaseRing = true; // ring at y=0 around equator

    // --- front emphasis (windshield feel near +Z) ---
    // Thickness multiplier = 1 + frontBias * exp(-0.5 * ang^2 / sigma^2), ang = azimuth difference from +Z in degrees
    private double frontBiasMul = 2.0; // peak multiplier at +Z (>=1)
    private double frontBiasSigmaDeg = 30.0; // width of emphasis lobe

    // --- A-pillars (optional) ---
    private boolean includeAPillars = true;
    private double  aPillarAzimuthDeg = 25.0; // ±this angle around +Z
    private double  aPillarTopThetaDeg = 20.0; // down from top (0°=top, 90°=equator)
    private double  aPillarBottomThetaDeg = 65.0;
    private double  aPillarMul = 2.2; // thickness multiplier for A-pillars

    // --- debug ---
    private boolean showMarkers = false;

    // --- material (per-node opacity; don't set Group opacity) ---
    private final PhongMaterial barMat = new PhongMaterial(Color.CYAN);

    public CockpitDome3D() {
        setDepthTest(DepthTest.ENABLE);
        barMat.setSpecularColor(Color.WHITE);
        getChildren().addAll(frame, markers);
        build();
    }

    // ================= public API =================
    // --- Compat surface to resemble BaseCockpit ---

    /** Treat the bar frame as the 'frame group' (for shared helpers). */
    public Group getFrameGroup() {
        return frame;
    }

    /** Dome has no glass mesh; return empty for API parity. */
    public java.util.Optional<javafx.scene.shape.MeshView> getGlass() {
        return java.util.Optional.empty();
    }

    /**
     * Map a frame material onto this dome’s bar material:
     * - Uses material's diffuse RGB (keeps current opacity).
     * - Copies specular color if provided.
     */
    public void setFrameMaterial(PhongMaterial mat) {
        if (mat == null) return;

        // Preserve current opacity, replace RGB from mat's diffuse
        Color target = mat.getDiffuseColor() != null ? mat.getDiffuseColor() : barMat.getDiffuseColor();
        Color current = barMat.getDiffuseColor();
        barMat.setDiffuseColor(new Color(
                target.getRed(), target.getGreen(), target.getBlue(),
                current.getOpacity()
        ));

        // Optional: mirror specular
        Color spec = mat.getSpecularColor() != null ? mat.getSpecularColor() : Color.WHITE;
        barMat.setSpecularColor(spec);
    }

    /** No-op: this implementation has no glass mesh, method here for parity. */
    public void setGlassMaterial(PhongMaterial mat) {
        // intentionally empty
    }

    /** No-op: this implementation has no glass mesh, method here for parity. */
    public void setGlassVisible(boolean visible) {
        // intentionally empty
    }

    public void rebuild(double newRadius, double newBarSize) {
        this.radius  = Math.max(1e-6, newRadius);
        this.barSize = Math.max(1e-6, newBarSize);
        build();
    }

    public void setMeridians(int m)            { this.meridians = Math.max(3, m); build(); }
    public void setLatitudeRings(int up)       { this.latRingsUp = Math.max(0, Math.min(12, up)); build(); }
    public void setLatitudeRingsLower(int dn)  { this.latRingsDown = Math.max(0, Math.min(12, dn)); build(); }
    public void setFull360(boolean on)         { this.full360 = on; build(); }
    public void setIncludeBaseRing(boolean on) { this.includeBaseRing = on; build(); }

    public void setShowMarkers(boolean on) { this.showMarkers = on; markers.setVisible(on); if (on && markers.getChildren().isEmpty()) build(); }

    public void setBarColor(Color c) {
        Color d = barMat.getDiffuseColor();
        barMat.setDiffuseColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), d.getOpacity()));
    }
    public void setBarOpacity(double a) {
        a = Math.max(0.0, Math.min(1.0, a));
        Color d = barMat.getDiffuseColor();
        barMat.setDiffuseColor(new Color(d.getRed(), d.getGreen(), d.getBlue(), a));
    }

    // Front emphasis
    public void setFrontBias(double peakMultiplier, double sigmaDeg) {
        this.frontBiasMul = Math.max(1.0, peakMultiplier);
        this.frontBiasSigmaDeg = Math.max(1.0, sigmaDeg);
        build();
    }

    // A-pillars
    public void setIncludeAPillars(boolean on) { this.includeAPillars = on; build(); }
    public void setAPillarParams(double azimuthDeg, double topThetaDeg, double bottomThetaDeg, double thicknessMul) {
        this.aPillarAzimuthDeg = azimuthDeg;
        this.aPillarTopThetaDeg = topThetaDeg;
        this.aPillarBottomThetaDeg = bottomThetaDeg;
        this.aPillarMul = Math.max(1.0, thicknessMul);
        build();
    }

    // ================= build =================

    private void build() {
        frame.getChildren().clear();
        markers.getChildren().clear();

        // Top/bottom poles (Y up is negative)
        Point3D top    = new Point3D(0, -radius, 0);
        Point3D bottom = new Point3D(0,  radius, 0);

        // Equator ring (y=0)
        List<Point3D> equator = ringPoints(radius, 0.0, meridians);

        // Upper latitude rings (exclude top/equator)
        List<List<Point3D>> upRings = new ArrayList<>();
        for (int r = 1; r <= latRingsUp; r++) {
            double theta = (r * (Math.PI / 2.0)) / (latRingsUp + 1); // (0, pi/2)
            upRings.add(ringAtTheta(theta));
        }

        // Lower latitude rings (exclude equator/bottom)
        List<List<Point3D>> dnRings = new ArrayList<>();
        if (full360 && latRingsDown > 0) {
            for (int r = 1; r <= latRingsDown; r++) {
                double theta = Math.PI/2.0 + (r * (Math.PI / 2.0)) / (latRingsDown + 1); // (pi/2, pi)
                dnRings.add(ringAtTheta(theta));
            }
        }

        // Base ring(s)
        if (includeBaseRing) {
            addRing(equator);
        }
        for (List<Point3D> ring : upRings) addRing(ring);
        if (full360) for (List<Point3D> ring : dnRings) addRing(ring);

        // Meridians: top -> up rings -> equator -> down rings -> bottom
        for (int i = 0; i < meridians; i++) {
            Point3D prev = top;
            for (List<Point3D> ring : upRings) { addBar3D(prev, ring.get(i)); prev = ring.get(i); }
            addBar3D(prev, equator.get(i)); prev = equator.get(i);
            if (full360) {
                for (List<Point3D> ring : dnRings) { addBar3D(prev, ring.get(i)); prev = ring.get(i); }
                addBar3D(prev, bottom);
            }
        }

        // A-pillars (two heavier ribs around +Z)
        if (includeAPillars) {
            addAPillar(+aPillarAzimuthDeg);
            addAPillar(-aPillarAzimuthDeg);
        }

        // Debug markers
        if (showMarkers) {
            markers.getChildren().add(makeDot(top, Color.YELLOW));
            markers.getChildren().add(makeDot(bottom, Color.ORANGE));
            for (Point3D p : equator) markers.getChildren().add(makeDot(p, Color.LIMEGREEN));
            for (List<Point3D> ring : upRings) for (Point3D p : ring) markers.getChildren().add(makeDot(p, Color.ORANGERED));
            for (List<Point3D> ring : dnRings) for (Point3D p : ring) markers.getChildren().add(makeDot(p, Color.STEELBLUE));
        }
        markers.setVisible(showMarkers);
    }

    // ================ helpers ================

    private List<Point3D> ringAtTheta(double theta) {
        double y = -radius * Math.cos(theta);   // -Y is up
        double r =  radius * Math.sin(theta);
        return ringPoints(r, y, meridians);
    }

    private List<Point3D> ringPoints(double ringR, double y, int count) {
        List<Point3D> pts = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double a = (i * 2.0 * Math.PI) / count; // azimuth about +Y
            pts.add(new Point3D(Math.cos(a) * ringR, y, Math.sin(a) * ringR));
        }
        return pts;
    }

    private void addRing(List<Point3D> ring) {
        for (int i = 0; i < ring.size(); i++) {
            addBar3D(ring.get(i), ring.get((i + 1) % ring.size()));
        }
    }

    private void addAPillar(double azDeg) {
        // Spherical points at given azimuth (around +Y), between two thetas
        double phi = Math.toRadians(azDeg); // 0° = +Z
        Point3D topP = sph(radius, Math.toRadians(aPillarTopThetaDeg), phi);
        Point3D botP = sph(radius, Math.toRadians(aPillarBottomThetaDeg), phi);
        addBar3D(topP, botP, barSize * aPillarMul);
    }

    private Point3D sph(double R, double theta, double phi) {
        // theta: 0=top pole, pi/2=equator, pi=bottom; phi: 0 along +Z, +X at +90°
        double y = -R * Math.cos(theta);
        double r =  R * Math.sin(theta);
        return new Point3D(r * Math.sin(phi), y, r * Math.cos(phi));
    }

    /**
     * Add a 3D bar from a to b. Thickness uses base barSize times a front-bias multiplier.
     */
    private void addBar3D(Point3D a, Point3D b) { addBar3D(a, b, thicknessFor(midpoint(a, b))); }

    private void addBar3D(Point3D a, Point3D b, double thickness) {
        Point3D d = b.subtract(a);
        double len = d.magnitude();
        if (len < 1e-6) return;

        Point3D dir = d.normalize();
        // rotate from +X to dir
        Point3D from = new Point3D(1, 0, 0);
        double dot = clamp(from.dotProduct(dir), -1.0, 1.0);
        double angleDeg = Math.toDegrees(Math.acos(dot));
        Point3D axis = from.crossProduct(dir);
        if (axis.magnitude() < 1e-8) axis = new Point3D(0, 1, 0);

        Point3D mid = midpoint(a, b);

        Box bar = new Box(len, thickness, thickness);
        bar.setCullFace(CullFace.NONE);
        bar.setMaterial(barMat);

        bar.setRotationAxis(axis);
        bar.setRotate(angleDeg);
        bar.setTranslateX(mid.getX());
        bar.setTranslateY(mid.getY());
        bar.setTranslateZ(mid.getZ());

        frame.getChildren().add(bar);
    }

    private double thicknessFor(Point3D p) {
        // azimuth angle around Y, 0° pointing +Z
        double ang = Math.toDegrees(Math.atan2(p.getX(), p.getZ())); // note atan2(x,z) => 0 at +Z
        double d = wrapDegrees(ang); // distance from +Z (0°), signed
        double w = Math.exp(-0.5 * (d * d) / (frontBiasSigmaDeg * frontBiasSigmaDeg));
        double mul = 1.0 + (frontBiasMul - 1.0) * w;
        return barSize * mul;
    }

    private static double wrapDegrees(double a) {
        // wrap to [-180,180], then absolute value distance from 0
        double w = ((a + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
        return Math.abs(w);
    }

    private static Point3D midpoint(Point3D a, Point3D b) {
        return new Point3D((a.getX()+b.getX())*0.5, (a.getY()+b.getY())*0.5, (a.getZ()+b.getZ())*0.5);
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

    private static double clamp(double v, double lo, double hi) { return (v < lo) ? lo : (v > hi) ? hi : v; }
}
