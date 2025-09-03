package AsteroidField.tether;

import javafx.scene.shape.TriangleMesh;

/**
 * A simple tube (cylinder without caps) oriented along +Y.
 * Base at y=0, top at y=1. Scale Y to set length.
 */
public class TubeMesh extends TriangleMesh {

    public TubeMesh(int radialDivisions, float radius) {
        super();
        if (radialDivisions < 3) radialDivisions = 3;
        build(radialDivisions, radius);
    }

    private void build(int n, float r) {
        float y0 = 0f, y1 = 1f;

        // Points: bottom ring (y0), top ring (y1)
        float[] points = new float[n * 3 * 2];
        for (int i = 0; i < n; i++) {
            double theta = 2.0 * Math.PI * i / n;
            float x = (float) (r * Math.cos(theta));
            float z = (float) (r * Math.sin(theta));
            // bottom
            points[i * 3 + 0] = x;
            points[i * 3 + 1] = y0;
            points[i * 3 + 2] = z;
            // top
            int ti = n + i;
            points[ti * 3 + 0] = x;
            points[ti * 3 + 1] = y1;
            points[ti * 3 + 2] = z;
        }
        getPoints().setAll(points);

        // Dummy UV
        getTexCoords().addAll(0, 0);

        // Faces (two triangles per quad)
        int[] faces = new int[n * 2 * 6];
        int f = 0;
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            int p0 = i;
            int p1 = j;
            int p2 = n + i;
            int p3 = n + j;

            // tri 1: p0, p2, p1
            faces[f++] = p0; faces[f++] = 0;
            faces[f++] = p2; faces[f++] = 0;
            faces[f++] = p1; faces[f++] = 0;

            // tri 2: p1, p2, p3
            faces[f++] = p1; faces[f++] = 0;
            faces[f++] = p2; faces[f++] = 0;
            faces[f++] = p3; faces[f++] = 0;
        }
        getFaces().setAll(faces);

        // Smoothing groups
        int[] sm = new int[n * 2];
        for (int i = 0; i < sm.length; i++) sm[i] = 1;
        getFaceSmoothingGroups().setAll(sm);
    }
}