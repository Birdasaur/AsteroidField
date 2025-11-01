package AsteroidField.asteroids.materials;

import javafx.collections.ObservableFloatArray;
import javafx.collections.ObservableIntegerArray;
import javafx.scene.shape.TriangleMesh;

/** Assign equirectangular UVs to any spherical-ish mesh (1 texcoord per point; t indices mirror p). */
public final class UVUtil {

    private UVUtil() {}

    public static void applyEquirectUVs(TriangleMesh mesh) {
        ObservableFloatArray pts = mesh.getPoints();
        int pointCount = pts.size() / 3;

        float[] uv = new float[pointCount * 2];
        for (int i = 0; i < pointCount; i++) {
            double x = pts.get(3*i);
            double y = pts.get(3*i + 1);
            double z = pts.get(3*i + 2);
            double r = Math.sqrt(x*x + y*y + z*z);
            if (r == 0.0) r = 1.0;
            double nx = x / r, ny = y / r, nz = z / r;
            double u = (Math.atan2(nz, nx) + Math.PI) / (2.0 * Math.PI);
            double v = Math.acos(ny) / Math.PI;
            uv[2*i] = (float) u;
            uv[2*i + 1] = (float) v;
        }
        mesh.getTexCoords().setAll(uv);

        ObservableIntegerArray faces = mesh.getFaces();
        for (int i = 0; i < faces.size(); i += 2) {
            int pIndex = faces.get(i);
            faces.set(i + 1, pIndex);
        }
    }
}
