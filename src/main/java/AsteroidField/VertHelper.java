package AsteroidField;

import java.util.*;

/**
 * Utility class for mesh vertex and topology helpers.
 */
public class VertHelper {
    public static void validateMesh(List<int[]> allFaces, List<float[]> allVerts) {
        int n = allVerts.size();
        for (int i = 0; i < allFaces.size(); i++) {
            int[] f = allFaces.get(i);
            for (int j = 0; j < 3; j++) {
                if (f[j] < 0 || f[j] >= n) {
                    System.out.println("POSTVALIDATE ERROR: Face " + i + " references invalid vertex " + f[j] + " (n=" + n + ")");
                }
            }
        }
    }
    /**
     * Collects all vertex indices within a given number of hops of a seed vertex,
     * using a BFS over the vertex adjacency map. Includes the seed.
     */
    public static List<Integer> collectNeighborVerts(int seed, int hops, Map<Integer, Set<Integer>> adjacency, Random rng) {
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

    /**
     * Builds a vertex adjacency map from mesh faces.
     * @param faces List of face arrays (each face is int[3])
     * @param numVerts Number of vertices in mesh
     */
    public static Map<Integer, Set<Integer>> buildVertexAdjacency(List<int[]> faces, int numVerts) {
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

    /**
     * For each face, returns a set of face indices which are adjacent (share at least one edge).
     * @param faces List of int[3] face vertex indices.
     * @return List of sets; faceNeighbors.get(i) is a Set<Integer> of neighboring face indices for face i.
     */
    public static List<Set<Integer>> collectFaceNeighbors(List<int[]> faces) {
        // Map from edge (min, max) to all faces that share it
        Map<String, List<Integer>> edgeToFaces = new HashMap<>();
        for (int i = 0; i < faces.size(); i++) {
            int[] face = faces.get(i);
            for (int j = 0; j < 3; j++) {
                int a = face[j], b = face[(j + 1) % 3];
                String key = edgeKey(a, b);
                edgeToFaces.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
            }
        }
        List<Set<Integer>> neighbors = new ArrayList<>(faces.size());
        for (int i = 0; i < faces.size(); i++) neighbors.add(new HashSet<>());
        for (List<Integer> faceList : edgeToFaces.values()) {
            if (faceList.size() > 1) {
                for (int i : faceList) {
                    for (int j : faceList) {
                        if (i != j) neighbors.get(i).add(j);
                    }
                }
            }
        }
        return neighbors;
    }

    private static String edgeKey(int a, int b) {
        return (Math.min(a, b)) + "_" + (Math.max(a, b));
    }

    /**
     * Computes the average normal at a vertex, given all mesh faces and vertex positions.
     * @param vertexIdx The vertex index
     * @param faces List of int[3] mesh faces
     * @param verts List of float[3] vertex positions
     * @return Normalized float[3] normal vector for the vertex
     */
    public static float[] vertexNormalFromFaces(int vertexIdx, List<int[]> faces, List<float[]> verts) {
        double[] normal = new double[3];
        float[] v0 = verts.get(vertexIdx);
        for (int[] face : faces) {
            if (face[0] == vertexIdx || face[1] == vertexIdx || face[2] == vertexIdx) {
                float[] v1 = verts.get(face[0]);
                float[] v2 = verts.get(face[1]);
                float[] v3 = verts.get(face[2]);
                double[] fn = faceNormal(v1, v2, v3);
                normal[0] += fn[0]; normal[1] += fn[1]; normal[2] += fn[2];
            }
        }
        double len = Math.sqrt(normal[0]*normal[0] + normal[1]*normal[1] + normal[2]*normal[2]);
        if (len == 0) return new float[]{0,0,0};
        return new float[]{(float)(normal[0]/len), (float)(normal[1]/len), (float)(normal[2]/len)};
    }

    private static double[] faceNormal(float[] v1, float[] v2, float[] v3) {
        double[] u = new double[]{v2[0] - v1[0], v2[1] - v1[1], v2[2] - v1[2]};
        double[] v = new double[]{v3[0] - v1[0], v3[1] - v1[1], v3[2] - v1[2]};
        return cross(u, v);
    }
    private static double[] cross(double[] a, double[] b) {
        return new double[]{
                a[1]*b[2] - a[2]*b[1],
                a[2]*b[0] - a[0]*b[2],
                a[0]*b[1] - a[1]*b[0]
        };
    }

    /**
     * Finds all border vertices: vertices that are part of at least one border edge
     * (i.e., an edge with only one adjacent face).
     * @param faces List of int[3] mesh faces
     * @return Set of Integer indices of border vertices
     */
    public static Set<Integer> findBorderVertices(List<int[]> faces) {
        Map<String, List<Integer>> edgeToFaces = new HashMap<>();
        for (int i = 0; i < faces.size(); i++) {
            int[] face = faces.get(i);
            for (int j = 0; j < 3; j++) {
                int a = face[j], b = face[(j + 1) % 3];
                String key = edgeKey(a, b);
                edgeToFaces.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
            }
        }
        Set<Integer> borderVerts = new HashSet<>();
        for (Map.Entry<String, List<Integer>> entry : edgeToFaces.entrySet()) {
            if (entry.getValue().size() == 1) {
                // This edge is only in one face → border edge
                String[] ab = entry.getKey().split("_");
                int a = Integer.parseInt(ab[0]);
                int b = Integer.parseInt(ab[1]);
                borderVerts.add(a);
                borderVerts.add(b);
            }
        }
        return borderVerts;
    }
}
