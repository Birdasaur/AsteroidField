package AsteroidField.spacecraft.cockpit;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

import java.util.*;

/**
 * Abstract base for cockpit frames built from Box struts and optional translucent glass panels.
 * Subclasses supply a Polyhedron and optional FaceFilter.
 */
public abstract class BaseCockpit extends Group {
    protected final double radius;
    protected final double strutThickness;
    protected final boolean addGlass;

    protected final PhongMaterial frameMaterial;
    protected final PhongMaterial glassMaterial;

    protected final Group frameGroup = new Group();
    protected MeshView glassMeshView; // null if addGlass == false

    protected BaseCockpit(double radius, double strutThickness, boolean addGlass,
                          PhongMaterial frameMaterial, PhongMaterial glassMaterial) {
        this.radius = radius;
        this.strutThickness = strutThickness;
        this.addGlass = addGlass;
        this.frameMaterial = frameMaterial != null ? frameMaterial : defaultFrameMat();
        this.glassMaterial = glassMaterial != null ? glassMaterial : defaultGlassMat();
        getChildren().add(frameGroup);
    }

    protected void buildFromPolyhedron(Polyhedron poly, FaceFilter filter) {
        List<int[]> keptFaces = new ArrayList<>();
        Set<Edge> edges = new HashSet<>();

        for (int[] f : poly.faces) {
            Point3D a = poly.vertices.get(f[0]);
            Point3D b = poly.vertices.get(f[1]);
            Point3D c = poly.vertices.get(f[2]);
            if (filter == null || filter.keep(a, b, c)) {
                keptFaces.add(new int[]{f[0], f[1], f[2]});
                edges.add(new Edge(f[0], f[1]));
                edges.add(new Edge(f[1], f[2]));
                edges.add(new Edge(f[2], f[0]));
            }
        }

        // Struts
        for (Edge e : edges) {
            Point3D a = poly.vertices.get(e.i);
            Point3D b = poly.vertices.get(e.j);
            frameGroup.getChildren().add(createBoxStrut(a, b, strutThickness, frameMaterial));
        }

        // Glass
        if (addGlass) {
            glassMeshView = createGlassMesh(poly.vertices, keptFaces);
            glassMeshView.setMaterial(glassMaterial);
            getChildren().add(glassMeshView);
        }
    }

    public Group getFrameGroup() { return frameGroup; }
    public Optional<MeshView> getGlass() { return Optional.ofNullable(glassMeshView); }
    public void setGlassVisible(boolean visible) { if (glassMeshView != null) glassMeshView.setVisible(visible); }
    public void setFrameMaterial(PhongMaterial mat) {
        for (javafx.scene.Node n : frameGroup.getChildren()) if (n instanceof Shape3D) ((Shape3D) n).setMaterial(mat);
    }
    public void setGlassMaterial(PhongMaterial mat) { if (glassMeshView != null) glassMeshView.setMaterial(mat); }

    // ----- helpers -----
    private static PhongMaterial defaultFrameMat() {
        PhongMaterial m = new PhongMaterial(Color.grayRgb(200));
        m.setSpecularColor(Color.WHITE);
        return m;
    }
    private static PhongMaterial defaultGlassMat() {
        PhongMaterial m = new PhongMaterial(Color.color(0.5, 0.8, 1.0, 0.18));
        m.setSpecularColor(Color.color(0.95, 0.98, 1.0, 0.9));
        return m;
    }

    protected static Box createBoxStrut(Point3D a, Point3D b, double thickness, PhongMaterial mat) {
        Point3D mid = a.midpoint(b);
        double length = a.distance(b);

        Box box = new Box(thickness, thickness, length);
        box.setMaterial(mat);

        Point3D dir = b.subtract(a);
        Point3D z = new Point3D(0, 0, 1);
        Point3D axis = z.crossProduct(dir);
        double angleDeg;
        if (axis.magnitude() == 0) {
            angleDeg = (dir.normalize().dotProduct(z) >= 0) ? 0 : 180;
            axis = new Point3D(1, 0, 0);
        } else {
            double cos = z.normalize().dotProduct(dir.normalize());
            cos = Math.max(-1, Math.min(1, cos));
            angleDeg = Math.toDegrees(Math.acos(cos));
        }

        box.getTransforms().addAll(new Translate(mid.getX(), mid.getY(), mid.getZ()), new Rotate(angleDeg, axis));
        return box;
    }

    protected static MeshView createGlassMesh(List<Point3D> verts, List<int[]> faces) {
        TriangleMesh tm = new TriangleMesh();
        for (Point3D v : verts) tm.getPoints().addAll((float) v.getX(), (float) v.getY(), (float) v.getZ());
        tm.getTexCoords().addAll(0, 0);
        for (int[] f : faces) tm.getFaces().addAll(f[0], 0, f[1], 0, f[2], 0);
        MeshView mv = new MeshView(tm);
        mv.setCullFace(CullFace.BACK);
        mv.setDrawMode(DrawMode.FILL);
        return mv;
    }

    protected interface FaceFilter { boolean keep(Point3D a, Point3D b, Point3D c); }

    protected static final class Edge {
        final int i, j; Edge(int a, int b) { if (a < b) { i = a; j = b; } else { i = b; j = a; } }
        @Override public boolean equals(Object o) { return (o instanceof Edge) && ((Edge) o).i == i && ((Edge) o).j == j; }
        @Override public int hashCode() { return Objects.hash(i, j); }
    }

    protected static final class Polyhedron {
        final List<Point3D> vertices; final int[][] faces;
        Polyhedron(List<Point3D> v, int[][] f) { this.vertices = v; this.faces = f; }
    }
}