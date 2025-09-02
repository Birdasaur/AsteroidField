package AsteroidField.asteroids.geometry;

import AsteroidField.asteroids.parameters.CubicAsteroidParameters;
import java.util.List;

import javafx.scene.shape.TriangleMesh;

public class CubicMesh extends TriangleMesh {
    protected final float[] verts;
    protected final List<float[]> vertsList;
    protected final int[] faces;
    protected final List<int[]> facesList;
    public final double size;
    public final int subdivisions;

    public CubicMesh(double size, int subdivisions, CubicAsteroidParameters params) {
        super();
        this.size = size;
        this.subdivisions = subdivisions;
        // Build a subdivided cube
        CubeMeshData mesh = CubeMeshHelper.generateCube(size, subdivisions);
        // Deform
        CubeMeshHelper.deformVertices(mesh.vertsList, mesh.vertsArray, params.getDeformation(), params.getSeed());
        // Build UVs (cube projection)
        CubeUVMapping uvMap = CubeMeshHelper.buildCubeUVs(mesh, size);

        //copy mesh data into actual TriangleMesh data structures
        vertsList = mesh.vertsList;
        verts = mesh.vertsArray;
        facesList = mesh.facesList;
        faces = mesh.facesArray;

        // Write to TriangleMesh
        getPoints().setAll(verts);
        getTexCoords().setAll(uvMap.uvArray);
        getFaces().setAll(uvMap.faceArray);
        getFaceSmoothingGroups().clear();
        for (int i = 0; i < uvMap.faceArray.length / 6; i++) 
            getFaceSmoothingGroups().addAll(1);
    }
}