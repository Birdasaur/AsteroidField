package AsteroidField.editor.material;

import javafx.scene.shape.TriangleMesh;

import java.util.ArrayList;
import java.util.List;

/**
 * Acceleration grid over UV space for picking the face that contains a given (u,v).
 * Handles U seam by unwrapping triangles into a domain U in [0,2).
 */
public final class UvSpatialIndex {

    private final TriangleMesh mesh;
    private final int uCells; // number of cells along U in [0,2)
    private final int vCells; // number of cells along V in [0,1)
    private final List<int[]> cells; // flattened grid; each int[] is a dynamic bag of face indices

    private final double du, dv;

    public UvSpatialIndex(TriangleMesh mesh, int cellsUPerWrap, int cellsV) {
        this.mesh = mesh;
        this.uCells = cellsUPerWrap * 2; // because our U domain is [0,2)
        this.vCells = cellsV;
        this.du = 2.0 / uCells;
        this.dv = 1.0 / vCells;
        int total = uCells * vCells;
        cells = new ArrayList<>(total);
        for (int i = 0; i < total; i++) cells.add(new int[0]);
        build();
    }

    private void build() {
        var faces = mesh.getFaces();
        var tex = mesh.getTexCoords();
        int faceCount = faces.size() / 6;

        for (int fi = 0; fi < faceCount; fi++) {
            int base = fi * 6;
            int t0 = faces.get(base + 1);
            int t1 = faces.get(base + 3);
            int t2 = faces.get(base + 5);
            int t0i = t0 * 2, t1i = t1 * 2, t2i = t2 * 2;
            double u0 = tex.get(t0i),     v0 = tex.get(t0i + 1);
            double u1 = tex.get(t1i),     v1 = tex.get(t1i + 1);
            double u2 = tex.get(t2i),     v2 = tex.get(t2i + 1);

            // unwrap into [0,2)
            double[] U = new double[]{u0, u1, u2};
            double[] V = new double[]{v0, v1, v2};
            unwrapU(U);

            // compute AABB
            double minU = Math.min(U[0], Math.min(U[1], U[2]));
            double maxU = Math.max(U[0], Math.max(U[1], U[2]));
            double minV = Math.min(V[0], Math.min(V[1], V[2]));
            double maxV = Math.max(V[0], Math.max(V[1], V[2]));

            int u0c = clamp((int) Math.floor(minU / du), 0, uCells - 1);
            int u1c = clamp((int) Math.floor(maxU / du), 0, uCells - 1);
            int v0c = clamp((int) Math.floor(minV / dv), 0, vCells - 1);
            int v1c = clamp((int) Math.floor(maxV / dv), 0, vCells - 1);

            for (int uc = u0c; uc <= u1c; uc++) {
                for (int vc = v0c; vc <= v1c; vc++) {
                    addToCell(uc, vc, fi);
                }
            }
        }
    }

    private void addToCell(int uc, int vc, int faceIndex) {
        int idx = vc * uCells + uc;
        int[] arr = cells.get(idx);
        int n = arr.length;
        int[] next = new int[n + 1];
        System.arraycopy(arr, 0, next, 0, n);
        next[n] = faceIndex;
        cells.set(idx, next);
    }

    /** Returns the face index containing (u,v) or -1 if none. */
    public int pickFace(double u, double v) {
        // Search candidates at (u,v) and (u+1, v) to handle triangles unwrapped into [1,2).
        int face = pickFaceNear(u, v);
        if (face >= 0) return face;
        return pickFaceNear(u + 1.0, v);
    }

    private int pickFaceNear(double u, double v) {
        int uc = clamp((int) Math.floor(u / du), 0, uCells - 1);
        int vc = clamp((int) Math.floor(v / dv), 0, vCells - 1);
        int idx = vc * uCells + uc;
        int[] arr = cells.get(idx);
        if (arr.length == 0) return -1;

        var faces = mesh.getFaces();
        var tex = mesh.getTexCoords();
        for (int fi : arr) {
            int base = fi * 6;
            int t0 = faces.get(base + 1);
            int t1 = faces.get(base + 3);
            int t2 = faces.get(base + 5);
            int t0i = t0 * 2, t1i = t1 * 2, t2i = t2 * 2;
            double U0 = tex.get(t0i),     V0 = tex.get(t0i + 1);
            double U1 = tex.get(t1i),     V1 = tex.get(t1i + 1);
            double U2 = tex.get(t2i),     V2 = tex.get(t2i + 1);

            double[] U = new double[]{U0, U1, U2};
            double[] V = new double[]{V0, V1, V2};
            unwrapU(U);

            double au = projectNear(U[0], u);
            double bu = projectNear(U[1], u);
            double cu = projectNear(U[2], u);

            if (pointInTriangle(u, v, au, V[0], bu, V[1], cu, V[2])) {
                return fi;
            }
        }
        return -1;
    }

    private static void unwrapU(double[] U) {
        double min = Math.min(U[0], Math.min(U[1], U[2]));
        double max = Math.max(U[0], Math.max(U[1], U[2]));
        if (max - min > 0.5) {
            for (int i = 0; i < 3; i++) if (U[i] < 0.5) U[i] += 1.0;
        }
    }

    private static double projectNear(double Up, double uSample) {
        double d = Up - uSample;
        if (d > 0.5) return Up - 1.0;
        if (d < -0.5) return Up + 1.0;
        return Up;
    }

    private static boolean pointInTriangle(double px, double py,
                                           double ax, double ay,
                                           double bx, double by,
                                           double cx, double cy) {
        double w0 = orient2d(bx, by, cx, cy, px, py);
        double w1 = orient2d(cx, cy, ax, ay, px, py);
        double w2 = orient2d(ax, ay, bx, by, px, py);
        boolean hasNeg = (w0 < 0) || (w1 < 0) || (w2 < 0);
        boolean hasPos = (w0 > 0) || (w1 > 0) || (w2 > 0);
        return !(hasNeg && hasPos);
    }

    private static double orient2d(double ax, double ay,
                                   double bx, double by,
                                   double cx, double cy) {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
