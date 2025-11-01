package AsteroidField.asteroids;

import java.util.ArrayList;
import java.util.List;

import AsteroidField.asteroids.geometry.HollowBaseSplitUtil;
import AsteroidField.asteroids.geometry.HollowBaseSplitUtil.Split;
import AsteroidField.asteroids.geometry.Portal;
import AsteroidField.asteroids.parameters.HollowBaseAsteroidParameters;
import AsteroidField.asteroids.providers.AsteroidMeshProvider;
import javafx.geometry.Point3D;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * Build an AsteroidNode by reusing the existing HollowBase mesh *and* your provider.
 * The combined mesh is split into outer/inner shells (each with equirect UVs),
 * so you can assign separate materials and cull modes.
 */
public final class HollowBaseAsteroidNodeFactory {

    private HollowBaseAsteroidNodeFactory() {}

    public static AsteroidNode create(AsteroidMeshProvider provider,
                                      HollowBaseAsteroidParameters p,
                                      PhongMaterial outerMaterial,
                                      PhongMaterial innerMaterial) {

        // Reuse your current geometry pipeline
        TriangleMesh combined = provider.generateMesh(p);

        // Split into two meshes with UVs
        Split split = HollowBaseSplitUtil.splitOuterInnerWithUV(combined);

        MeshView outer = new MeshView(split.outerMesh);
        outer.setDrawMode(DrawMode.FILL);
        outer.setCullFace(CullFace.BACK);
        if (outerMaterial != null) outer.setMaterial(outerMaterial);

        MeshView inner = new MeshView(split.innerMesh);
        inner.setDrawMode(DrawMode.FILL);
        inner.setCullFace(CullFace.FRONT);
        if (innerMaterial != null) inner.setMaterial(innerMaterial);

        // Metadata: radii + portals from parameters
        double outerR = p.getRadius();
        double innerR = p.getRadius() * p.getInnerRadiusRatio();
        double halfAngleRad = Math.toRadians(p.getMouthApertureDeg());
        List<Portal> portals = buildPortals(p.getMouthCount(), p.getSeed(), p.getMouthJitter(), halfAngleRad, innerR, outerR);

        // Handy link so your existing collidables supplier can include the inner shell without UI churn
        outer.getProperties().put("innerSibling", inner);

        return new AsteroidNode(outer, inner, innerR, outerR, portals);
    }

    private static List<Portal> buildPortals(int count, long seed, double jitter, double halfAngle,
                                             double innerR, double outerR) {
        List<Portal> list = new ArrayList<>();
        double[][] dirs = sampleDirections(count, seed, jitter);
        for (double[] d : dirs) {
            list.add(new Portal(new Point3D(d[0], d[1], d[2]), halfAngle, innerR, outerR));
        }
        return list;
    }

    /** Same golden-angle sampling approach you use in HollowBaseMesh. */
    private static double[][] sampleDirections(int count, long seed, double jitter) {
        java.util.Random rnd = new java.util.Random(seed ^ 0x6C62272E07BB0142L);
        int n = Math.max(1, count);
        double[][] dirs = new double[n][3];
        double goldenAngle = Math.PI * (3 - Math.sqrt(5));

        for (int k = 0; k < n; k++) {
            double y = (n == 1) ? 0.0 : (1.0 - 2.0 * k / (n - 1.0));
            double r = Math.sqrt(Math.max(0.0, 1.0 - y * y));
            double theta = k * goldenAngle;

            double x = r * Math.cos(theta);
            double z = r * Math.sin(theta);

            double j = Math.max(0.0, Math.min(1.0, jitter));
            if (j > 0.0) {
                double ax = rnd.nextDouble() * 2 - 1;
                double ay = rnd.nextDouble() * 2 - 1;
                double az = rnd.nextDouble() * 2 - 1;
                double am = Math.sqrt(ax*ax + ay*ay + az*az);
                if (am < 1e-8) { ax = 0; ay = 1; az = 0; am = 1; }
                ax /= am; ay /= am; az /= am;
                double ang = (rnd.nextDouble() * 2 - 1) * j * 0.35;
                double s = Math.sin(ang), c = Math.cos(ang);
                double dot = ax*x + ay*y + az*z;
                double rx = x*c + (ay*z - az*y)*s + ax*dot*(1 - c);
                double ry = y*c + (az*x - ax*z)*s + ay*dot*(1 - c);
                double rz = z*c + (ax*y - ay*x)*s + az*dot*(1 - c);
                x = rx; y = ry; z = rz;
            }
            double len = Math.sqrt(x*x + y*y + z*z);
            dirs[k][0] = x/len; dirs[k][1] = y/len; dirs[k][2] = z/len;
        }
        return dirs;
    }
}
