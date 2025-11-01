package AsteroidField.spacecraft.collision;

import java.util.Optional;

import javafx.collections.ObservableFloatArray;
import javafx.collections.ObservableIntegerArray;
import javafx.geometry.Point3D;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Transform;

/**
 * Swept sphere (center C0->C1, radius R) vs TriangleMesh (static).
 * Inputs: endpoints are in SCENE coordinates. Internally we transform into the mesh LOCAL space.
 * Returns earliest time-of-impact (TOI), contact point (SCENE), and contact normal (SCENE).
 *
 * This initial version does face sweeps (plane at offset R). If you later need extra robustness
 * on razor-sharp rims, we can add edge/vertex sweeps (easy extension).
 */
public final class SweepSphereMesh {

    private SweepSphereMesh() {}

    public static final class Hit {
        public final MeshView meshView;
        public final double t;            // TOI in [0,1] along C0->C1
        public final Point3D pointScene;  // SCENE-space contact point
        public final Point3D normalScene; // SCENE-space unit normal
        public Hit(MeshView meshView, double t, Point3D pointScene, Point3D normalScene) {
            this.meshView = meshView;
            this.t = t;
            this.pointScene = pointScene;
            this.normalScene = normalScene;
        }
    }

    private static final double EPS = 1e-8;

    public static Optional<Hit> firstHit(MeshView mv,
                                         Point3D c0Scene,
                                         Point3D c1Scene,
                                         double radius,
                                         boolean frontFaceOnly) {
        if (!(mv.getMesh() instanceof TriangleMesh tm)) return Optional.empty();

        // Scene → Local
        Point3D c0 = mv.sceneToLocal(c0Scene);
        Point3D c1 = mv.sceneToLocal(c1Scene);
        Point3D d  = c1.subtract(c0);

        ObservableFloatArray pts = tm.getPoints();
        ObservableIntegerArray faces = tm.getFaces();

        double bestT = Double.POSITIVE_INFINITY;
        Point3D bestPointLocal = null;
        Point3D bestNormalLocal = null;

        for (int fi = 0; fi < faces.size(); fi += 6) {
            int p0 = faces.get(fi);
            int p1 = faces.get(fi + 2);
            int p2 = faces.get(fi + 4);

            int i0 = p0 * 3, i1 = p1 * 3, i2 = p2 * 3;
            Point3D v0 = new Point3D(pts.get(i0), pts.get(i0 + 1), pts.get(i0 + 2));
            Point3D v1 = new Point3D(pts.get(i1), pts.get(i1 + 1), pts.get(i1 + 2));
            Point3D v2 = new Point3D(pts.get(i2), pts.get(i2 + 1), pts.get(i2 + 2));

            // Face normal (unit)
            Point3D n0 = v1.subtract(v0).crossProduct(v2.subtract(v0));
            double nl = Math.sqrt(n0.getX()*n0.getX() + n0.getY()*n0.getY() + n0.getZ()*n0.getZ());
            if (nl < EPS) continue;
            Point3D n = new Point3D(n0.getX()/nl, n0.getY()/nl, n0.getZ()/nl);

            double nd = n.dotProduct(d);
            boolean approaching = nd < -EPS;
            if (frontFaceOnly && !approaching) {
                // Not approaching the front face; skip face sweep
                continue;
            }

            // Plane at offset R: dot(n, C(t) - v0) = R → t = (R - dot(n, c0 - v0)) / dot(n, d)
            double numer = radius - n.dotProduct(c0.subtract(v0));
            double denom = nd;
            if (denom < -EPS) {
                double t = numer / denom;
                if (t >= -1e-6 && t <= 1.0 + 1e-6) {
                    double tClamped = clamp01(t);
                    Point3D cAt = c0.add(d.multiply(tClamped));
                    Point3D q = cAt.subtract(n.multiply(radius)); // projected on triangle plane
                    if (pointInTriangle(q, v0, v1, v2, n)) {
                        if (tClamped < bestT) {
                            bestT = tClamped;
                            bestPointLocal = q;
                            bestNormalLocal = n;
                        }
                    }
                }
            }
        }

        if (bestPointLocal != null && bestNormalLocal != null) {
            // Local → Scene
            Transform l2s = mv.getLocalToSceneTransform();
            Point3D pointScene = mv.localToScene(bestPointLocal);
            Point3D normalScene = normalize(l2s.deltaTransform(bestNormalLocal));
            return Optional.of(new Hit(mv, bestT, pointScene, normalScene));
        }

        return Optional.empty();
    }

    // --- helpers ---
    private static boolean pointInTriangle(Point3D p, Point3D a, Point3D b, Point3D c, Point3D n) {
        Point3D ab = b.subtract(a), bc = c.subtract(b), ca = a.subtract(c);
        Point3D ap = p.subtract(a), bp = p.subtract(b), cp = p.subtract(c);
        Point3D c1 = ab.crossProduct(ap);
        Point3D c2 = bc.crossProduct(bp);
        Point3D c3 = ca.crossProduct(cp);
        return dot(c1, n) >= -1e-8 && dot(c2, n) >= -1e-8 && dot(c3, n) >= -1e-8;
    }

    private static double clamp01(double x) { return x < 0 ? 0 : (x > 1 ? 1 : x); }
    private static double dot(Point3D a, Point3D b) { return a.getX()*b.getX() + a.getY()*b.getY() + a.getZ()*b.getZ(); }
    private static Point3D normalize(Point3D v) {
        double m = Math.sqrt(v.getX()*v.getX() + v.getY()*v.getY() + v.getZ()*v.getZ());
        if (m < EPS) return new Point3D(0,1,0);
        return new Point3D(v.getX()/m, v.getY()/m, v.getZ()/m);
    }
}
