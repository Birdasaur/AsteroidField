package AsteroidField;

import javafx.scene.shape.TriangleMesh;
import java.util.Random;

public class CubicAsteroidMeshProvider implements AsteroidMeshProvider {
    @Override
    public TriangleMesh generateMesh(AsteroidParameters params) {
        float size = (float) params.getRadius();
        float def = (float) params.getDeformation();
        Random rng = new Random(params.getSeed());

        // 8 cube vertices
        float[][] verts = {
            { -size, -size, -size }, {  size, -size, -size },
            {  size,  size, -size }, { -size,  size, -size },
            { -size, -size,  size }, {  size, -size,  size },
            {  size,  size,  size }, { -size,  size,  size }
        };

        // Perturb vertices
        for (float[] v : verts) {
            v[0] += def * size * (rng.nextFloat() - 0.5f) * 2;
            v[1] += def * size * (rng.nextFloat() - 0.5f) * 2;
            v[2] += def * size * (rng.nextFloat() - 0.5f) * 2;
        }

        // Cube faces (each face = 2 triangles)
        int[][] faces = {
            {0,1,2}, {0,2,3}, // back
            {4,7,5}, {5,7,6}, // front
            {0,4,1}, {1,4,5}, // bottom
            {2,6,7}, {2,7,3}, // top
            {0,3,4}, {4,3,7}, // left
            {1,5,6}, {1,6,2}  // right
        };

        // Build mesh arrays
        float[] meshVerts = new float[8*3];
        for (int i=0; i<8; i++) {
            meshVerts[i*3]   = verts[i][0];
            meshVerts[i*3+1] = verts[i][1];
            meshVerts[i*3+2] = verts[i][2];
        }

        int[] meshFaces = new int[faces.length*6];
        for (int i=0; i<faces.length; i++) {
            int[] f = faces[i];
            meshFaces[i*6]   = f[0]; meshFaces[i*6+1] = 0;
            meshFaces[i*6+2] = f[1]; meshFaces[i*6+3] = 0;
            meshFaces[i*6+4] = f[2]; meshFaces[i*6+5] = 0;
        }

        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().setAll(meshVerts);
        mesh.getTexCoords().addAll(0,0);
        mesh.getFaces().setAll(meshFaces);

        // Smoothing group: make each face "flat" for hard cube look,
        // or set all to same for smooth (try both for fun!)
        mesh.getFaceSmoothingGroups().clear();
        for (int i = 0; i < faces.length; i++) {
            mesh.getFaceSmoothingGroups().addAll(0); // Flat shaded (0)
            // Or try 1 for all-smooth
        }

        return mesh;
    }
}