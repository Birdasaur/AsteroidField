package AsteroidField.asteroids.geometry;

import AsteroidField.asteroids.parameters.HomeBaseAsteroidParameters;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Deform-only Home Base mesh:
 * - Starts from IcosphereMesh.
 * - Applies exterior noise (like your other icosphere families).
 * - Sculpts one or more large, elliptical "amphitheater" mouths with a rounded (non-conical) profile.
 * - Pulls a broad interior cavity behind each mouth using a plateaued, raised-cosine falloff.
 * - Optionally applies local Taubin smoothing (λ/μ) only in influenced regions to reduce faceting
 *   at low subdivision levels.
 *
 * No faces are removed or added; mesh stays watertight.
 */
public class HomeBaseMesh extends IcosphereMesh {

    public HomeBaseMesh(double radius, int subdivisions, HomeBaseAsteroidParameters params) {
        super(radius, subdivisions);
        build(params);
    }

    private void build(HomeBaseAsteroidParameters p) {
        // 1) Exterior deformation (deterministic, similar to Cratered/Spiky styles)
        applyExteriorDeformation(p);

        // 2) Seeded mouth directions (Fibonacci sphere + jitter)
        double[][] axes = sampleDirections(p.getMouthCount(), p.getSeed(), p.getMouthJitter());

        // 3) Rounded amphitheater + cavity shaping (elliptical, plateaued)
        double majorRad = Math.toRadians(p.getMouthMajorDeg());
        double minorRad = Math.toRadians(p.getMouthMinorDeg());
        double blendMajor = majorRad * p.getCavityBlendScale();
        double blendMinor = minorRad * p.getCavityBlendScale();

        float[] influence = sculptCratersRounded(
                axes,
                majorRad, minorRad,
                blendMajor, blendMinor,
                p.getCavityRadiusRatio(),
                p.getRimLift(), p.getRimSharpness(),
                p.getPlateau(), p.getSoftnessExp(),
                p.getRadius()
        );

        // 4) Local Taubin smoothing only where influence > 0
        if (p.getSmoothIterations() > 0) {
            taubinSmoothLocal(influence, p.getSmoothIterations(), p.getSmoothLambda(), p.getSmoothMu());
        }

        // 5) Commit vertex buffer; faces unchanged (smoothing groups refreshed)
        getPoints().setAll(verts);
        getFaceSmoothingGroups().clear();
        int triCount = faces.length / 6;
        for (int i = 0; i < triCount; i++) {
            getFaceSmoothingGroups().addAll(1);
        }
    }

    /** Exterior deformation: radial noise bump with deterministic RNG. */
    private void applyExteriorDeformation(HomeBaseAsteroidParameters p) {
        Random rng = new Random(p.getSeed() ^ 0xA17E5B3DL);
        double deform = p.getDeformation();
        double R = p.getRadius();

        for (int i = 0; i < verts.length; i += 3) {
            // use original normalized direction from vertsList (matches your other families)
            float[] v = vertsList.get(i / 3);
            double x = v[0], y = v[1], z = v[2];
            double len = Math.sqrt(x * x + y * y + z * z);
            double nx = x / len, ny = y / len, nz = z / len;

            double bump = 1.0 + deform * (rng.nextDouble() - 0.5) * 2.0;
            double newLen = R * bump;

            verts[i]     = (float) (nx * newLen);
            verts[i + 1] = (float) (ny * newLen);
            verts[i + 2] = (float) (nz * newLen);
        }
    }

    /** Evenly spaced directions on the sphere with jitter (deterministic). */
    private static double[][] sampleDirections(int count, long seed, double jitter) {
        Random rnd = new Random(seed ^ 0x6C62272E07BB0142L);
        int n = Math.max(1, count);
        double[][] dirs = new double[n][3];
        double goldenAngle = Math.PI * (3 - Math.sqrt(5));

        for (int k = 0; k < n; k++) {
            double y = (n == 1) ? 0.0 : (1.0 - 2.0 * k / (n - 1.0));
            double r = Math.sqrt(Math.max(0.0, 1.0 - y * y));
            double phi = goldenAngle * k + (rnd.nextDouble() - 0.5) * 2.0 * Math.PI * clamp(jitter, 0.0, 0.5);
            double x = Math.cos(phi) * r;
            double z = Math.sin(phi) * r;
            double L = Math.sqrt(x * x + y * y + z * z);
            dirs[k][0] = x / L;
            dirs[k][1] = y / L;
            dirs[k][2] = z / L;
        }
        return dirs;
    }

    private static class Basis {
        final double[] n, u, v;
        Basis(double[] n, double[] u, double[] v) { this.n = n; this.u = u; this.v = v; }
        static Basis fromDir(double[] d) {
            double[] nn = norm(d);
            double[] a = (Math.abs(nn[1]) < 0.99) ? new double[]{0.0, 1.0, 0.0} : new double[]{1.0, 0.0, 0.0};
            double[] u = norm(cross(nn, a));
            double[] v = norm(cross(nn, u));
            return new Basis(nn, u, v);
        }
    }

    /**
     * Rounded amphitheater + cavity:
     * - Elliptical half-angle around each axis (major/minor).
     * - Wider blend ellipse (blendMajor/blendMinor) to define cavity influence.
     * - Plateau region near axis for a flat-ish bottom; outside uses a raised-cosine falloff
     *   with softness exponent for a non-conical, rounded look.
     * - Optional rim lift creates a subtle lip near the rim line.
     * Returns per-vertex influence (0..1) used for local smoothing.
     */
    private float[] sculptCratersRounded(
            double[][] axes,
            double majorRad, double minorRad,
            double blendMajor, double blendMinor,
            double cavityRatio, double rimLiftFrac, double rimSharpness,
            double plateau, double softnessExp,
            double outerR
    ) {
        // Precompute local bases
        Basis[] bases = new Basis[axes.length];
        for (int i = 0; i < axes.length; i++) {
            bases[i] = Basis.fromDir(axes[i]);
        }

        double targetR = outerR * cavityRatio;
        float[] influence = new float[verts.length / 3];

        for (int vi = 0; vi < verts.length; vi += 3) {
            // current normalized direction (after exterior deformation)
            double x = verts[vi], y = verts[vi + 1], z = verts[vi + 2];
            double r = Math.sqrt(x * x + y * y + z * z);
            if (r == 0.0) continue;
            double nx = x / r, ny = y / r, nz = z / r;

            // strongest influence among all mouths
            double best_s = 1e9, best_t = 1e9;
            for (Basis b : bases) {
                double du = nx * b.u[0] + ny * b.u[1] + nz * b.u[2];
                double dv = nx * b.v[0] + ny * b.v[1] + nz * b.v[2];
                double dd = nx * b.n[0] + ny * b.n[1] + nz * b.n[2];

                double phi = Math.atan2(dv, du);
                double theta = Math.acos(clamp(dd, -1.0, 1.0));

                // Elliptical half-angle at this azimuth:
                double c = Math.cos(phi), s = Math.sin(phi);

                double invA2 = (c * c) / (majorRad * majorRad) + (s * s) / (minorRad * minorRad);
                double Aphi = 1.0 / Math.sqrt(invA2); // rim ellipse

                double invB2 = (c * c) / (blendMajor * blendMajor) + (s * s) / (blendMinor * blendMinor);
                double Bphi = 1.0 / Math.sqrt(invB2); // cavity blend ellipse (wider)

                double sNorm = theta / Aphi; // 0 at axis, 1 ~ rim
                double tNorm = theta / Bphi; // 0 at axis, 1 ~ end of cavity influence

                if (sNorm < best_s) best_s = sNorm;
                if (tNorm < best_t) best_t = tNorm;
            }

            // Plateau + raised-cosine weight
            double w;
            double plateauClamped = clamp(plateau, 0.0, 0.8);
            if (best_t <= plateauClamped) {
                w = 1.0;
            } else if (best_t >= 1.0) {
                w = 0.0;
            } else {
                double u = (best_t - plateauClamped) / (1.0 - plateauClamped); // remap to [0..1]
                double exp = clamp(softnessExp, 0.5, 3.0);
                w = 0.5 * (1.0 + Math.cos(Math.PI * Math.pow(u, exp)));
            }

            // Rounded lip near the rim
            double sClamped = clamp((best_s - 0.8) / 0.2, 0.0, 1.0); // emphasize near rim
            double lip = (rimLiftFrac > 0.0)
                    ? rimLiftFrac * outerR * Math.pow(1.0 - sClamped, clamp(rimSharpness, 0.5, 3.0))
                    : 0.0;

            double desired = mix(r, targetR, w) + lip;
            double scale = desired / r;

            verts[vi]     = (float) (x * scale);
            verts[vi + 1] = (float) (y * scale);
            verts[vi + 2] = (float) (z * scale);

            influence[vi / 3] = (float) w;
        }

        return influence;
    }

    /**
     * Local Taubin (λ/μ) smoothing using 1-ring neighbors from VertHelper, applied
     * only where 'influence' > 0. This significantly reduces faceting in the
     * shaped region at low subdivisions without blurring the rest of the asteroid.
     */
    private void taubinSmoothLocal(float[] influence, int iters, double lambda, double mu) {
        if (iters <= 0) return;

        final int n = verts.length / 3;

        // Build adjacency once via VertHelper
        Map<Integer, Set<Integer>> adjMap = VertHelper.buildVertexAdjacency(facesList, n);
        int[][] adj = new int[n][];
        for (int i = 0; i < n; i++) {
            Set<Integer> s = adjMap.get(i);
            int size = (s != null) ? s.size() : 0;
            int[] row = new int[size];
            if (size > 0) {
                int k = 0;
                for (Integer v : s) row[k++] = v;
            }
            adj[i] = row;
        }

        // Position buffers
        double[] px = new double[n], py = new double[n], pz = new double[n];
        double[] tx = new double[n], ty = new double[n], tz = new double[n];
        for (int i = 0; i < n; i++) {
            px[i] = verts[3 * i];
            py[i] = verts[3 * i + 1];
            pz[i] = verts[3 * i + 2];
        }

        double lam = clamp(lambda, 0.0, 1.0);
        double m   = clamp(mu, -1.0, 0.0);

        for (int it = 0; it < iters; it++) {
            // λ-step
            for (int i = 0; i < n; i++) {
                if (influence[i] <= 1e-4f) {
                    tx[i] = px[i]; ty[i] = py[i]; tz[i] = pz[i];
                    continue;
                }
                int[] nbrs = adj[i];
                if (nbrs == null || nbrs.length == 0) {
                    tx[i] = px[i]; ty[i] = py[i]; tz[i] = pz[i];
                    continue;
                }
                double ax = 0.0, ay = 0.0, az = 0.0;
                for (int j : nbrs) { ax += px[j]; ay += py[j]; az += pz[j]; }
                ax /= nbrs.length; ay /= nbrs.length; az /= nbrs.length;
                double w = influence[i];
                tx[i] = px[i] + lam * w * (ax - px[i]);
                ty[i] = py[i] + lam * w * (ay - py[i]);
                tz[i] = pz[i] + lam * w * (az - pz[i]);
            }
            // copy back
            for (int i = 0; i < n; i++) { px[i] = tx[i]; py[i] = ty[i]; pz[i] = tz[i]; }

            // μ-step
            for (int i = 0; i < n; i++) {
                if (influence[i] <= 1e-4f) {
                    tx[i] = px[i]; ty[i] = py[i]; tz[i] = pz[i];
                    continue;
                }
                int[] nbrs = adj[i];
                if (nbrs == null || nbrs.length == 0) {
                    tx[i] = px[i]; ty[i] = py[i]; tz[i] = pz[i];
                    continue;
                }
                double ax = 0.0, ay = 0.0, az = 0.0;
                for (int j : nbrs) { ax += px[j]; ay += py[j]; az += pz[j]; }
                ax /= nbrs.length; ay /= nbrs.length; az /= nbrs.length;
                double w = influence[i];
                tx[i] = px[i] + m * w * (ax - px[i]);
                ty[i] = py[i] + m * w * (ay - py[i]);
                tz[i] = pz[i] + m * w * (az - pz[i]);
            }
            // copy back
            for (int i = 0; i < n; i++) { px[i] = tx[i]; py[i] = ty[i]; pz[i] = tz[i]; }
        }

        // Write back to verts[]
        for (int i = 0; i < n; i++) {
            verts[3 * i]     = (float) px[i];
            verts[3 * i + 1] = (float) py[i];
            verts[3 * i + 2] = (float) pz[i];
        }
    }

    // --- math helpers ---
    private static double[] cross(double[] a, double[] b) {
        return new double[]{ a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0] };
    }
    private static double[] norm(double[] a) {
        double l = Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
        return new double[]{ a[0] / l, a[1] / l, a[2] / l };
    }
    private static double clamp(double x, double a, double b) { return (x < a) ? a : (x > b) ? b : x; }
    private static double mix(double a, double b, double t) { return a * (1.0 - t) + b * t; }
}
