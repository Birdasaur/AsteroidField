package AsteroidField.tether;

import java.util.Optional;

import javafx.collections.ObservableFloatArray;
import javafx.collections.ObservableIntegerArray;
import javafx.geometry.Point3D;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Transform;

/**
 * Triangle-level ray/segment tests for JavaFX TriangleMesh.
 * Uses Möller–Trumbore with an optional FRONT-FACE rule.
 */
public final class MeshRaycast {

    private MeshRaycast(){}

    public static final class TriHit {
        public final MeshView meshView;
        public final int faceIndex;
        public final double t;            // param along [A->B], 0..1
        public final Point3D pointWorld;  // world-space
        public final Point3D normalWorld; // unit world-space geometric normal

        public TriHit(MeshView mv, int faceIndex, double t, Point3D pW, Point3D nW) {
            this.meshView = mv;
            this.faceIndex = faceIndex;
            this.t = t;
            this.pointWorld = pW;
            this.normalWorld = nW;
        }
    }

    private static final double EPS = 1e-8;

    /**
     * Return the nearest intersection against a TriangleMesh contained in the MeshView.
     * The segment endpoints (aWorld,bWorld) are given in SCENE space.
     *
     * @param frontFaceOnly if true, accepts only triangles with dot(n, dir) < 0 in mesh local space
     */
    public static Optional<TriHit> segmentMeshFirstHit(MeshView meshView,
                                                       Point3D aWorld,
                                                       Point3D bWorld,
                                                       boolean frontFaceOnly) {
        if (!(meshView.getMesh() instanceof TriangleMesh tm)) return Optional.empty();

        // Work in mesh LOCAL space
        Point3D aLocal = meshView.sceneToLocal(aWorld);
        Point3D bLocal = meshView.sceneToLocal(bWorld);
        Point3D dirLocal = bLocal.subtract(aLocal);

        ObservableFloatArray pts = tm.getPoints();
        ObservableIntegerArray faces = tm.getFaces();

        double bestT = Double.POSITIVE_INFINITY;
        int bestFace = -1;
        Point3D bestNLocal = null;

        for (int fi = 0, faceIndex = 0; fi < faces.size(); fi += 6, faceIndex++) {
            int p0 = faces.get(fi);
            int p1 = faces.get(fi + 2);
            int p2 = faces.get(fi + 4);

            int i0 = p0 * 3, i1 = p1 * 3, i2 = p2 * 3;

            Point3D v0 = new Point3D(pts.get(i0), pts.get(i0 + 1), pts.get(i0 + 2));
            Point3D v1 = new Point3D(pts.get(i1), pts.get(i1 + 1), pts.get(i1 + 2));
            Point3D v2 = new Point3D(pts.get(i2), pts.get(i2 + 1), pts.get(i2 + 2));

            // Optional early cull by front-face rule
            if (frontFaceOnly) {
                Point3D n = v1.subtract(v0).crossProduct(v2.subtract(v0));
                if (n.dotProduct(dirLocal) >= 0.0) continue;
            }

            double t = segmentTriangleT(aLocal, bLocal, v0, v1, v2, frontFaceOnly);
            if (!Double.isNaN(t) && t < bestT) {
                bestT = t;
                bestFace = faceIndex;
                Point3D nLocal = v1.subtract(v0).crossProduct(v2.subtract(v0));
                bestNLocal = normalizeSafe(nLocal);
            }
        }

        if (bestFace >= 0) {
            Point3D hitLocal = aLocal.add(dirLocal.multiply(bestT));
            Point3D hitWorld = meshView.localToScene(hitLocal);

            Transform l2s = meshView.getLocalToSceneTransform();
            Point3D nWorld = normalizeSafe(l2s.deltaTransform(bestNLocal));

            return Optional.of(new TriHit(meshView, bestFace, bestT, hitWorld, nWorld));
        }
        return Optional.empty();
    }

    /**
     * Möller–Trumbore segment-triangle; returns t in [0,1] or NaN if no hit.
     */
    private static double segmentTriangleT(Point3D a, Point3D b,
                                           Point3D v0, Point3D v1, Point3D v2,
                                           boolean frontFaceOnly) {
        Point3D dir = b.subtract(a);
        Point3D e1 = v1.subtract(v0);
        Point3D e2 = v2.subtract(v0);

        // det = (e1 x e2) · dir = n · dir
        Point3D pvec = dir.crossProduct(e2);
        double det = e1.dotProduct(pvec);

        if (frontFaceOnly) {
            if (det > -EPS) return Double.NaN; // reject backfaces and nearly parallel
        } else {
            if (Math.abs(det) < EPS) return Double.NaN;
        }
        double invDet = 1.0 / det;

        Point3D tvec = a.subtract(v0);
        double u = tvec.dotProduct(pvec) * invDet;
        if (u < 0.0 || u > 1.0) return Double.NaN;

        Point3D qvec = tvec.crossProduct(e1);
        double v = dir.dotProduct(qvec) * invDet;
        if (v < 0.0 || u + v > 1.0) return Double.NaN;

        double t = e2.dotProduct(qvec) * invDet;
        return (t >= 0.0 && t <= 1.0) ? t : Double.NaN;
    }

    private static Point3D normalizeSafe(Point3D v) {
        double m = Math.sqrt(v.getX()*v.getX() + v.getY()*v.getY() + v.getZ()*v.getZ());
        if (m < EPS) return new Point3D(0, 1, 0);
        return new Point3D(v.getX()/m, v.getY()/m, v.getZ()/m);
    }
}
