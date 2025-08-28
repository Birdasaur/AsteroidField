package AsteroidField;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SpikyMesh extends IcosphereMesh {

    public SpikyMesh(double radius, int subdivisions, SpikyAsteroidParameters params) {
        super(radius, subdivisions);
        deform(params);
    }

    /** Deform the mesh using new spiky parameters (can be called any time). */
    public void deform(SpikyAsteroidParameters params) {
        Random rng = new Random(params.getSeed());
        int spikeCount = Math.max(1, params.getSpikeCount());
        int numVerts = verts.length / 3;

        //Generate spike directions & their parameters
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
            double jitter = params.getSpikeSpacingJitter(); // 0..0.3 typically
            phi += (rng.nextDouble() - 0.5) * 2.0 * Math.PI * jitter;

            double x = Math.cos(phi) * r;
            double z = Math.sin(phi) * r;
            double[] dir = {x, y, z};

            double lengthVar = 1.0 + params.getSpikeLength() *
                    (1.0 + params.getRandomness() * (rng.nextDouble() - 0.5) * 2.0);
            double widthVar = params.getSpikeWidth() * (1.0 + params.getRandomness() * (rng.nextDouble() - 0.5));
            double widthRad = Math.max(0.001, widthVar * Math.PI); // radians

            spikes.add(new Spike(dir, lengthVar, widthRad));
        }

        //Deform each vertex based on *maximum* spike effect
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
                double t = angle / spike.widthRad;
                if (t < 1.0) {
                    double falloff = Math.cos(Math.PI * t / 2); // 1 at center, 0 at edge
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
}