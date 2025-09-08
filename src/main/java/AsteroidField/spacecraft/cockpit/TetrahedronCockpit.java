package AsteroidField.spacecraft.cockpit;

import javafx.geometry.Point3D;
import javafx.scene.paint.PhongMaterial;
import java.util.*;

/** Minimal tetrahedron frame (4 faces). */
public final class TetrahedronCockpit extends BaseCockpit {
    public TetrahedronCockpit(double radius, double strutThickness, boolean addGlass) {
        this(radius, strutThickness, addGlass, null, null);
    }
    public TetrahedronCockpit(double radius, double strutThickness, boolean addGlass,
                              PhongMaterial frameMaterial, PhongMaterial glassMaterial) {
        super(radius, strutThickness, addGlass, frameMaterial, glassMaterial);
        buildFromPolyhedron(tetra(radius), null);
    }

    private static BaseCockpit.Polyhedron tetra(double r) {
        double s = r / Math.sqrt(3);
        List<Point3D> v = Arrays.asList(
                new Point3D( s,  s,  s), new Point3D(-s, -s,  s),
                new Point3D(-s,  s, -s), new Point3D( s, -s, -s)
        );
        int[][] f = { {0,1,2}, {0,3,1}, {0,2,3}, {1,3,2} };
        return new BaseCockpit.Polyhedron(v, f);
    }
}