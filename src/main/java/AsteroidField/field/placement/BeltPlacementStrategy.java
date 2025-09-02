package AsteroidField.field.placement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;

/**
 * Annular "belt" placement. Samples area-uniformly in XY annulus and Gaussian thickness in Z.
 * Optional Poisson-disk min separation in 3D to avoid overlaps.
 */
public final class BeltPlacementStrategy implements PlacementStrategy {
    private final DoubleProperty rMin = new SimpleDoubleProperty(5000);
    private final DoubleProperty rMax = new SimpleDoubleProperty(8000);
    private final DoubleProperty thickness = new SimpleDoubleProperty(400);     // 1σ vertical
    private final DoubleProperty minSeparation = new SimpleDoubleProperty(0);   // 0 disables Poisson
    private final DoubleProperty baseScale = new SimpleDoubleProperty(1.0);     // uniform pre-scale

    @Override public String getName() { return "Belt / Ring"; }

    @Override
    public Node getControls() {
        GridPane gp = new GridPane(); gp.setHgap(6); gp.setVgap(6);
        Slider sR0 = new Slider(100, 100000, rMin.get());
        Slider sR1 = new Slider(100, 100000, rMax.get());
        Slider sT  = new Slider(0, 5000, thickness.get());
        Slider sSep= new Slider(0, 500, minSeparation.get());
        Slider sSc = new Slider(0.1, 5.0, baseScale.get());

        rMin.bind(sR0.valueProperty()); rMax.bind(sR1.valueProperty());
        thickness.bind(sT.valueProperty()); minSeparation.bind(sSep.valueProperty());
        baseScale.bind(sSc.valueProperty());

        gp.addRow(0, new Label("Inner radius"), sR0);
        gp.addRow(1, new Label("Outer radius"), sR1);
        gp.addRow(2, new Label("Thickness σ"), sT);
        gp.addRow(3, new Label("Min separation"), sSep);
        gp.addRow(4, new Label("Base scale"), sSc);
        return gp;
    }

    @Override
    public List<Placement> generate(int count, Random rng) {
        double r0 = Math.min(rMin.get(), rMax.get());
        double r1 = Math.max(rMin.get(), rMax.get());
        double sigmaZ = Math.max(0, thickness.get());
        double sep = Math.max(0, minSeparation.get());

        List<Point3D> pts = (sep > 0)
                ? poissonAnnulus(count, r0, r1, sigmaZ, sep, rng)
                : jitteredAnnulus(count, r0, r1, sigmaZ, rng);

        List<Placement> out = new ArrayList<Placement>(count);
        for (Point3D p : pts) {
            // Tangential forward in XY plane; Up = global +Z
            Point3D radial = new Point3D(p.getX(), p.getY(), 0);
            Point3D f = new Point3D(-radial.getY(), radial.getX(), 0); // CCW tangent
            Point3D u = new Point3D(0, 0, 1);
            out.add(new Placement(p, f, u, baseScale.get()));
        }
        return out;
    }

    // --- sampling helpers ---

    private static List<Point3D> jitteredAnnulus(int n, double r0, double r1, double sigmaZ, Random rng) {
        List<Point3D> pts = new ArrayList<Point3D>(n);
        for (int i = 0; i < n; i++) {
            double u = rng.nextDouble(), v = rng.nextDouble();
            double r = Math.sqrt(u * (r1 * r1 - r0 * r0) + r0 * r0);
            double th = 2 * Math.PI * v;
            double x = r * Math.cos(th), y = r * Math.sin(th);
            double z = rng.nextGaussian() * sigmaZ;
            pts.add(new Point3D(x, y, z));
        }
        return pts;
    }

    /** Bridson-like Poisson sampling in an annular cylinder (good for ≤ 1000). */
    private static List<Point3D> poissonAnnulus(int target, double r0, double r1, double sigmaZ,
                                                double minDist, Random rng) {
        final double cell = minDist / Math.sqrt(3.0);
        final Map<Long, Point3D> grid = new HashMap<Long, Point3D>();
        final List<Point3D> active = new ArrayList<Point3D>();
        final List<Point3D> samples = new ArrayList<Point3D>();

        Point3D first = jitteredAnnulus(1, r0, r1, sigmaZ, rng).get(0);
        grid.put(hashCell(first, cell), first);
        active.add(first); samples.add(first);

        final int k = 30;
        while (!active.isEmpty() && samples.size() < target) {
            Point3D p = active.remove(rng.nextInt(active.size()));
            boolean found = false;
            for (int i = 0; i < k; i++) {
                double r = minDist * (1 + rng.nextDouble());
                double theta = 2 * Math.PI * rng.nextDouble();
                double phi = Math.acos(2 * rng.nextDouble() - 1);
                double dx = r * Math.sin(phi) * Math.cos(theta);
                double dy = r * Math.sin(phi) * Math.sin(theta);
                double dz = r * Math.cos(phi);
                Point3D q = p.add(dx, dy, dz);
                double rad = Math.hypot(q.getX(), q.getY());
                if (rad < r0 || rad > r1 || Math.abs(q.getZ()) > 3 * sigmaZ) continue;
                if (farEnough(grid, q, cell, minDist)) {
                    grid.put(hashCell(q, cell), q);
                    samples.add(q); active.add(q); found = true;
                    if (samples.size() >= target) break;
                }
            }
            // if not found, p leaves active set
        }
        // Rare top-up
        Random topRng = new Random(rng.nextLong());
        while (samples.size() < target) {
            Point3D q = jitteredAnnulus(1, r0, r1, sigmaZ, topRng).get(0);
            if (farEnough(grid, q, cell, minDist)) {
                grid.put(hashCell(q, cell), q); samples.add(q);
            }
        }
        return samples;
    }

    private static long hashCell(Point3D p, double cell) {
        int ix = (int) Math.floor(p.getX() / cell);
        int iy = (int) Math.floor(p.getY() / cell);
        int iz = (int) Math.floor(p.getZ() / cell);
        long key = (((long) ix & 0x1FFFFF) << 42) | (((long) iy & 0x1FFFFF) << 21) | ((long) iz & 0x1FFFFF);
        return key;
    }

    private static boolean farEnough(Map<Long, Point3D> grid, Point3D q, double cell, double minDist) {
        int ix = (int) Math.floor(q.getX() / cell);
        int iy = (int) Math.floor(q.getY() / cell);
        int iz = (int) Math.floor(q.getZ() / cell);
        for (int dx = -2; dx <= 2; dx++)
            for (int dy = -2; dy <= 2; dy++)
                for (int dz = -2; dz <= 2; dz++) {
                    long key = (((long) (ix + dx) & 0x1FFFFF) << 42)
                            | (((long) (iy + dy) & 0x1FFFFF) << 21)
                            | ((long) (iz + dz) & 0x1FFFFF);
                    Point3D n = grid.get(key);
                    if (n != null && n.distance(q) < minDist) return false;
                }
        return true;
    }
}
