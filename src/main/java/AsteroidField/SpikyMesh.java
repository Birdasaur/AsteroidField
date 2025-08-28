package AsteroidField;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javafx.scene.shape.TriangleMesh;


public class SpikyMesh extends TriangleMesh {
    private final float[] verts;
    private final List<float[]> vertsList;
    private final int[] faces;
    public final double radius;
    public final int subdivisions;

    public SpikyMesh(double radius, int subdivisions, SpikyAsteroidParameters params) {
        super();
        this.radius = radius;
        this.subdivisions = subdivisions;

        // Base geometry
        IcosphereMesh ico = new IcosphereMesh(radius, subdivisions);
        this.verts = ico.getVertices();
        this.vertsList = new ArrayList<>();
        float[] arr = ico.getVertices();
        for (int i = 0; i < arr.length; i += 3) {
            vertsList.add(new float[] { arr[i], arr[i+1], arr[i+2] });
        }
        this.faces = ico.getFaces();

        // Deform (spikes, etc)
        deform(params);

        getPoints().setAll(verts);
        getTexCoords().addAll(0, 0);
        getFaces().setAll(faces);
        getFaceSmoothingGroups().clear();
        for (int i = 0; i < faces.length / 6; i++) getFaceSmoothingGroups().addAll(1);
    }

    /** Deform the mesh using new spiky parameters (can be called any time). */
    public void deform(SpikyAsteroidParameters params) {
        Random rng = new Random(params.getSeed());
        int spikeCount = Math.max(1, params.getSpikeCount());
        int numVerts = verts.length / 3;

        // Evenly distribute spike directions
        List<double[]> spikeDirs = new ArrayList<>();
        for (int k = 0; k < spikeCount; k++) {
            double y = 1 - 2.0 * k / (spikeCount - 1.0);
            double r = Math.sqrt(1 - y * y);
            double phi = Math.PI * (3 - Math.sqrt(5)) * k;
            double x = Math.cos(phi) * r;
            double z = Math.sin(phi) * r;
            spikeDirs.add(new double[] { x, y, z });
        }

        // Closest vertex to each direction
        Set<Integer> spikeIndices = new HashSet<>();
        for (double[] dir : spikeDirs) {
            int bestIdx = -1;
            double bestDot = -2;
            for (int i = 0; i < verts.length; i += 3) {
                double vx = verts[i], vy = verts[i+1], vz = verts[i+2];
                double len = Math.sqrt(vx*vx + vy*vy + vz*vz);
                vx /= len; vy /= len; vz /= len;
                double dot = vx * dir[0] + vy * dir[1] + vz * dir[2];
                if (dot > bestDot) {
                    bestDot = dot;
                    bestIdx = i / 3;
                }
            }
            spikeIndices.add(bestIdx);
        }

        // Optionally widen spikes (affect more verts around each main spike)
        int widthWindow = Math.max(0, (int)(params.getSpikeWidth() * numVerts / 2));
        Set<Integer> allSpikeVerts = new HashSet<>();
        for (int idx : spikeIndices) {
            for (int j = -widthWindow; j <= widthWindow; j++) {
                int vIdx = (idx + j + numVerts) % numVerts;
                allSpikeVerts.add(vIdx);
            }
        }

        // Deform
        for (int i = 0; i < verts.length; i += 3) {
            float[] v = vertsList.get(i / 3);
            double x = v[0], y = v[1], z = v[2];
            double len = Math.sqrt(x*x + y*y + z*z);
            double bump = 1.0 + params.getDeformation() * (rng.nextDouble() - 0.5) * 2.0;

            if (allSpikeVerts.contains(i / 3)) {
                double spikeVar = 1.0 + params.getSpikeLength() *
                        (1.0 + params.getRandomness() * (rng.nextDouble() - 0.5) * 2.0);
                bump *= spikeVar;
            }
            double newLen = params.getRadius() * bump;
            verts[i] = (float) (x / len * newLen);
            verts[i + 1] = (float) (y / len * newLen);
            verts[i + 2] = (float) (z / len * newLen);
        }
        getPoints().setAll(verts);
    }

    public float[] getVertsArray() { return verts; }
    public List<float[]> getVertsList() { return vertsList; }
    public int[] getFacesArray() { return faces; }
}
