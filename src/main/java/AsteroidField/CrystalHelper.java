package AsteroidField;

import java.util.*;

public class CrystalHelper {

    /** Generate a jittered, faceted ring of points around a center and axis */
    public static List<float[]> generateFacetedRing(
            double[] center, double[] axis, double[] up, double[] right,
            int sides, double radius, double facetJitter, Random rng)
    {
        List<float[]> ring = new ArrayList<>(sides);
        for (int s = 0; s < sides; s++) {
            double theta = 2 * Math.PI * s / sides;
            double[] dir = add(scale(up, Math.cos(theta)), scale(right, Math.sin(theta)));
            double jitter = 1.0 + (rng.nextDouble() - 0.5) * 2.0 * facetJitter;
            float[] pt = new float[]{
                    (float)(center[0] + dir[0] * radius * jitter),
                    (float)(center[1] + dir[1] * radius * jitter),
                    (float)(center[2] + dir[2] * radius * jitter)
            };
            ring.add(pt);
        }
        return ring;
    }

    /** Apply a progressive or random twist to a ring of points about a given axis and center.
     * maxTwistRadians controls total amount of twist (positive = right hand rule).
     * For organic minerals, random twist per-vertex is nice; for stylized, try progressive twist.
     */
    public static void applyTwistToRing(List<float[]> ring, double maxTwistRadians, double[] axis, double[] center, Random rng) {
        int n = ring.size();
        for (int i = 0; i < n; i++) {
            float[] v = ring.get(i);
            double twist = rng.nextDouble() * maxTwistRadians;
            float[] rotated = rotateAroundAxis(v, center, axis, twist);
            v[0] = rotated[0]; v[1] = rotated[1]; v[2] = rotated[2];
        }
    }

    /** Rotate a single point around an axis and center by angle (radians). */
    public static float[] rotateAroundAxis(float[] point, double[] center, double[] axis, double angle) {
        // Move point to origin-relative
        double[] p = new double[]{point[0] - center[0], point[1] - center[1], point[2] - center[2]};
        double[] k = normalize(axis);
        double cos = Math.cos(angle), sin = Math.sin(angle);
        // Rodrigues' rotation formula
        double[] rotated = new double[3];
        rotated[0] = p[0] * cos + (k[1] * p[2] - k[2] * p[1]) * sin + k[0] * (k[0] * p[0] + k[1] * p[1] + k[2] * p[2]) * (1 - cos);
        rotated[1] = p[1] * cos + (k[2] * p[0] - k[0] * p[2]) * sin + k[1] * (k[0] * p[0] + k[1] * p[1] + k[2] * p[2]) * (1 - cos);
        rotated[2] = p[2] * cos + (k[0] * p[1] - k[1] * p[0]) * sin + k[2] * (k[0] * p[0] + k[1] * p[1] + k[2] * p[2]) * (1 - cos);
        // Move back to world coords
        return new float[]{(float)(rotated[0] + center[0]), (float)(rotated[1] + center[1]), (float)(rotated[2] + center[2])};
    }

    /** Generate a single tip (pointy) or beveled tip */
    public static List<float[]> generateTip(
            double[] tipCenter, double[] axis, double[] up, double[] right,
            int sides, double radius, double tipRadiusScale,
            boolean pointy, boolean beveled, double bevelDepth, double crystalLength, Random rng)
    {
        List<float[]> tipRing = new ArrayList<>();
        if (pointy) {
            tipRing.add(new float[]{(float)tipCenter[0], (float)tipCenter[1], (float)tipCenter[2]});
        } else if (beveled) {
            double[] bevelCenter = new double[] {
                    tipCenter[0] - axis[0] * bevelDepth * crystalLength,
                    tipCenter[1] - axis[1] * bevelDepth * crystalLength,
                    tipCenter[2] - axis[2] * bevelDepth * crystalLength
            };
            for (int s = 0; s < sides; s++) {
                double theta = 2 * Math.PI * s / sides;
                double[] dir = add(scale(up, Math.cos(theta)), scale(right, Math.sin(theta)));
                float[] pt = new float[]{
                        (float)(bevelCenter[0] + dir[0] * radius * tipRadiusScale),
                        (float)(bevelCenter[1] + dir[1] * radius * tipRadiusScale),
                        (float)(bevelCenter[2] + dir[2] * radius * tipRadiusScale)
                };
                tipRing.add(pt);
            }
        } else {
            for (int s = 0; s < sides; s++) {
                double theta = 2 * Math.PI * s / sides;
                double[] dir = add(scale(up, Math.cos(theta)), scale(right, Math.sin(theta)));
                float[] pt = new float[]{
                        (float)(tipCenter[0] + dir[0] * radius * tipRadiusScale),
                        (float)(tipCenter[1] + dir[1] * radius * tipRadiusScale),
                        (float)(tipCenter[2] + dir[2] * radius * tipRadiusScale)
                };
                tipRing.add(pt);
            }
        }
        return tipRing;
    }

    /** Randomly select the number of prism sides for this crystal (for variety) */
    public static int randomizePrismSides(int minSides, int maxSides, Random rng) {
        if (minSides >= maxSides) return minSides;
        return minSides + rng.nextInt(maxSides - minSides + 1);
    }

    /** Add a "chisel notch" to a ring by pulling/pushing some vertices in the ring inward or outward. */
    public static void addChiselNotch(List<float[]> ring, double notchDepth, int maxNotches, Random rng) {
        int n = ring.size();
        int numNotches = 1 + rng.nextInt(Math.max(1, maxNotches));
        Set<Integer> used = new HashSet<>();
        for (int i = 0; i < numNotches; i++) {
            int idx;
            do { idx = rng.nextInt(n); } while (used.contains(idx));
            used.add(idx);

            float[] v = ring.get(idx);
            double[] center = new double[3];
            for (float[] vert : ring) {
                center[0] += vert[0];
                center[1] += vert[1];
                center[2] += vert[2];
            }
            center[0] /= n; center[1] /= n; center[2] /= n;
            double[] dir = new double[] {
                v[0] - center[0],
                v[1] - center[1],
                v[2] - center[2]
            };
            double len = Math.sqrt(dir[0]*dir[0] + dir[1]*dir[1] + dir[2]*dir[2]);
            if (len > 1e-8) {
                dir[0] /= len; dir[1] /= len; dir[2] /= len;
                v[0] += dir[0] * notchDepth;
                v[1] += dir[1] * notchDepth;
                v[2] += dir[2] * notchDepth;
            }
        }
    }
    public static double[] floatToDouble(float[] v) {
        return new double[]{v[0], v[1], v[2]};
    }
    // --- Utility: convert double[] to float[] (for positions/normals)
    public static float[] centerAsFloat(double[] d) {
        return new float[]{(float)d[0], (float)d[1], (float)d[2]};
    }
    public static float[] doubleToFloat(double[] d) {
        return new float[]{(float)d[0], (float)d[1], (float)d[2]};
    }
    
    /** Compute a random axis (for tilt/offshoots) given a base direction and max angle */
    public static double[] randomTiltedAxis(double[] base, double maxAngle, Random rng) {
        double angle = rng.nextDouble() * maxAngle;
        double azimuth = rng.nextDouble() * 2.0 * Math.PI;
        double[] perp = pickPerpendicular(base, rng);
        double[] cross = cross(base, perp);
        double[] tilted = new double[3];
        for (int i = 0; i < 3; i++)
            tilted[i] = base[i]*Math.cos(angle) + perp[i]*Math.sin(angle)*Math.cos(azimuth) + cross[i]*Math.sin(angle)*Math.sin(azimuth);
        double len = Math.sqrt(tilted[0]*tilted[0] + tilted[1]*tilted[1] + tilted[2]*tilted[2]);
        for (int i = 0; i < 3; i++) tilted[i] /= len;
        return tilted;
    }

    /** Fracture a prism (list of vertices) by offsetting points on one side of a plane */
    public static void generateFracturePlane(List<float[]> verts, double[] planeNormal, double[] planePoint, double depth) {
        for (float[] v : verts) {
            double[] p = new double[]{v[0], v[1], v[2]};
            double dot = (p[0] - planePoint[0]) * planeNormal[0]
                       + (p[1] - planePoint[1]) * planeNormal[1]
                       + (p[2] - planePoint[2]) * planeNormal[2];
            if (dot > 0) { // One side of the plane
                v[0] += planeNormal[0] * depth;
                v[1] += planeNormal[1] * depth;
                v[2] += planeNormal[2] * depth;
            }
        }
    }

    /** Vector: Add */
    public static double[] add(double[] a, double[] b) {
        return new double[]{a[0]+b[0], a[1]+b[1], a[2]+b[2]};
    }
    /** Vector: Scale */
    public static double[] scale(double[] v, double s) {
        return new double[]{v[0]*s, v[1]*s, v[2]*s};
    }
    /** Vector: Cross product */
    public static double[] cross(double[] a, double[] b) {
        return new double[]{
                a[1]*b[2] - a[2]*b[1],
                a[2]*b[0] - a[0]*b[2],
                a[0]*b[1] - a[1]*b[0]
        };
    }
    /** Pick a perpendicular vector (not unique but always valid) */
    public static double[] pickPerpendicular(double[] v, Random rng) {
        double[] candidate = Math.abs(v[0]) < 0.9 ? new double[]{1,0,0} : new double[]{0,1,0};
        double[] perp = cross(v, candidate);
        double len = Math.sqrt(perp[0]*perp[0] + perp[1]*perp[1] + perp[2]*perp[2]);
        if (len < 1e-6) return new double[]{0,0,1};
        return scale(perp, 1.0/len);
    }
    /** Normalize a vector */
    public static double[] normalize(double[] v) {
        double len = Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
        if (len < 1e-8) return new double[]{0,0,0};
        return new double[]{v[0]/len, v[1]/len, v[2]/len};
    }

    /** Generate offshoot axis, optionally with extra randomization */
    public static double[] randomOffshootAxis(double[] parentAxis, double maxAngle, Random rng) {
        return randomTiltedAxis(parentAxis, maxAngle, rng);
    }
}