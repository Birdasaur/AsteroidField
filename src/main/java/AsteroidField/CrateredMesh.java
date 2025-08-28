package AsteroidField;

import java.util.List;
import javafx.scene.shape.TriangleMesh;

public class CrateredMesh extends TriangleMesh {
    public final float[] verts;
    public final List<float[]> vertsList;
    public final int[] faces;

    public CrateredMesh(double radius, int subdivisions) {
        IcosphereMesh mesh = new IcosphereMesh(radius, subdivisions);

        // vertsList for procedural logic, verts (flat array) for JavaFX
        vertsList = mesh.getVerticesList(); 
        verts = mesh.getVertices();
        faces = mesh.getFaces();

        getPoints().setAll(verts);
        getTexCoords().addAll(0, 0);
        getFaces().setAll(faces);
        getFaceSmoothingGroups().clear();
        for (int i = 0; i < faces.length / 6; i++) getFaceSmoothingGroups().addAll(1);
    }
}