package AsteroidField.asteroids.geometry;

import AsteroidField.asteroids.parameters.HomeBaseAsteroidParameters;

import java.util.Random;

/**
 * Deform-only "Home Base":
 *  - Exterior noise (like IcosphereDeformer)
 *  - One or more large, elliptical amphitheater craters
 *  - A broad interior cavity behind each mouth, blended smoothly
 * 
 * Keeps faces intact (no culling), so the mesh is watertight.
 */
public class HomeBaseMesh extends IcosphereMesh {

    public HomeBaseMesh(double radius, int subdivisions, HomeBaseAsteroidParameters params) {
        super(radius, subdivisions);
        build(params);
    }

    private void build(HomeBaseAsteroidParameters p) {
        // 1) Exterior deformation (deterministic)
        applyExteriorDeformation(p);

        // 2) Mouth directions via Fibonacci sphere with jitter
        double[][] axes = sampleDirections(p.getMouthCount(), p.getSeed(), p.getMouthJitter());

        // 3) Sculpt amphitheater(s) + cavity (elliptical half-angles, smooth falloff, optional lip)
        double majorRad = Math.toRadians(p.getMouthMajorDeg());
        double minorRad = Math.toRadians(p.getMouthMinorDeg());
        double blendMajor = majorRad * p.getCavityBlendScale();
        double blendMinor = minorRad * p.getCavityBlendScale();
        deformCratersAndCavity(axes, majorRad, minorRad, blendMajor, blendMinor,
                p.getCavityRadiusRatio(), p.getRimLift(), p.getRimSharpness(), p.getRadius());

        // 4) Push to mesh
        getPoints().setAll(verts);
        getFaceSmoothingGroups().clear();
        int triCount = faces.length / 6;
        for (int i = 0; i < triCount; i++) getFaceSmoothingGroups().addAll(1);
    }

    private void applyExteriorDeformation(HomeBaseAsteroidParameters p) {
        Random rng = new Random(p.getSeed() ^ 0xA17E5B3DL);
        double deform = p.getDeformation();
        double R = p.getRadius();

        for (int i = 0; i < verts.length; i += 3) {
            float[] v = vertsList.get(i / 3);
            double x = v[0], y = v[1], z = v[2];
            double len = Math.sqrt(x*x + y*y + z*z);
            double nx = x/len, ny = y/len, nz = z/len;

            double bump = 1.0 + deform * (rng.nextDouble() - 0.5) * 2.0;
            double newLen = R * bump;

            verts[i  ] = (float)(nx * newLen);
            verts[i+1] = (float)(ny * newLen);
            verts[i+2] = (float)(nz * newLen);
        }
    }

    private static double[][] sampleDirections(int count, long seed, double jitter) {
        Random rnd = new Random(seed ^ 0x6C62272E07BB0142L);
        int n = Math.max(1, count);
        double[][] dirs = new double[n][3];
        double ga = Math.PI * (3 - Math.sqrt(5)); // golden angle

        for (int k = 0; k < n; k++) {
            double y = (n == 1) ? 0.0 : (1.0 - 2.0 * k / (n - 1.0));
            double r = Math.sqrt(Math.max(0, 1 - y*y));
            double phi = ga * k + (rnd.nextDouble() - 0.5) * 2.0 * Math.PI * Math.max(0, Math.min(0.5, jitter));

            double x = Math.cos(phi) * r;
            double z = Math.sin(phi) * r;
            double len = Math.sqrt(x*x + y*y + z*z);
            dirs[k][0] = x/len; dirs[k][1] = y/len; dirs[k][2] = z/len;
        }
        return dirs;
    }

    private static class Basis {
        final double[] n, u, v;
        Basis(double[] n, double[] u, double[] v){ this.n=n; this.u=u; this.v=v; }
        static Basis fromDir(double[] d) {
            double[] nn = norm(d);
            double[] a = (Math.abs(nn[1]) < 0.99) ? new double[]{0,1,0} : new double[]{1,0,0};
            double[] u = norm(cross(nn, a));
            double[] v = norm(cross(nn, u));
            return new Basis(nn, u, v);
        }
    }

    private void deformCratersAndCavity(
            double[][] axes,
            double majorRad, double minorRad,
            double blendMajor, double blendMinor,
            double cavityRatio, double rimLiftFrac, double rimSharpness,
            double outerR
    ) {
        // Precompute bases for each axis
        Basis[] bases = new Basis[axes.length];
        for (int i = 0; i < axes.length; i++) bases[i] = Basis.fromDir(axes[i]);

        double targetR = outerR * cavityRatio;

        for (int i = 0; i < verts.length; i += 3) {
            // current normalized direction
            double x = verts[i], y = verts[i+1], z = verts[i+2];
            double r = Math.sqrt(x*x + y*y + z*z);
            if (r == 0) continue;
            double nx = x/r, ny = y/r, nz = z/r;

            // Find the strongest crater influence (min normalized elliptical angle s in [0,∞))
            double best_s = 1e9, best_t = 1e9; // s for mouth rim, t for cavity blend region
            for (Basis b : bases) {
                // projections in local frame around axis n
                double du = nx*b.u[0] + ny*b.u[1] + nz*b.u[2];
                double dv = nx*b.v[0] + ny*b.v[1] + nz*b.v[2];
                double dd = nx*b.n[0] + ny*b.n[1] + nz*b.n[2];

                // azimuth around axis and central angle
                double phi = Math.atan2(dv, du);
                phi = (phi < 0) ? (phi + 2*Math.PI) : phi;
                double theta = Math.acos(Math.max(-1, Math.min(1, dd)));

                // elliptical half-angle at this azimuth:
                double c = Math.cos(phi), s = Math.sin(phi);
                double invA2 = (c*c)/(majorRad*majorRad) + (s*s)/(minorRad*minorRad);
                double Aphi = 1.0 / Math.sqrt(invA2);

                double invB2 = (c*c)/(blendMajor*blendMajor) + (s*s)/(blendMinor*blendMinor);
                double Bphi = 1.0 / Math.sqrt(invB2);

                double sNorm = theta / Aphi; // 0 at axis, 1 at mouth rim, >1 outside
                double tNorm = theta / Bphi; // 0 at axis, 1 at cavity blend rim

                if (sNorm < best_s) best_s = sNorm;
                if (tNorm < best_t) best_t = tNorm;
            }

            // Smooth weights
            double sClamped = clamp01(best_s);
            double tClamped = clamp01(best_t);

            // crater cavity blend: w=1 at axis -> targetR, w=0 outside -> keep r
            double w = 1.0 - smoothstep(0.0, 1.0, tClamped);
            double desired = mix(r, targetR, w);

            // optional lip near mouth rim (at s ≈ 1)
            double lip = 0.0;
            if (rimLiftFrac > 0) {
                double k = clamp01(Math.pow(smoothstep(0.8, 1.0, sClamped), rimSharpness));
                lip = rimLiftFrac * outerR * k;
            }

            double newR = desired + lip;
            double scale = newR / r;
            verts[i  ] = (float)(x * scale);
            verts[i+1] = (float)(y * scale);
            verts[i+2] = (float)(z * scale);
        }
    }

    // --- math helpers ---
    private static double[] cross(double[] a, double[] b) {
        return new double[]{ a[1]*b[2]-a[2]*b[1], a[2]*b[0]-a[0]*b[2], a[0]*b[1]-a[1]*b[0] };
    }
    private static double[] norm(double[] a) {
        double l = Math.sqrt(a[0]*a[0]+a[1]*a[1]+a[2]*a[2]); return new double[]{a[0]/l,a[1]/l,a[2]/l};
    }
    private static double clamp01(double x){ return (x<0)?0:(x>1)?1:x; }
    private static double smoothstep(double a, double b, double x){
        double t = clamp01((x - a) / (b - a)); return t*t*(3 - 2*t);
    }
    private static double mix(double a, double b, double t){ return a*(1-t) + b*t; }
}
