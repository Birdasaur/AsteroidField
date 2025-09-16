package AsteroidField.spacecraft.cockpit;

import javafx.geometry.Point3D;
import javafx.scene.paint.PhongMaterial;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Icosahedron-derived wrap-around canopy (~3/4 sphere).
 * Keeps faces whose centroid.z >= -backExtent (in world units).
 * backExtent is typically a fraction of radius (e.g., 0.5 * r).
 */
public final class IcosaWrapCockpit extends BaseCockpit {

    private final double backExtent; // how far behind origin we allow panels (world units)

    public IcosaWrapCockpit(double radius, double strutThickness, boolean addGlass) {
        this(radius, strutThickness, addGlass, null, null, 0.5); // default: 50% of radius behind
    }

    public IcosaWrapCockpit(double radius, double strutThickness, boolean addGlass,
                            PhongMaterial frameMaterial, PhongMaterial glassMaterial,
                            double backExtentFrac) {
        super(radius, strutThickness, addGlass, frameMaterial, glassMaterial);
        this.backExtent = Math.max(0, backExtentFrac) * radius;
        Polyhedron ico = IcosahedronCockpit.icosahedron(radius);
        buildFromPolyhedron(ico, (a,b,c) -> {
            Point3D centroid = new Point3D(
                    (a.getX()+b.getX()+c.getX())/3.0,
                    (a.getY()+b.getY()+c.getY())/3.0,
                    (a.getZ()+b.getZ()+c.getZ())/3.0
            );
            return centroid.getZ() >= -backExtent; // extend coverage behind player
        });
    }
}