package AsteroidField.spacecraft.cockpit;

import javafx.geometry.Point3D;
import javafx.scene.paint.PhongMaterial;
import java.util.*;

/** Full icosahedron cockpit (20 faces). */
public final class IcosahedronCockpit extends BaseCockpit {
    public IcosahedronCockpit(double radius, double strutThickness, boolean addGlass) {
        this(radius, strutThickness, addGlass, null, null);
    }
    public IcosahedronCockpit(double radius, double strutThickness, boolean addGlass,
                              PhongMaterial frameMaterial, PhongMaterial glassMaterial) {
        super(radius, strutThickness, addGlass, frameMaterial, glassMaterial);
        buildFromPolyhedron(icosahedron(radius), null);
    }

    static BaseCockpit.Polyhedron icosahedron(double r) {
        double t = (1.0 + Math.sqrt(5.0)) / 2.0;
        List<Point3D> raw = Arrays.asList(
                new Point3D(-1,  t,  0), new Point3D( 1,  t,  0),
                new Point3D(-1, -t,  0), new Point3D( 1, -t,  0),
                new Point3D( 0, -1,  t), new Point3D( 0,  1,  t),
                new Point3D( 0, -1, -t), new Point3D( 0,  1, -t),
                new Point3D( t,  0, -1), new Point3D( t,  0,  1),
                new Point3D(-t,  0, -1), new Point3D(-t,  0,  1)
        );
        List<Point3D> v = new ArrayList<>(raw.size());
        for (Point3D p : raw) v.add(normalize(p).multiply(r));
        int[][] f = {
                {0,11,5},{0,5,1},{0,1,7},{0,7,10},{0,10,11},
                {1,5,9},{5,11,4},{11,10,2},{10,7,6},{7,1,8},
                {3,9,4},{3,4,2},{3,2,6},{3,6,8},{3,8,9},
                {4,9,5},{2,4,11},{6,2,10},{8,6,7},{9,8,1}
        };
        return new BaseCockpit.Polyhedron(v, f);
    }

    static Point3D normalize(Point3D p) {
        double m = Math.sqrt(p.getX()*p.getX() + p.getY()*p.getY() + p.getZ()*p.getZ());
        return new Point3D(p.getX()/m, p.getY()/m, p.getZ()/m);
    }
}
