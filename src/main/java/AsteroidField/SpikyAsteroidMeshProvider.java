package AsteroidField;

import javafx.scene.shape.TriangleMesh;
import java.util.Random;

public class SpikyAsteroidMeshProvider implements AsteroidMeshProvider {
    @Override
    public TriangleMesh generateMesh(AsteroidParameters params) {
        // Start with a simple icosphere, but exaggerate deformation for spikes
        IcosphereMesh base = new IcosphereMesh(params.getRadius(), params.getSubdivisions());
        float[] verts = base.getVertices();
        Random rng = new Random(params.getSeed());

        // Spike every Nth vertex
        int spikeStep = Math.max(1, verts.length / (3 * 20)); // About 20 spikes
        for (int i = 0; i < verts.length; i += 3) {
            double x = verts[i], y = verts[i+1], z = verts[i+2];
            double len = Math.sqrt(x*x + y*y + z*z);
            double bump = 1.0 + params.getDeformation() * (rng.nextDouble() - 0.5) * 2.0;

            // Every Nth: add a spike
            if ((i/3) % spikeStep == 0) bump += 1.5 * params.getDeformation() * (0.8 + rng.nextDouble());

            double newLen = params.getRadius() * bump;
            verts[i]   = (float)(x / len * newLen);
            verts[i+1] = (float)(y / len * newLen);
            verts[i+2] = (float)(z / len * newLen);
        }

        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().setAll(verts);
        mesh.getTexCoords().addAll(0,0);
        mesh.getFaces().setAll(base.getFaces());
        mesh.getFaceSmoothingGroups().clear();
        int numFaces = base.getFaces().length / 6;
        for (int i = 0; i < numFaces; i++) mesh.getFaceSmoothingGroups().addAll(1);

        return mesh;
    }
}
