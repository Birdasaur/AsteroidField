package AsteroidField.asteroids.geometry;

import AsteroidField.asteroids.parameters.CrateredAsteroidParameters;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CrateredMesh extends IcosphereMesh {

    public CrateredMesh(double radius, int subdivisions, CrateredAsteroidParameters params) {
        super(radius, subdivisions);
        deform(params);
    }

    public void deform(CrateredAsteroidParameters params) {
        List<double[]> craters = params.getCraterCenters();
        if (craters == null) {
            craters = Collections.emptyList();
        }
        double radius = params.getRadius();

        // Deterministic RNG for deformation, to avoid jumpiness
        Random deformRng = new Random(params.getSeed() ^ 0xFACE1234);
        double deform = params.getDeformation();

        for (int idx = 0; idx < vertsList.size(); idx++) {
            float[] v = vertsList.get(idx);
            double x = v[0], y = v[1], z = v[2];
            double r = Math.sqrt(x * x + y * y + z * z);
            double vx = x / r, vy = y / r, vz = z / r;
            double maxCraterEffect = 0;
            for (double[] crater : craters) {
                double dot = vx * crater[0] + vy * crater[1] + vz * crater[2];
                double angle = Math.acos(Math.max(-1, Math.min(1, dot)));
                double normalized = angle / (params.getCraterWidth() * Math.PI);
                if (normalized < 1.0) {
                    double effect = (1.0 - normalized * normalized);
                    if (effect > maxCraterEffect) {
                        maxCraterEffect = effect;
                    }
                }
            }
            double r2 = r;
            if (maxCraterEffect > 0) {
                r2 -= params.getCraterDepth() * radius * maxCraterEffect;
            }
            // --- Apply deformation bump here ---
            double bump = 1.0 + deform * (deformRng.nextDouble() - 0.5) * 2.0;
            r2 *= bump;

            verts[idx * 3] = (float) (vx * r2);
            verts[idx * 3 + 1] = (float) (vy * r2);
            verts[idx * 3 + 2] = (float) (vz * r2);
        }
        getPoints().setAll(verts);
    }
}
