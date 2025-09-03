package AsteroidField.tether;

import javafx.geometry.Bounds;
import javafx.geometry.Point3D;
import javafx.geometry.BoundingBox;

import java.util.OptionalDouble;

public final class Intersections {
    private Intersections(){}

    public static Bounds inflate(Bounds b, double by) {
        return new BoundingBox(
                b.getMinX() - by, b.getMinY() - by, b.getMinZ() - by,
                b.getWidth() + 2*by, b.getHeight() + 2*by, b.getDepth() + 2*by
        );
    }

    /**
     * Returns tEnter in [0,1] if the segment (p0 -> p1) intersects AABB bounds.
     * Implements the "slabs" method.
     */
    public static OptionalDouble segmentAabbFirstHit(Point3D p0, Point3D p1, Bounds aabb) {
        Point3D d = p1.subtract(p0);
        double tmin = 0.0;
        double tmax = 1.0;

        double[] min = {aabb.getMinX(), aabb.getMinY(), aabb.getMinZ()};
        double[] max = {aabb.getMaxX(), aabb.getMaxY(), aabb.getMaxZ()};
        double[] o   = {p0.getX(), p0.getY(), p0.getZ()};
        double[] dd  = {d.getX(), d.getY(), d.getZ()};

        for (int axis = 0; axis < 3; axis++) {
            if (Math.abs(dd[axis]) < 1e-9) {
                if (o[axis] < min[axis] || o[axis] > max[axis]) {
                    return OptionalDouble.empty();
                }
            } else {
                double inv = 1.0 / dd[axis];
                double t1 = (min[axis] - o[axis]) * inv;
                double t2 = (max[axis] - o[axis]) * inv;
                if (t1 > t2) { double tmp = t1; t1 = t2; t2 = tmp; }
                tmin = Math.max(tmin, t1);
                tmax = Math.min(tmax, t2);
                if (tmin > tmax) return OptionalDouble.empty();
            }
        }
        return (tmin >= 0.0 && tmin <= 1.0) ? OptionalDouble.of(tmin) : OptionalDouble.empty();
    }
}