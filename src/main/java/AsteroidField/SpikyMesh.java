package AsteroidField;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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

    // 1. Generate spike directions & their parameters
    class Spike {
        double[] dir;
        double lengthMult;
        double widthRad;
        Spike(double[] d, double l, double w) { dir = d; lengthMult = l; widthRad = w; }
    }
    List<Spike> spikes = new ArrayList<>();
    for (int k = 0; k < spikeCount; k++) {
        double y = 1 - 2.0 * k / (spikeCount - 1.0);
        double r = Math.sqrt(1 - y * y);
        double phi = Math.PI * (3 - Math.sqrt(5)) * k;
        // Optional: apply spacing jitter (new param)
        double jitter = params.getSpikeSpacingJitter(); // e.g., 0..0.3
        phi += (rng.nextDouble() - 0.5) * 2.0 * Math.PI * jitter;

        double x = Math.cos(phi) * r;
        double z = Math.sin(phi) * r;
        double[] dir = {x, y, z};

        double lengthVar = 1.0 + params.getSpikeLength() *
                (1.0 + params.getRandomness() * (rng.nextDouble() - 0.5) * 2.0);
        // Randomize width per spike if desired
        double widthVar = params.getSpikeWidth() * (1.0 + params.getRandomness() * (rng.nextDouble() - 0.5));
        double widthRad = Math.max(0.001, widthVar * Math.PI); // interpret as radians

        spikes.add(new Spike(dir, lengthVar, widthRad));
    }

    // 2. Deform each vertex based on *maximum* spike effect
    for (int i = 0; i < verts.length; i += 3) {
        float[] v = vertsList.get(i / 3);
        double x = v[0], y = v[1], z = v[2];
        double len = Math.sqrt(x * x + y * y + z * z);
        double vx = x / len, vy = y / len, vz = z / len;
        double deformBump = 1.0 + params.getDeformation() * (rng.nextDouble() - 0.5) * 2.0;

        double maxSpikeEffect = 0;
        for (Spike spike : spikes) {
            double dot = vx * spike.dir[0] + vy * spike.dir[1] + vz * spike.dir[2];
            double angle = Math.acos(Math.max(-1, Math.min(1, dot)));
            // Use a smooth falloff (e.g., cosine, gaussian, etc.)
            double t = angle / spike.widthRad;
            if (t < 1.0) {
                double falloff = Math.cos(Math.PI * t / 2); // from 1 at center to 0 at edge
                double effect = spike.lengthMult * Math.max(0, falloff);
                if (effect > maxSpikeEffect) maxSpikeEffect = effect;
            }
        }

        double bump = deformBump;
        if (maxSpikeEffect > 0) bump *= maxSpikeEffect;
        double newLen = params.getRadius() * bump;
        verts[i] = (float) (vx * newLen);
        verts[i + 1] = (float) (vy * newLen);
        verts[i + 2] = (float) (vz * newLen);
    }
    getPoints().setAll(verts);
}

    public float[] getVertsArray() { return verts; }
    public List<float[]> getVertsList() { return vertsList; }
    public int[] getFacesArray() { return faces; }
}
