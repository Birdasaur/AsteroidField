package AsteroidField.util;

import javafx.geometry.Point2D;
import javafx.geometry.Point3D;

public final class Math3D {
    private Math3D() {}

    public static final Point3D WORLD_UP = new Point3D(0,1,0);

    public static double clamp01(double x) { return Math.max(0, Math.min(1, x)); }

    public static Point3D nullToZero(Point3D p) { return (p == null ? Point3D.ZERO : p); }

    public static Point2D clampToRadius(Point2D p, double r) {
        double m = p.magnitude();
        if (m <= r) return p;
        return p.multiply(r / (m + 1e-9));
    }

    public static boolean isZero(Point2D p) { return Math.abs(p.getX()) < 1e-6 && Math.abs(p.getY()) < 1e-6; }

    public static Point3D lerp(Point3D a, Point3D b, double t) {
        if (a == null) a = Point3D.ZERO;
        if (b == null) b = Point3D.ZERO;
        t = clamp01(t);
        return new Point3D(
            a.getX() + (b.getX() - a.getX()) * (1 - t),
            a.getY() + (b.getY() - a.getY()) * (1 - t),
            a.getZ() + (b.getZ() - a.getZ()) * (1 - t)
        );
    }    
    public static Basis basisFromForward(Point3D forward) {
        if (forward == null || forward.magnitude() < 1e-6) forward = new Point3D(0,0,-1);
        Point3D f = forward.normalize();
        Point3D right = f.crossProduct(WORLD_UP);
        if (right.magnitude() < 1e-6) right = f.crossProduct(new Point3D(0,0,1));
        right = right.normalize();
        Point3D up = right.crossProduct(f).normalize();
        return new Basis(right, up, f);
    }
    public static Point2D projectToRadar(Point3D v, Basis b) {
        // Project onto camera right/up plane
        double x = v.dotProduct(b.right);
        double y = v.dotProduct(b.up);
        return new Point2D(x, y);
    }
    public static class Basis {
        public final Point3D right, up, forward;
        public Basis(Point3D r, Point3D u, Point3D f){ right=r; up=u; forward=f; }
    }
}

