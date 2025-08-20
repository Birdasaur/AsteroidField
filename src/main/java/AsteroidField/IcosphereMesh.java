package AsteroidField;

import java.util.*;

public class IcosphereMesh {
    private final float[] vertices;
    private final int[] faces;

    public IcosphereMesh(double radius, int subdivisions) {
        // Golden ratio for icosahedron
        double t = (1.0 + Math.sqrt(5.0)) / 2.0;

        // Create base icosahedron points
        List<double[]> verts = new ArrayList<>(12);
        verts.add(new double[]{-1,  t,  0});
        verts.add(new double[]{ 1,  t,  0});
        verts.add(new double[]{-1, -t,  0});
        verts.add(new double[]{ 1, -t,  0});

        verts.add(new double[]{ 0, -1,  t});
        verts.add(new double[]{ 0,  1,  t});
        verts.add(new double[]{ 0, -1, -t});
        verts.add(new double[]{ 0,  1, -t});

        verts.add(new double[]{ t,  0, -1});
        verts.add(new double[]{ t,  0,  1});
        verts.add(new double[]{-t,  0, -1});
        verts.add(new double[]{-t,  0,  1});

        // Normalize to sphere of given radius
        for (double[] v : verts) {
            double len = Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
            v[0] = v[0] / len * radius;
            v[1] = v[1] / len * radius;
            v[2] = v[2] / len * radius;
        }

        // Base icosahedron faces
        int[][] facesData = {
            {0,11,5}, {0,5,1}, {0,1,7}, {0,7,10}, {0,10,11},
            {1,5,9}, {5,11,4}, {11,10,2}, {10,7,6}, {7,1,8},
            {3,9,4}, {3,4,2}, {3,2,6}, {3,6,8}, {3,8,9},
            {4,9,5}, {2,4,11}, {6,2,10}, {8,6,7}, {9,8,1}
        };

        // Subdivide faces
        Map<Long, Integer> middlePointCache = new HashMap<>();
        List<int[]> facesList = new ArrayList<>();
        for (int[] f : facesData) facesList.add(f);

        for (int i = 0; i < subdivisions; i++) {
            List<int[]> faces2 = new ArrayList<>();
            for (int[] tri : facesList) {
                // Replace triangle by 4 triangles
                int a = tri[0];
                int b = tri[1];
                int c = tri[2];
                int ab = getMiddlePoint(a, b, verts, middlePointCache, radius);
                int bc = getMiddlePoint(b, c, verts, middlePointCache, radius);
                int ca = getMiddlePoint(c, a, verts, middlePointCache, radius);

                faces2.add(new int[]{a, ab, ca});
                faces2.add(new int[]{b, bc, ab});
                faces2.add(new int[]{c, ca, bc});
                faces2.add(new int[]{ab, bc, ca});
            }
            facesList = faces2;
        }

        // Convert List<double[]> verts to float[]
        float[] vertArray = new float[verts.size() * 3];
        for (int i = 0; i < verts.size(); i++) {
            vertArray[3*i  ] = (float)verts.get(i)[0];
            vertArray[3*i+1] = (float)verts.get(i)[1];
            vertArray[3*i+2] = (float)verts.get(i)[2];
        }

        // Convert faces to int[]
        int[] faceArray = new int[facesList.size() * 6];
        for (int i = 0; i < facesList.size(); i++) {
            int[] tri = facesList.get(i);
            // For JavaFX: v0/t0 v1/t1 v2/t2 for each triangle (t0 always 0 for now)
            faceArray[i*6  ] = tri[0]; faceArray[i*6+1] = 0;
            faceArray[i*6+2] = tri[1]; faceArray[i*6+3] = 0;
            faceArray[i*6+4] = tri[2]; faceArray[i*6+5] = 0;
        }

        this.vertices = vertArray;
        this.faces = faceArray;
    }

    // Map key for middle points (edge key)
    private static long getEdgeKey(int a, int b) {
        return (long)Math.min(a, b) << 32 | Math.max(a, b);
    }

    // Get/create midpoint index in verts list
    private static int getMiddlePoint(int a, int b, List<double[]> verts, Map<Long, Integer> cache, double radius) {
        long key = getEdgeKey(a, b);
        if (cache.containsKey(key)) return cache.get(key);

        double[] va = verts.get(a);
        double[] vb = verts.get(b);
        double[] vm = new double[]{
            (va[0] + vb[0]) / 2.0,
            (va[1] + vb[1]) / 2.0,
            (va[2] + vb[2]) / 2.0
        };
        // Normalize to sphere
        double len = Math.sqrt(vm[0]*vm[0] + vm[1]*vm[1] + vm[2]*vm[2]);
        vm[0] = vm[0] / len * radius;
        vm[1] = vm[1] / len * radius;
        vm[2] = vm[2] / len * radius;
        verts.add(vm);
        int idx = verts.size() - 1;
        cache.put(key, idx);
        return idx;
    }

    public float[] getVertices() { return vertices; }
    public int[] getFaces() { return faces; }
}
