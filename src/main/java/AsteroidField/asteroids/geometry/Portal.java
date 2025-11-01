package AsteroidField.asteroids.geometry;

import javafx.geometry.Point3D;

/** Mouth/portal cone in asteroid-local space. Axis points outward. */
public final class Portal {
    private final Point3D axisUnit;
    private final double halfAngleRad;
    private final double cosHalfAngle;
    private final double innerRadius;
    private final double outerRadius;

    public Portal(Point3D axis, double halfAngleRad, double innerRadius, double outerRadius) {
        if (axis == null) throw new IllegalArgumentException("axis null");
        double m = Math.sqrt(axis.getX()*axis.getX() + axis.getY()*axis.getY() + axis.getZ()*axis.getZ());
        if (m == 0.0) throw new IllegalArgumentException("axis zero length");
        this.axisUnit = new Point3D(axis.getX()/m, axis.getY()/m, axis.getZ()/m);
        if (halfAngleRad <= 0.0 || halfAngleRad >= Math.PI * 0.5) {
            throw new IllegalArgumentException("halfAngle must be in (0, pi/2)");
        }
        if (outerRadius <= innerRadius) throw new IllegalArgumentException("outerRadius <= innerRadius");
        this.halfAngleRad = halfAngleRad;
        this.cosHalfAngle = Math.cos(halfAngleRad);
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
    }

    public Point3D getAxisUnit() { return axisUnit; }
    public double getHalfAngleRad() { return halfAngleRad; }
    public double getCosHalfAngle() { return cosHalfAngle; }
    public double getInnerRadius() { return innerRadius; }
    public double getOuterRadius() { return outerRadius; }
}
