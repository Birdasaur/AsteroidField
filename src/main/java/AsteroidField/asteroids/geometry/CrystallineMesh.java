package AsteroidField.asteroids.geometry;

import AsteroidField.asteroids.parameters.CrystallineAsteroidParameters;
import java.util.*;

public class CrystallineMesh extends IcosphereMesh {

    public CrystallineMesh(double radius, int subdivisions, CrystallineAsteroidParameters params) {
        super(radius, subdivisions);
        deform(params);
    }

    public void deform(CrystallineAsteroidParameters params) {
        deformBaseIcosphere(params);
        Random rng = new Random(params.getSeed());
        int crystalCount = Math.max(1, params.getCrystalCount());
        int maxClusterSize = Math.max(1, params.getMaxClusterSize());
        int neighborRadius = Math.max(1, params.getClusterSpread());
        double baseRadius = params.getRadius();

        int nVerts = vertsList.size();
        if (crystalCount > nVerts) {
            System.out.println("Warning: Requested " + crystalCount + " crystals but only " + nVerts + " unique vertices available. Capping count.");
            crystalCount = nVerts;
        }

        Map<Integer, Set<Integer>> adjacency = VertHelper.buildVertexAdjacency(facesList, vertsList.size());
        List<CrystalBase> bases = selectCrystalBasesWithSpread(crystalCount, maxClusterSize, neighborRadius, rng, adjacency);

        List<float[]> allVerts = new ArrayList<>(vertsList);
        List<int[]> allFaces = new ArrayList<>(facesList);

        for (CrystalBase base : bases) {
            createCrystalWithOffshoots(
                allVerts, allFaces, base.position, base.normal, params, baseRadius, rng, 0
            );
        }

        VertHelper.validateMesh(allFaces, allVerts);

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

    private void addFaceSafe(List<int[]> allFaces, List<float[]> allVerts, int a, int b, int c, String note) {
        int n = allVerts.size();
        if (a < 0 || a >= n || b < 0 || b >= n || c < 0 || c >= n) {
            System.out.println("ERROR: Face out of bounds: [" + a + "," + b + "," + c + "] with verts=" + n + " (" + note + ")");
        } else {
            allFaces.add(new int[]{a, b, c});
        }
    }

    private void createCrystalWithOffshoots(
            List<float[]> allVerts, List<int[]> allFaces,
            float[] base, float[] normal,
            CrystallineAsteroidParameters params, double baseRadius, Random rng, int recursionDepth)
    {
        // --- 1. Feature selection and sizing ---
        int prismSides = Math.max(3, params.getPrismSides());
        int minSides = prismSides - 2;
        int maxSides = prismSides + 2;
        int sides = CrystalHelper.randomizePrismSides(
                Math.max(3, minSides),
                Math.max(3, maxSides),
                rng);

        double tiltAmount = params.getMaxTiltAngleRadians();
        double facetJitter = params.getFacetJitter();
        double tipRadiusScale = params.getTipRadiusScale();
        double embedDepth = params.getEmbedDepth() * baseRadius;
        double lengthJitter = params.getLengthJitter();
        double radiusJitter = params.getRadiusJitter();

        double minLen = params.getMinCrystalLength() * baseRadius;
        double maxLen = params.getMaxCrystalLength() * baseRadius;
        double minRad = params.getMinCrystalRadius() * baseRadius;
        double maxRad = params.getMaxCrystalRadius() * baseRadius;

        double length = minLen + rng.nextDouble() * (maxLen - minLen);
        length *= (1.0 + (rng.nextDouble() - 0.5) * 2.0 * lengthJitter);
        double radius = minRad + rng.nextDouble() * (maxRad - minRad);
        radius *= (1.0 + (rng.nextDouble() - 0.5) * 2.0 * radiusJitter);

        // Axis, orientation
        double[] axis = CrystalHelper.randomTiltedAxis(
                CrystalHelper.floatToDouble(normal), tiltAmount, rng);

        // Base and tip centers
        double[] centerBase = new double[]{
                base[0] - normal[0] * embedDepth,
                base[1] - normal[1] * embedDepth,
                base[2] - normal[2] * embedDepth
        };
        double[] centerTip = new double[]{
                centerBase[0] + axis[0] * length,
                centerBase[1] + axis[1] * length,
                centerBase[2] + axis[2] * length
        };

        // Up/right for rings
        double[] up = CrystalHelper.pickPerpendicular(axis, rng);
        double[] right = CrystalHelper.cross(axis, up);

        // Base ring: faceted, jittered, possibly twisted
        List<float[]> baseRing = CrystalHelper.generateFacetedRing(centerBase, axis, up, right, sides, radius, facetJitter, rng);
        if (params.getTwistAmount() > 1e-6)
            CrystalHelper.applyTwistToRing(baseRing, params.getTwistAmount(), axis, centerBase, rng);

        // Tip: pointy, beveled, or normal ring
        boolean pointy = rng.nextDouble() < params.getPointyTipChance();
        boolean beveled = !pointy && (rng.nextDouble() < params.getBevelTipChance());
        List<float[]> tipRing = CrystalHelper.generateTip(centerTip, axis, up, right, sides, radius, tipRadiusScale, pointy, beveled, params.getBevelDepth(), length, rng);

        int baseRingSize = baseRing.size();
        int tipRingSize = tipRing.size();
        boolean baseOk = (baseRingSize >= 3);
        boolean tipOk = (tipRingSize >= 3);
        boolean pointyOk = (tipRingSize == 1);

        if (!baseOk || (!pointy && !tipOk) || (pointy && !pointyOk)) {
            System.out.println("Skipping degenerate crystal: baseRing.size()=" + baseRingSize +
                    ", tipRing.size()=" + tipRingSize + ", sides=" + sides + ", pointy=" + pointy);
            return;
        }

        // Chisel notch
        if (rng.nextDouble() < 0.22 && baseRingSize >= 3) {
            CrystalHelper.addChiselNotch(baseRing, 0.13 * radius, 1, rng);
        }
        if (!pointy && (rng.nextDouble() < 0.15) && tipRingSize >= 3) {
            CrystalHelper.addChiselNotch(tipRing, -0.13 * radius, 1, rng);
        }

        // Fracture plane (randomly applied to tip)
        if (rng.nextDouble() < params.getFractureChance() && tipRingSize > 0) {
            double[] fractureNormal = CrystalHelper.randomTiltedAxis(axis, Math.toRadians(45), rng);
            double[] fracturePoint = centerTip;
            double fractureDepth = params.getFractureDepth() * baseRadius;
            CrystalHelper.generateFracturePlane(tipRing, fractureNormal, fracturePoint, fractureDepth);
        }

        // --- 2. Add all vertices for this crystal ---
        int baseStart = allVerts.size();
        for (float[] v : baseRing) allVerts.add(v);
        int tipStart = allVerts.size();
        for (float[] v : tipRing) allVerts.add(v);

        boolean capBase = params.isCapBase();
        boolean capTip = params.isCapTip();

        // --- 3. Add all faces for this crystal using actual ring sizes ---
        if (pointy && pointyOk && baseOk) {
            // Pointy tip: all base verts to single tip vert
            int tipIdx = tipStart;
//            System.out.println("Adding pointy tip: baseStart=" + baseStart + ", tipIdx=" + tipIdx +
//                    ", baseRing.size=" + baseRingSize + ", sides=" + sides +
//                    ", allVerts.size=" + allVerts.size());
            for (int s = 0; s < baseRingSize; s++) {
                int a = baseStart + s;
                int b = baseStart + (s + 1) % baseRingSize;
                addFaceSafe(allFaces, allVerts, a, b, tipIdx, "pointyTip");
            }
        } else if (baseOk && tipOk && baseRingSize == tipRingSize) {
            // Sides
            for (int s = 0; s < baseRingSize; s++) {
                int a = baseStart + s;
                int b = baseStart + (s + 1) % baseRingSize;
                int c = tipStart + s;
                int d = tipStart + (s + 1) % tipRingSize;
                addFaceSafe(allFaces, allVerts, a, b, d, "side1");
                addFaceSafe(allFaces, allVerts, a, d, c, "side2");
            }
        } else {
            System.out.println("Warning: Not generating faces, ring size mismatch: baseRing=" + baseRingSize +
                    ", tipRing=" + tipRingSize + ", sides=" + sides + ", pointy=" + pointy);
        }

        // Cap base
        if (capBase && baseOk) {
            int centerBaseIdx = allVerts.size();
            allVerts.add(CrystalHelper.centerAsFloat(centerBase));
            for (int s = 0; s < baseRingSize; s++) {
                int a = baseStart + s;
                int b = baseStart + (s + 1) % baseRingSize;
                addFaceSafe(allFaces, allVerts, centerBaseIdx, b, a, "baseCap");
            }
        }

        // Cap tip (normal or beveled)
        if (capTip && !pointy && tipOk) {
            int centerTipIdx = allVerts.size();
            allVerts.add(CrystalHelper.centerAsFloat(centerTip));
            for (int s = 0; s < tipRingSize; s++) {
                int a = tipStart + s;
                int b = tipStart + (s + 1) % tipRingSize;
                addFaceSafe(allFaces, allVerts, centerTipIdx, a, b, "tipCap");
            }
        }

        // --- 4. Recursively generate offshoots, if any ---
        if (recursionDepth < params.getOffshootRecursion() && rng.nextDouble() < params.getOffshootChance()) {
            double t = 0.3 + rng.nextDouble() * 0.55;
            double[] offshootBase = new double[]{
                    centerBase[0] + axis[0] * length * t,
                    centerBase[1] + axis[1] * length * t,
                    centerBase[2] + axis[2] * length * t
            };
            double[] offshootAxis = CrystalHelper.randomOffshootAxis(axis, Math.toRadians(45), rng);
            double offshootScale = params.getOffshootScale();
            CrystallineAsteroidParameters childParams = params.toBuilder()
                    .minCrystalLength(params.getMinCrystalLength() * offshootScale)
                    .maxCrystalLength(params.getMaxCrystalLength() * offshootScale)
                    .minCrystalRadius(params.getMinCrystalRadius() * offshootScale)
                    .maxCrystalRadius(params.getMaxCrystalRadius() * offshootScale)
                    .offshootRecursion(params.getOffshootRecursion() - 1)
                    .build();
            createCrystalWithOffshoots(allVerts, allFaces,
                    CrystalHelper.doubleToFloat(offshootBase), CrystalHelper.doubleToFloat(offshootAxis),
                    childParams, baseRadius, rng, recursionDepth + 1
            );
        }
    }

    // Adjacency, cluster base selection, deformBaseIcosphere, etc
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
        for (int i = 0; i < vertsList.size(); i++) {
            float[] v = vertsList.get(i);
            verts[i * 3] = v[0];
            verts[i * 3 + 1] = v[1];
            verts[i * 3 + 2] = v[2];
        }
    }

    private static class CrystalBase {
        float[] position;
        float[] normal;
        CrystalBase(float[] pos, float[] norm) {
            position = pos;
            normal = norm;
        }
    }

    private List<CrystalBase> selectCrystalBasesWithSpread(
            int count, int maxClusterSize, int neighborRadius, Random rng,
            Map<Integer, Set<Integer>> adjacency) {
        List<CrystalBase> bases = new ArrayList<>();
        Set<Integer> usedSeeds = new HashSet<>();
        int nVerts = vertsList.size();

        while (bases.size() < count) {
            int seedIdx;
            do { seedIdx = rng.nextInt(nVerts); }
            while (usedSeeds.contains(seedIdx) && maxClusterSize == 1);
            usedSeeds.add(seedIdx);
            List<Integer> clusterVerts = VertHelper.collectNeighborVerts(seedIdx, neighborRadius, adjacency, rng);
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
}
