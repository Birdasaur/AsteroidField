package AsteroidField.asteroids.geometry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.collections.ObservableFloatArray;
import javafx.collections.ObservableIntegerArray;
import javafx.geometry.Point3D;
import javafx.scene.shape.TriangleMesh;

/**
 * Split a combined HollowBase mesh (outer + inner in one TriangleMesh)
 * into two TriangleMeshes: OUTER (outward normals) and INNER (inward normals).
 * Also assigns equirect UVs (1:1 texcoord per vertex; faces’ t index mirrors p index).
 */
public final class HollowBaseSplitUtil {

    private HollowBaseSplitUtil() {}

    public static final class Split {
        public final TriangleMesh outerMesh;
        public final TriangleMesh innerMesh;
        public Split(TriangleMesh outerMesh, TriangleMesh innerMesh) {
            this.outerMesh = outerMesh;
            this.innerMesh = innerMesh;
        }
    }

    /** Partition faces by sign of dot(centroid, normal) and build two new meshes with equirect UVs. */
    public static Split splitOuterInnerWithUV(TriangleMesh combined) {
        ObservableFloatArray pts = combined.getPoints();
        ObservableIntegerArray faces = combined.getFaces();

        List<int[]> outerFaces = new ArrayList<>();
        List<int[]> innerFaces = new ArrayList<>();

        for (int fi = 0; fi < faces.size(); fi += 6) {
            int p0 = faces.get(fi);
            int p1 = faces.get(fi + 2);
            int p2 = faces.get(fi + 4);

            Point3D v0 = pointAt(pts, p0);
            Point3D v1 = pointAt(pts, p1);
            Point3D v2 = pointAt(pts, p2);

            Point3D e1 = v1.subtract(v0);
            Point3D e2 = v2.subtract(v0);
            Point3D n  = e1.crossProduct(e2);
            Point3D c  = v0.add(v1).add(v2).multiply(1.0 / 3.0);

            double sign = n.getX()*c.getX() + n.getY()*c.getY() + n.getZ()*c.getZ();
            if (sign >= 0.0) outerFaces.add(new int[]{p0, p1, p2});
            else innerFaces.add(new int[]{p0, p1, p2});
        }

        TriangleMesh outer = buildSubmeshWithUV(pts, outerFaces);
        TriangleMesh inner = buildSubmeshWithUV(pts, innerFaces);
        return new Split(outer, inner);
    }

    private static TriangleMesh buildSubmeshWithUV(ObservableFloatArray srcPts, List<int[]> faces) {
        TriangleMesh m = new TriangleMesh();

        Map<Integer,Integer> remap = new HashMap<>();
        List<Float> outPts = new ArrayList<>();
        int nextIndex = 0;

        int[] remappedFaces = new int[faces.size() * 6];
        int fIdx = 0;

        for (int[] tri : faces) {
            for (int k = 0; k < 3; k++) {
                int oldIndex = tri[k];
                Integer newIndex = remap.get(oldIndex);
                if (newIndex == null) {
                    Point3D p = pointAt(srcPts, oldIndex);
                    outPts.add((float) p.getX());
                    outPts.add((float) p.getY());
                    outPts.add((float) p.getZ());
                    newIndex = nextIndex++;
                    remap.put(oldIndex, newIndex);
                }
                remappedFaces[fIdx++] = newIndex; // p index
                remappedFaces[fIdx++] = 0;        // t index placeholder
            }
        }

        float[] ptsArray = new float[outPts.size()];
        for (int i = 0; i < outPts.size(); i++) ptsArray[i] = outPts.get(i);
        m.getPoints().setAll(ptsArray);

        // Equirect UVs (1 texcoord per vertex)
        int pointCount = ptsArray.length / 3;
        float[] uv = new float[pointCount * 2];
        for (int i = 0; i < pointCount; i++) {
            double x = ptsArray[i*3];
            double y = ptsArray[i*3 + 1];
            double z = ptsArray[i*3 + 2];
            double r = Math.sqrt(x*x + y*y + z*z);
            if (r == 0.0) r = 1.0;
            double nx = x / r, ny = y / r, nz = z / r;
            double u = (Math.atan2(nz, nx) + Math.PI) / (2.0 * Math.PI);
            double v = Math.acos(ny) / Math.PI;
            uv[i*2] = (float) u;
            uv[i*2 + 1] = (float) v;
        }
        m.getTexCoords().setAll(uv);

        // Faces: make t indices mirror p indices
        for (int i = 0; i < remappedFaces.length; i += 2) {
            int pIndex = remappedFaces[i];
            remappedFaces[i + 1] = pIndex;
        }
        m.getFaces().setAll(remappedFaces);

        // Smoothing groups (1 per face keeps shading mellow)
        int triCount = remappedFaces.length / 6;
        for (int i = 0; i < triCount; i++) m.getFaceSmoothingGroups().addAll(1);

        return m;
    }

    private static Point3D pointAt(ObservableFloatArray pts, int pIndex) {
        int i = pIndex * 3;
        return new Point3D(pts.get(i), pts.get(i + 1), pts.get(i + 2));
    }
}
