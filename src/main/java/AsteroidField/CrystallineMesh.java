package AsteroidField;

import java.util.*;
import javafx.scene.shape.TriangleMesh;

public class CrystallineMesh extends IcosphereMesh {

    public CrystallineMesh(double radius, int subdivisions, CrystallineAsteroidParameters params) {
        super(radius, subdivisions);
        deform(params);
    }

    public void deform(CrystallineAsteroidParameters params) {
        Random rng = new Random(params.getSeed());
        int crystalCount = Math.max(1, params.getCrystalCount());
        int sides = Math.max(3, params.getPrismSides());

        // Extracted: Step 1 – select base points and normals (with clustering)
        List<CrystalBase> bases = selectCrystalBases(crystalCount, params.getMaxClusterSize(), rng);

        // Step 2 – mesh lists
        List<float[]> allVerts = new ArrayList<>(vertsList);
        List<int[]> allFaces = new ArrayList<>(facesList);

        // Step 3 – for each base, build crystal prism
        for (CrystalBase base : bases) {
            buildCrystalPrism(
                allVerts, allFaces,
                base.position, base.normal,
                params, sides, rng
            );
        }

        // Step 4 – finalize mesh
        float[] newVerts = new float[allVerts.size() * 3];
        for (int i = 0; i < allVerts.size(); i++) {
            float[] v = allVerts.get(i);
            newVerts[i * 3] = v[0];
            newVerts[i * 3 + 1] = v[1];
            newVerts[i * 3 + 2] = v[2];
        }
        int[] newFaces = new int[allFaces.size() * 6];
        for (int i = 0; i < allFaces.size(); i++) {
            int[] f = allFaces.get(i);
            newFaces[i * 6] = f[0]; newFaces[i * 6 + 1] = 0;
            newFaces[i * 6 + 2] = f[1]; newFaces[i * 6 + 3] = 0;
            newFaces[i * 6 + 4] = f[2]; newFaces[i * 6 + 5] = 0;
        }

        getPoints().setAll(newVerts);
        getFaces().setAll(newFaces);
        getFaceSmoothingGroups().clear();
        for (int i = 0; i < newFaces.length / 6; i++) getFaceSmoothingGroups().addAll(1);
    }

    /*** --- Helper data class for base position/normal --- ***/
    private static class CrystalBase {
        float[] position;
        float[] normal;
        CrystalBase(float[] pos, float[] norm) {
            position = pos;
            normal = norm;
        }
    }

    /*** --- Extracted: Step 1: Crystal base selection with clustering --- ***/
    private List<CrystalBase> selectCrystalBases(int count, int maxClusterSize, Random rng) {
        List<CrystalBase> bases = new ArrayList<>();
        Set<Integer> used = new HashSet<>();
        int coreVerts = vertsList.size();
        while (bases.size() < count) {
            int idx = rng.nextInt(coreVerts);
            if (used.contains(idx) && maxClusterSize == 1) continue;
            float[] v = vertsList.get(idx);
            double len = Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
            float[] n = new float[]{(float)(v[0]/len), (float)(v[1]/len), (float)(v[2]/len)};
            int clusterSize = (maxClusterSize == 1) ? 1 : 1 + rng.nextInt(maxClusterSize);
            for (int k = 0; k < clusterSize && bases.size() < count; k++) {
                bases.add(new CrystalBase(v.clone(), n.clone()));
            }
            used.add(idx);
        }
        return bases;
    }

    /*** --- Extracted: Step 3: Build crystal prism mesh --- ***/
    private void buildCrystalPrism(
        List<float[]> allVerts, List<int[]> allFaces,
        float[] base, float[] normal,
        CrystallineAsteroidParameters params, int sides, Random rng
    ) {
        boolean capBase = params.isCapBase();
        boolean capTip = params.isCapTip();
        double tiltAmount = params.getMaxTiltAngleRadians();
        double lengthJitter = params.getLengthJitter();
        double radiusJitter = params.getRadiusJitter();
        double tipRadiusScale = params.getTipRadiusScale();

        // Random tilt
        double[] crystalDir = addRandomTilt(normal, tiltAmount, rng);

        // Randomized length/radius
        double length = params.getMinCrystalLength() +
                rng.nextDouble() * (params.getMaxCrystalLength() - params.getMinCrystalLength());
        length *= (1.0 + (rng.nextDouble() - 0.5) * 2.0 * lengthJitter);

        double radius = params.getMinCrystalRadius() +
                rng.nextDouble() * (params.getMaxCrystalRadius() - params.getMinCrystalRadius());
        radius *= (1.0 + (rng.nextDouble() - 0.5) * 2.0 * radiusJitter);

        // Generate local basis for prism cross-section
        double[] up = pickPerpendicular(crystalDir, rng);
        double[] right = cross(crystalDir, up);

        double[] centerBase = new double[]{base[0], base[1], base[2]};
        double[] centerTip = new double[]{
                base[0] + crystalDir[0] * length,
                base[1] + crystalDir[1] * length,
                base[2] + crystalDir[2] * length
        };

        int baseStart = allVerts.size();
        int tipStart = baseStart + sides;

        // Add base ring verts
        for (int s = 0; s < sides; s++) {
            double theta = 2 * Math.PI * s / sides;
            double[] dir = add(scale(up, Math.cos(theta)), scale(right, Math.sin(theta)));
            float[] pt = new float[]{
                    (float) (centerBase[0] + dir[0] * radius),
                    (float) (centerBase[1] + dir[1] * radius),
                    (float) (centerBase[2] + dir[2] * radius)
            };
            allVerts.add(pt);
        }
        // Add tip ring verts
        for (int s = 0; s < sides; s++) {
            double theta = 2 * Math.PI * s / sides;
            double[] dir = add(scale(up, Math.cos(theta)), scale(right, Math.sin(theta)));
            float[] pt = new float[]{
                    (float) (centerTip[0] + dir[0] * radius * tipRadiusScale),
                    (float) (centerTip[1] + dir[1] * radius * tipRadiusScale),
                    (float) (centerTip[2] + dir[2] * radius * tipRadiusScale)
            };
            allVerts.add(pt);
        }
        // Add faces for sides
        for (int s = 0; s < sides; s++) {
            int a = baseStart + s;
            int b = baseStart + (s + 1) % sides;
            int c = tipStart + s;
            int d = tipStart + (s + 1) % sides;
            allFaces.add(new int[]{a, b, d});
            allFaces.add(new int[]{a, d, c});
        }
        // Cap base if enabled
        if (capBase) {
            int centerBaseIdx = allVerts.size();
            allVerts.add(new float[]{(float)centerBase[0], (float)centerBase[1], (float)centerBase[2]});
            for (int s = 0; s < sides; s++) {
                int a = baseStart + s;
                int b = baseStart + (s + 1) % sides;
                allFaces.add(new int[]{centerBaseIdx, b, a});
            }
        }
        // Cap tip if enabled
        if (capTip) {
            int centerTipIdx = allVerts.size();
            allVerts.add(new float[]{(float)centerTip[0], (float)centerTip[1], (float)centerTip[2]});
            for (int s = 0; s < sides; s++) {
                int a = tipStart + s;
                int b = tipStart + (s + 1) % sides;
                allFaces.add(new int[]{centerTipIdx, a, b});
            }
        }
    }

    // ---- Helper vector math ----
    private static double[] add(double[] a, double[] b) { return new double[]{a[0] + b[0], a[1] + b[1], a[2] + b[2]}; }
    private static double[] scale(double[] v, double s) { return new double[]{v[0] * s, v[1] * s, v[2] * s}; }
    private static double[] cross(double[] a, double[] b) {
        return new double[]{
                a[1] * b[2] - a[2] * b[1],
                a[2] * b[0] - a[0] * b[2],
                a[0] * b[1] - a[1] * b[0]
        };
    }
    private static double[] pickPerpendicular(double[] v, Random rng) {
        double[] candidate = Math.abs(v[0]) < 0.9 ? new double[]{1, 0, 0} : new double[]{0, 1, 0};
        double[] perp = cross(v, candidate);
        double len = Math.sqrt(perp[0] * perp[0] + perp[1] * perp[1] + perp[2] * perp[2]);
        if (len < 1e-6) return new double[]{0, 0, 1};
        return scale(perp, 1.0 / len);
    }
    private static double[] addRandomTilt(float[] dir, double maxAngle, Random rng) {
        double[] d = new double[]{dir[0], dir[1], dir[2]};
        double angle = rng.nextDouble() * maxAngle;
        double azimuth = rng.nextDouble() * 2.0 * Math.PI;
        double[] perp = pickPerpendicular(d, rng);
        double[] cross = cross(d, perp);
        double[] tilted = new double[3];
        for (int i = 0; i < 3; i++)
            tilted[i] = d[i] * Math.cos(angle) +
                        perp[i] * Math.sin(angle) * Math.cos(azimuth) +
                        cross[i] * Math.sin(angle) * Math.sin(azimuth);
        double len = Math.sqrt(tilted[0] * tilted[0] + tilted[1] * tilted[1] + tilted[2] * tilted[2]);
        for (int i = 0; i < 3; i++) tilted[i] /= len;
        return tilted;
    }
}