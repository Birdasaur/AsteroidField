package AsteroidField.asteroids.geometry;

import AsteroidField.asteroids.parameters.AsteroidParameters;
import javafx.scene.shape.TriangleMesh;
import java.util.*;

// Utility for creating deformed icosphere meshes
public class IcosphereDeformer {

    public static TriangleMesh generate(AsteroidParameters params) {
        // 1. Build base icosphere (vertices, faces)
        IcosphereMesh mesh = new IcosphereMesh(params.getRadius(), params.getSubdivisions());

        // 2. Deform vertices using seeded random
        Random rng = new Random(params.getSeed());
        float[] verts = mesh.getVertsArray();
        double def = params.getDeformation();

        for (int i = 0; i < verts.length; i += 3) {
            double x = verts[i], y = verts[i+1], z = verts[i+2];
            double len = Math.sqrt(x*x + y*y + z*z);

            // Outward/inward displacement
            double bump = 1.0 + def * (rng.nextDouble() - 0.5) * 2.0;
            double newLen = params.getRadius() * bump;
            verts[i]   = (float)(x / len * newLen);
            verts[i+1] = (float)(y / len * newLen);
            verts[i+2] = (float)(z / len * newLen);
        }

        // 3. Build TriangleMesh object
        TriangleMesh triMesh = new TriangleMesh();
        triMesh.getPoints().setAll(verts);
        triMesh.getTexCoords().addAll(0,0); // Flat UV for now

        // Faces: int[] -> float[] for JavaFX
        triMesh.getFaces().setAll(mesh.getFaces());

        // After building mesh and setting points/faces:
        int numFaces = triMesh.getFaces().size() / 6;
        for (int i = 0; i < numFaces; i++) {
            triMesh.getFaceSmoothingGroups().addAll(1); // All faces share group 1: fully smooth
        }
        return triMesh;
    }
}