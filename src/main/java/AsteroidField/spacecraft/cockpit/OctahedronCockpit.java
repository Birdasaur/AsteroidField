package AsteroidField.spacecraft.cockpit;

import javafx.geometry.Point3D;
import javafx.scene.paint.PhongMaterial;
import java.util.*;

/** Full octahedron cockpit (8 faces). */
public final class OctahedronCockpit extends BaseCockpit {
    public OctahedronCockpit(double radius, double strutThickness, boolean addGlass) {
        this(radius, strutThickness, addGlass, null, null);
    }
    public OctahedronCockpit(double radius, double strutThickness, boolean addGlass,
                             PhongMaterial frameMaterial, PhongMaterial glassMaterial) {
        super(radius, strutThickness, addGlass, frameMaterial, glassMaterial);
        buildFromPolyhedron(octahedron(radius), null);
    }

    private static BaseCockpit.Polyhedron octahedron(double r) {
        List<Point3D> v = Arrays.asList(
                new Point3D( r, 0, 0), new Point3D(-r, 0, 0),
                new Point3D(0,  r, 0), new Point3D(0, -r, 0),
                new Point3D(0, 0,  r), new Point3D(0, 0, -r)
        );
        int[][] f = {
                {0,2,4},{2,1,4},{1,3,4},{3,0,4},
                {2,0,5},{1,2,5},{3,1,5},{0,3,5}
        };
        return new BaseCockpit.Polyhedron(v, f);
    }
}
