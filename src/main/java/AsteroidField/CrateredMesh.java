package AsteroidField;

import java.util.Collections;
import java.util.List;

public class CrateredMesh extends IcosphereMesh {
    public CrateredMesh(double radius, int subdivisions, CrateredAsteroidParameters params) {
        super(radius, subdivisions);
        deform(params);
    }

    /** Deform the mesh with the given crater parameters. Can be called repeatedly for real-time updates. */
    public void deform(CrateredAsteroidParameters params) {
        List<double[]> craters = params.getCraterCenters();
        if (craters == null) craters = Collections.emptyList();
        double radius = params.getRadius();

        for (int idx = 0; idx < vertsList.size(); idx++) {
            float[] v = vertsList.get(idx);
            double x = v[0], y = v[1], z = v[2];
            double r = Math.sqrt(x * x + y * y + z * z);
            double vx = x / r, vy = y / r, vz = z / r;
            double maxCraterEffect = 0;
            for (double[] crater : craters) {
                double dot = vx * crater[0] + vy * crater[1] + vz * crater[2];
                double angle = Math.acos(dot);
                double normalized = angle / (params.getCraterWidth() * Math.PI);
                if (normalized < 1.0) {
                    double effect = (1.0 - normalized * normalized);
                    if (effect > maxCraterEffect) maxCraterEffect = effect;
                }
            }
            double r2 = r;
            if (maxCraterEffect > 0) {
                r2 -= params.getCraterDepth() * radius * maxCraterEffect;
            }
            verts[idx*3]   = (float) (vx * r2);
            verts[idx*3+1] = (float) (vy * r2);
            verts[idx*3+2] = (float) (vz * r2);
        }
        getPoints().setAll(verts);
    }
}
