package AsteroidField.asteroids.geometry;

import AsteroidField.asteroids.parameters.KryptoniteClusterParameters;
import AsteroidField.asteroids.parameters.CrystallineAsteroidParameters;
import java.util.*;

public class KryptoniteClusterMesh extends CrystallineMesh {

public KryptoniteClusterMesh(KryptoniteClusterParameters params) {
    super(params.getRadius(), params.getSubdivisions(), params);
    System.out.println("KryptoniteClusterMesh CONSTRUCTOR CALLED!");
    this.deform(params);
}

    @Override
    public void deform(CrystallineAsteroidParameters baseParams) {
            // Always set up UVs and tex indices first!
        setupBaseTexCoords(baseParams);

        KryptoniteClusterParameters params = (KryptoniteClusterParameters) baseParams;
        System.out.println("KryptoniteClusterMesh.deform() called.");

        deformBaseIcosphere(params);
        Random rng = new Random(params.getSeed());
        double baseRadius = params.getRadius();

        // Sample disk clusters
        List<double[]> clusterDirs = KryptoniteHelper.generateRandomClusterCenters(params.getNumClusters(), rng);

        List<float[]> allBases = new ArrayList<>();
        for (double[] clusterDir : clusterDirs) {
            List<float[]> clusterBases = KryptoniteHelper.sampleClusterOnIcosphere(
                vertsList,
                clusterDir,
                Math.toRadians(params.getDiskAngleDegrees()),
                params.getCrystalsPerCluster(),
                rng
            );
            allBases.addAll(clusterBases);
        }
        int crystalCount = allBases.size();
        System.out.println("KryptoniteClusterMesh: Sampled " + crystalCount + " cluster bases.");
        for (int i = 0; i < allBases.size(); i++) {
            float[] v = allBases.get(i);
            System.out.printf("  Base %d: (%.2f, %.2f, %.2f)%n", i, v[0], v[1], v[2]);
        }

        List<float[]> allVerts = new ArrayList<>(vertsList);
        List<FaceRef> allFaceRefs = new ArrayList<>();

        // Add the base mesh asteroid faces (as in parent)
        for (int[] face : facesList) {
            int vi0 = face[0], vi1 = face[1], vi2 = face[2];
            int ti0 = baseVertToTexIdx.get(vi0), ti1 = baseVertToTexIdx.get(vi1), ti2 = baseVertToTexIdx.get(vi2);
            allFaceRefs.add(new FaceRef(vi0, ti0, vi1, ti1, vi2, ti2));
        }

        int paletteRows = 12; // For UV palette
        double paletteU = 0.0;
        int crystalCounter = 0;
        int facesBefore = allFaceRefs.size();
        int vertsBefore = allVerts.size();

        for (float[] base : allBases) {
            crystalCounter++;
            int paletteIdx = (crystalCounter-1) % paletteRows;
            double paletteV = (paletteIdx + 0.5) / paletteRows;
            float[] normal = getNormalForBase(base);
            System.out.printf("Crystal %d: base (%.2f, %.2f, %.2f), normal (%.2f, %.2f, %.2f), paletteV=%.2f%n",
                    crystalCounter, base[0], base[1], base[2], normal[0], normal[1], normal[2], paletteV);

            // Defensive: try/catch to log any geometry errors
            try {
                createCrystalWithUVs(
                    allVerts, allFaceRefs, base, normal, params, baseRadius, rng, 0,
                    paletteU, paletteV, paletteRows, baseVertToTexIdx
                );
            } catch (Exception e) {
                System.out.println("Error building crystal " + crystalCounter + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        int vertsAfter = allVerts.size();
        int facesAfter = allFaceRefs.size();

        System.out.printf("Mesh verts: %d -> %d (added %d)\n", vertsBefore, vertsAfter, vertsAfter - vertsBefore);
        System.out.printf("Mesh faces: %d -> %d (added %d)\n", facesBefore, facesAfter, facesAfter - facesBefore);

        // Finalize mesh data as in CrystallineMesh
        float[] newVerts = new float[allVerts.size() * 3];
        for (int i = 0; i < allVerts.size(); i++) {
            float[] v = allVerts.get(i);
            newVerts[i * 3] = v[0];
            newVerts[i * 3 + 1] = v[1];
            newVerts[i * 3 + 2] = v[2];
        }
        float[] newTexCoords = new float[texCoordList.size() * 2];
        for (int i = 0; i < texCoordList.size(); i++) {
            double[] uv = texCoordList.get(i);
            newTexCoords[i * 2] = (float)uv[0];
            newTexCoords[i * 2 + 1] = (float)uv[1];
        }
        int[] newFaces = new int[allFaceRefs.size() * 6];
        for (int i = 0; i < allFaceRefs.size(); i++) {
            FaceRef f = allFaceRefs.get(i);
            newFaces[i * 6 + 0] = f.vi0;
            newFaces[i * 6 + 1] = f.ti0;
            newFaces[i * 6 + 2] = f.vi1;
            newFaces[i * 6 + 3] = f.ti1;
            newFaces[i * 6 + 4] = f.vi2;
            newFaces[i * 6 + 5] = f.ti2;
        }

        getPoints().setAll(newVerts);
        getTexCoords().setAll(newTexCoords);
        getFaces().setAll(newFaces);
        getFaceSmoothingGroups().clear();
        for (int i = 0; i < newFaces.length / 6; i++) getFaceSmoothingGroups().addAll(1);

        System.out.println("KryptoniteClusterMesh: Mesh construction complete.");
    }

    /** Returns a normalized normal vector for a given base position. */
    private float[] getNormalForBase(float[] base) {
        double len = Math.sqrt(base[0]*base[0] + base[1]*base[1] + base[2]*base[2]);
        return new float[] { (float)(base[0]/len), (float)(base[1]/len), (float)(base[2]/len) };
    }
}
