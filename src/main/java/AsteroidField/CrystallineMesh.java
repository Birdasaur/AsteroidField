package AsteroidField;

import java.util.*;

public class CrystallineMesh extends IcosphereMesh {

    public CrystallineMesh(double radius, int subdivisions, CrystallineAsteroidParameters params) {
        super(radius, subdivisions);
        deform(params);
    }

    public void deform(CrystallineAsteroidParameters params) {
        // 1. Apply deformation to base icosphere only
        deformBaseIcosphere(params);

        Random rng = new Random(params.getSeed());
        int crystalCount = Math.max(1, params.getCrystalCount());
        int sides = Math.max(3, params.getPrismSides());
        int maxClusterSize = Math.max(1, params.getMaxClusterSize());
        int neighborRadius = Math.max(1, params.getClusterSpread());
        double baseRadius = params.getRadius();

        // Step 2: Precompute adjacency for neighbor search
        Map<Integer, Set<Integer>> adjacency = buildVertexAdjacency(facesList, vertsList.size());

        // Step 3: Select clusters with spread (from the *deformed* vertsList)
        List<CrystalBase> bases = selectCrystalBasesWithSpread(
            crystalCount, maxClusterSize, neighborRadius, rng, adjacency
        );

        // Step 4: Build mesh
        List<float[]> allVerts = new ArrayList<>(vertsList);
        List<int[]> allFaces = new ArrayList<>(facesList);

        for (CrystalBase base : bases) {
            buildCrystalPrism(
                allVerts, allFaces,
                base.position, base.normal,
                params, sides, rng, baseRadius
            );
        }

        // Step 5: Finalize
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

    // --- Apply deformation to base icosphere vertices only ---
    private void deformBaseIcosphere(CrystallineAsteroidParameters params) {
        double deform = params.getDeformation();
        if (deform == 0) return;
        Random rng = new Random(params.getSeed() ^ 0xF00DF00D);

        for (int i = 0; i < vertsList.size(); i++) {
            float[] v = vertsList.get(i);
            double x = v[0], y = v[1], z = v[2];
            double len = Math.sqrt(x * x + y * y + z * z);
            double vx = x / len, vy = y / len, vz = z / len;

            double deformBump = 1.0 + deform * (rng.nextDouble() - 0.5) * 2.0;
            double newLen = params.getRadius() * deformBump;
            v[0] = (float) (vx * newLen);
            v[1] = (float) (vy * newLen);
            v[2] = (float) (vz * newLen);
        }
        // Sync verts[] array
        for (int i = 0; i < vertsList.size(); i++) {
            float[] v = vertsList.get(i);
            verts[i * 3] = v[0];
            verts[i * 3 + 1] = v[1];
            verts[i * 3 + 2] = v[2];
        }
    }

    /*** --- Data class for crystal bases --- ***/
    private static class CrystalBase {
        float[] position;
        float[] normal;
        CrystalBase(float[] pos, float[] norm) {
            position = pos;
            normal = norm;
        }
    }

    /*** --- Helper: Build vertex adjacency for neighbor search --- ***/
    private Map<Integer, Set<Integer>> buildVertexAdjacency(List<int[]> faces, int numVerts) {
        Map<Integer, Set<Integer>> adj = new HashMap<>();
        for (int i = 0; i < numVerts; i++) adj.put(i, new HashSet<>());
        for (int[] face : faces) {
            for (int j = 0; j < 3; j++) {
                int a = face[j], b = face[(j + 1) % 3];
                adj.get(a).add(b);
                adj.get(b).add(a);
            }
        }
        return adj;
    }

    /*** --- Step 2: Improved cluster base selection (spread over region) --- ***/
    private List<CrystalBase> selectCrystalBasesWithSpread(
            int count, int maxClusterSize, int neighborRadius, Random rng,
            Map<Integer, Set<Integer>> adjacency) {

        List<CrystalBase> bases = new ArrayList<>();
        Set<Integer> usedSeeds = new HashSet<>();
        int nVerts = vertsList.size();

        while (bases.size() < count) {
            // 1. Pick random seed vertex for cluster
            int seedIdx;
            do { seedIdx = rng.nextInt(nVerts); }
            while (usedSeeds.contains(seedIdx) && maxClusterSize == 1);
            usedSeeds.add(seedIdx);

            // 2. Gather neighbor vertices within cluster spread
            List<Integer> clusterVerts = collectNeighborVerts(seedIdx, neighborRadius, adjacency, rng);

            int clusterSize = (maxClusterSize == 1) ? 1 : 1 + rng.nextInt(maxClusterSize);
            for (int k = 0; k < clusterSize && bases.size() < count; k++) {
                int vi = clusterVerts.get(rng.nextInt(clusterVerts.size()));
                float[] v = vertsList.get(vi);
                double len = Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
                float[] n = new float[]{(float)(v[0]/len), (float)(v[1]/len), (float)(v[2]/len)};
                bases.add(new CrystalBase(v.clone(), n));
            }
        }
        return bases;
    }

    // Helper: BFS to get all neighbors within radius hops
    private List<Integer> collectNeighborVerts(int seed, int hops, Map<Integer, Set<Integer>> adjacency, Random rng) {
        Set<Integer> visited = new HashSet<>();
        Queue<Integer> queue = new LinkedList<>();
        visited.add(seed);
        queue.add(seed);
        int depth = 0;
        int sizeThisDepth = 1;

        List<Integer> result = new ArrayList<>();
        result.add(seed);

        while (!queue.isEmpty() && depth < hops) {
            int curr = queue.poll();
            for (int nbr : adjacency.get(curr)) {
                if (!visited.contains(nbr)) {
                    visited.add(nbr);
                    queue.add(nbr);
                    result.add(nbr);
                }
            }
            sizeThisDepth--;
            if (sizeThisDepth == 0) {
                sizeThisDepth = queue.size();
                depth++;
            }
        }
        return result;
    }

    /*** --- Step 3: Build each crystal prism with embedding --- ***/
    private void buildCrystalPrism(
        List<float[]> allVerts, List<int[]> allFaces,
        float[] base, float[] normal,
        CrystallineAsteroidParameters params, int sides, Random rng, double baseRadius
    ) {
        boolean capBase = params.isCapBase();
        boolean capTip = params.isCapTip();
        double tiltAmount = params.getMaxTiltAngleRadians();
        double lengthJitter = params.getLengthJitter();
        double radiusJitter = params.getRadiusJitter();
        double tipRadiusScale = params.getTipRadiusScale();
        double embedDepth = params.getEmbedDepth() * baseRadius;

        // Random tilt
        double[] crystalDir = addRandomTilt(normal, tiltAmount, rng);

        // Sizing
        double minLen = params.getMinCrystalLength() * baseRadius;
        double maxLen = params.getMaxCrystalLength() * baseRadius;
        double minRad = params.getMinCrystalRadius() * baseRadius;
        double maxRad = params.getMaxCrystalRadius() * baseRadius;

        double length = minLen + rng.nextDouble() * (maxLen - minLen);
        length *= (1.0 + (rng.nextDouble() - 0.5) * 2.0 * lengthJitter);

        double radius = minRad + rng.nextDouble() * (maxRad - minRad);
        radius *= (1.0 + (rng.nextDouble() - 0.5) * 2.0 * radiusJitter);

        // Prism orientation
        double[] up = pickPerpendicular(crystalDir, rng);
        double[] right = cross(crystalDir, up);

        double[] centerBase = new double[]{
            base[0] - normal[0] * embedDepth,
            base[1] - normal[1] * embedDepth,
            base[2] - normal[2] * embedDepth
        };
        double[] centerTip = new double[]{
                centerBase[0] + crystalDir[0] * length,
                centerBase[1] + crystalDir[1] * length,
                centerBase[2] + crystalDir[2] * length
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

    // ---- Vector helpers ----
    private static double[] add(double[] a, double[] b) { return new double[]{a[0]+b[0], a[1]+b[1], a[2]+b[2]}; }
    private static double[] scale(double[] v, double s) { return new double[]{v[0]*s, v[1]*s, v[2]*s}; }
    private static double[] cross(double[] a, double[] b) {
        return new double[]{
                a[1]*b[2] - a[2]*b[1],
                a[2]*b[0] - a[0]*b[2],
                a[0]*b[1] - a[1]*b[0]
        };
    }
    private static double[] pickPerpendicular(double[] v, Random rng) {
        double[] candidate = Math.abs(v[0]) < 0.9 ? new double[]{1,0,0} : new double[]{0,1,0};
        double[] perp = cross(v, candidate);
        double len = Math.sqrt(perp[0]*perp[0] + perp[1]*perp[1] + perp[2]*perp[2]);
        if (len < 1e-6) return new double[]{0,0,1};
        return scale(perp, 1.0/len);
    }
    private static double[] addRandomTilt(float[] dir, double maxAngle, Random rng) {
        double[] d = new double[]{dir[0], dir[1], dir[2]};
        double angle = rng.nextDouble() * maxAngle;
        double azimuth = rng.nextDouble() * 2.0 * Math.PI;
        double[] perp = pickPerpendicular(d, rng);
        double[] cross = cross(d, perp);
        double[] tilted = new double[3];
        for (int i = 0; i < 3; i++)
            tilted[i] = d[i]*Math.cos(angle) + perp[i]*Math.sin(angle)*Math.cos(azimuth) + cross[i]*Math.sin(angle)*Math.sin(azimuth);
        double len = Math.sqrt(tilted[0]*tilted[0] + tilted[1]*tilted[1] + tilted[2]*tilted[2]);
        for (int i = 0; i < 3; i++) tilted[i] /= len;
        return tilted;
    }
}