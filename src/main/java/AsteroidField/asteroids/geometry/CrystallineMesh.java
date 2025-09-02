package AsteroidField.asteroids.geometry;

import AsteroidField.asteroids.parameters.CrystallineAsteroidParameters;
import java.util.*;

public class CrystallineMesh extends IcosphereMesh {

public CrystallineMesh(double radius, int subdivisions, CrystallineAsteroidParameters params) {
    super(radius, subdivisions);
    System.out.println("CrystallineMesh CONSTRUCTOR CALLED! Type: " + this.getClass().getSimpleName());
    deform(params);
}

    // Store all texCoords as doubles for better math, but use float[] at end
    public List<double[]> texCoordList = new ArrayList<>();
    public Map<String, Integer> texCoordMap = new HashMap<>(); // "u,v" → index
    // Asteroid base: equirect UVs per base mesh vertex
    public List<Integer> baseVertToTexIdx = new ArrayList<>();
    /**
     * Initializes the texCoordList, texCoordMap, and baseVertToTexIdx for equirectangular UV mapping
     * for every base mesh vertex.
     * <p>
     * Call this at the top of your KryptoniteClusterMesh.deform() before any crystal or face generation.
     * </p>
     */
    protected void setupBaseTexCoords(CrystallineAsteroidParameters params) {
        texCoordList.clear();
        texCoordMap.clear();
        baseVertToTexIdx.clear();

        for (float[] v : vertsList) {
            double len = Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
            double x = v[0]/len, y = v[1]/len, z = v[2]/len;
            double theta = Math.atan2(z, x);
            double phi = Math.acos(y);
            double u = (theta + Math.PI) / (2 * Math.PI);
            double vTex = phi / Math.PI;
            int tIdx = addOrGetTexCoord(u, vTex);
            baseVertToTexIdx.add(tIdx);
        }
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

        // --- 1. UV Setup for asteroid base
        setupBaseTexCoords(params);

        // --- 2. Generate mesh geometry and faces
        List<float[]> allVerts = new ArrayList<>(vertsList);   // All positions
        List<FaceRef> allFaceRefs = new ArrayList<>();         // All faces with vert/tex indices

        // Add base mesh faces (asteroid only)
        for (int[] face : facesList) {
            int vi0 = face[0], vi1 = face[1], vi2 = face[2];
            int ti0 = baseVertToTexIdx.get(vi0), ti1 = baseVertToTexIdx.get(vi1), ti2 = baseVertToTexIdx.get(vi2);
            allFaceRefs.add(new FaceRef(vi0, ti0, vi1, ti1, vi2, ti2));
        }

        // --- 3. Add crystal geometry (vertices, faces, and texCoords)
        Map<Integer, Set<Integer>> adjacency = VertHelper.buildVertexAdjacency(facesList, vertsList.size());
        List<CrystalBase> bases = selectCrystalBasesWithSpread(crystalCount, maxClusterSize, neighborRadius, rng, adjacency);

        int paletteRows = 12; // Number of palette color rows (V resolution)
        double paletteU = 0.0; // U=0 = left edge of texture (palette column)
        int crystalCounter = 0;

        for (CrystalBase base : bases) {
            crystalCounter++;
            // Pick a palette row for this crystal
            int paletteIdx = (crystalCounter-1) % paletteRows;
            double paletteV = (paletteIdx + 0.5) / paletteRows;

            createCrystalWithUVs(
                allVerts, allFaceRefs, base.position, base.normal, params, baseRadius, rng, 0,
                paletteU, paletteV, paletteRows, baseVertToTexIdx
            );
        }

        // --- 4. Write mesh data (verts, texCoords, faces) to TriangleMesh
        float[] newVerts = new float[allVerts.size() * 3];
        for (int i = 0; i < allVerts.size(); i++) {
            float[] v = allVerts.get(i);
            newVerts[i * 3] = v[0];
            newVerts[i * 3 + 1] = v[1];
            newVerts[i * 3 + 2] = v[2];
        }
        float[] newTexCoords = new float[texCoordList.size() * 2];
        for (int i = 0; i < texCoordList.size(); i++) {
            double[] uv = texCoordList.get(i);
            newTexCoords[i * 2] = (float)uv[0];
            newTexCoords[i * 2 + 1] = (float)uv[1];
        }
        int[] newFaces = new int[allFaceRefs.size() * 6];
        for (int i = 0; i < allFaceRefs.size(); i++) {
            FaceRef f = allFaceRefs.get(i);
            newFaces[i * 6 + 0] = f.vi0;
            newFaces[i * 6 + 1] = f.ti0;
            newFaces[i * 6 + 2] = f.vi1;
            newFaces[i * 6 + 3] = f.ti1;
            newFaces[i * 6 + 4] = f.vi2;
            newFaces[i * 6 + 5] = f.ti2;
        }

        getPoints().setAll(newVerts);
        getTexCoords().setAll(newTexCoords);
        getFaces().setAll(newFaces);
        getFaceSmoothingGroups().clear();
        for (int i = 0; i < newFaces.length / 6; i++) getFaceSmoothingGroups().addAll(1);
    }

    // Add or return texCoord index for (u,v)
    public int addOrGetTexCoord(double u, double v) {
        String key = String.format("%.5f,%.5f", u, v);
        Integer idx = texCoordMap.get(key);
        if (idx != null) return idx;
        int newIdx = texCoordList.size();
        texCoordList.add(new double[]{u, v});
        texCoordMap.put(key, newIdx);
        return newIdx;
    }

    // Helper for crystal generation with proper UV assignment
    public void createCrystalWithUVs(
            List<float[]> allVerts, List<FaceRef> allFaceRefs,
            float[] base, float[] normal,
            CrystallineAsteroidParameters params, double baseRadius, Random rng, int recursionDepth,
            double paletteU, double paletteV, int paletteRows, List<Integer> baseVertToTexIdx
    ) {
        // --- Geometry code as before ---
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

        double[] axis = CrystalHelper.randomTiltedAxis(
                CrystalHelper.floatToDouble(normal), tiltAmount, rng);

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

        double[] up = CrystalHelper.pickPerpendicular(axis, rng);
        double[] right = CrystalHelper.cross(axis, up);

        List<float[]> baseRing = CrystalHelper.generateFacetedRing(centerBase, axis, up, right, sides, radius, facetJitter, rng);
        if (params.getTwistAmount() > 1e-6)
            CrystalHelper.applyTwistToRing(baseRing, params.getTwistAmount(), axis, centerBase, rng);

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

        // Assign UVs
        // 1. Base UV: match nearest asteroid base vertex UV
        int closestBaseIdx = 0;
        double bestDist = Double.MAX_VALUE;
        for (int i = 0; i < vertsList.size(); i++) {
            float[] v = vertsList.get(i);
            double dx = v[0] - base[0], dy = v[1] - base[1], dz = v[2] - base[2];
            double dist = dx*dx + dy*dy + dz*dz;
            if (dist < bestDist) { bestDist = dist; closestBaseIdx = i; }
        }
        int baseUVidx = baseVertToTexIdx.get(closestBaseIdx);

        // 2. Palette UV: assign for sides/tip
        int sideUVidx = addOrGetTexCoord(paletteU, paletteV);

        // --- Add verts and UV indices for all crystal geometry
        int baseStart = allVerts.size();
        List<Integer> baseUVs = new ArrayList<>();
        for (int i = 0; i < baseRing.size(); i++) {
            allVerts.add(baseRing.get(i));
            baseUVs.add(baseUVidx); // root uses base mesh UV
        }
        int tipStart = allVerts.size();
        List<Integer> tipUVs = new ArrayList<>();
        for (int i = 0; i < tipRing.size(); i++) {
            allVerts.add(tipRing.get(i));
            tipUVs.add(sideUVidx); // sides/tip use palette UV
        }

        boolean capBase = params.isCapBase();
        boolean capTip = params.isCapTip();

        if (pointy && pointyOk && baseOk) {
            int tipIdx = tipStart;
            int tipUV = sideUVidx;
            for (int s = 0; s < baseRingSize; s++) {
                int a = baseStart + s, b = baseStart + (s + 1) % baseRingSize;
                int aUV = baseUVs.get(s), bUV = baseUVs.get((s + 1) % baseRingSize);
                allFaceRefs.add(new FaceRef(a, aUV, b, bUV, tipIdx, tipUV));
            }
        } else if (baseOk && tipOk && baseRingSize == tipRingSize) {
            for (int s = 0; s < baseRingSize; s++) {
                int a = baseStart + s, b = baseStart + (s + 1) % baseRingSize;
                int c = tipStart + s, d = tipStart + (s + 1) % tipRingSize;
                int aUV = baseUVs.get(s), bUV = baseUVs.get((s + 1) % baseRingSize);
                int cUV = tipUVs.get(s), dUV = tipUVs.get((s + 1) % tipRingSize);
                allFaceRefs.add(new FaceRef(a, aUV, b, bUV, d, dUV));
                allFaceRefs.add(new FaceRef(a, aUV, d, dUV, c, cUV));
            }
        } else {
            System.out.println("Warning: Not generating faces, ring size mismatch: baseRing=" + baseRingSize +
                    ", tipRing=" + tipRingSize + ", sides=" + sides + ", pointy=" + pointy);
        }

        // Cap base (use asteroid base UV for center)
        if (capBase && baseOk) {
            int centerBaseIdx = allVerts.size();
            allVerts.add(CrystalHelper.centerAsFloat(centerBase));
            int centerBaseUV = baseUVidx;
            for (int s = 0; s < baseRingSize; s++) {
                int a = baseStart + s, b = baseStart + (s + 1) % baseRingSize;
                int aUV = baseUVs.get(s), bUV = baseUVs.get((s + 1) % baseRingSize);
                //allFaceRefs.add(new FaceRef(centerBaseIdx, centerBaseUV, b, bUV, a, aUV));
                allFaceRefs.add(new FaceRef(centerBaseIdx, centerBaseUV, a, aUV, b, bUV));
            }
        }

        // Cap tip (palette UV)
        if (capTip && !pointy && tipOk) {
            int centerTipIdx = allVerts.size();
            allVerts.add(CrystalHelper.centerAsFloat(centerTip));
            int centerTipUV = sideUVidx;
            for (int s = 0; s < tipRingSize; s++) {
                int a = tipStart + s, b = tipStart + (s + 1) % tipRingSize;
                int aUV = tipUVs.get(s), bUV = tipUVs.get((s + 1) % tipRingSize);
                //allFaceRefs.add(new FaceRef(centerTipIdx, centerTipUV, a, aUV, b, bUV));
                allFaceRefs.add(new FaceRef(centerTipIdx, centerTipUV, b, bUV, a, aUV));
            }
        }

        // Recursively generate offshoots, if any
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
            createCrystalWithUVs(allVerts, allFaceRefs,
                    CrystalHelper.doubleToFloat(offshootBase), CrystalHelper.doubleToFloat(offshootAxis),
                    childParams, baseRadius, rng, recursionDepth + 1,
                    paletteU, paletteV, paletteRows, baseVertToTexIdx
            );
        }
    }

    // Adjacency, cluster base selection, deformBaseIcosphere, etc (unchanged)
    public void deformBaseIcosphere(CrystallineAsteroidParameters params) {
        double deform = params.getDeformation();
        if (deform == 0) return;
        Random rng = new Random(params.getSeed() ^ 0xF00DF00D);
for (int i = 0; i < vertsList.size(); i++) {
    float[] v = vertsList.get(i);
    double len = Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
    double vx = v[0] / len, vy = v[1] / len, vz = v[2] / len;
    double latitude = Math.asin(vy); // -PI/2 (south pole) to +PI/2 (north pole)
    double poleFalloff = Math.pow(Math.cos(latitude), 0.5); // Stronger falloff near poles

    double deformBump = 1.0 + deform * poleFalloff * (rng.nextDouble() - 0.5) * 2.0;
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
