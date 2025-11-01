package AsteroidField.asteroids.geometry;

import AsteroidField.asteroids.AsteroidNode;
import java.util.List;
import javafx.geometry.Point3D;

/**
 * Portal pass-through for spacecraft and tether.
 * For spacecraft (sphere of radius r): both endpoints must be within the same mouth cone,
 * and radial distance between [innerRadius + r, outerRadius - r].
 */
public final class PortalUtil {

    private PortalUtil() {}

    public static boolean segmentInsideAnyPortal(Point3D aWorld,
                                                 Point3D bWorld,
                                                 AsteroidNode asteroid,
                                                 double sphereRadius) {
        Point3D aLocal = asteroid.sceneToLocal(aWorld);
        Point3D bLocal = asteroid.sceneToLocal(bWorld);
        List<Portal> portals = asteroid.getPortals();

        for (Portal p : portals) {
            if (insideConeAndThickness(aLocal, p, sphereRadius)
                    && insideConeAndThickness(bLocal, p, sphereRadius)) {
                return true;
            }
        }
        return false;
    }

    private static boolean insideConeAndThickness(Point3D v, Portal p, double r) {
        double dist = Math.sqrt(v.getX()*v.getX() + v.getY()*v.getY() + v.getZ()*v.getZ());
        if (dist < p.getInnerRadius() + r - 1e-6) return false;
        if (dist > p.getOuterRadius() - r + 1e-6) return false;

        double inv = dist > 0.0 ? 1.0 / dist : 0.0;
        double cosTheta = (v.getX()*inv)*p.getAxisUnit().getX()
                + (v.getY()*inv)*p.getAxisUnit().getY()
                + (v.getZ()*inv)*p.getAxisUnit().getZ();
        return cosTheta >= p.getCosHalfAngle() - 1e-7;
    }
}
