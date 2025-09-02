package AsteroidField.asteroids.geometry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.shape.TriangleMesh;

public class IcosphereMesh extends TriangleMesh {
    protected final float[] verts;
    protected final List<float[]> vertsList;
    protected final int[] faces;
    protected final List<int[]> facesList;
    public final double radius;
    public final int subdivisions;

    public IcosphereMesh(double radius, int subdivisions) {
        super();
        this.radius = radius;
        this.subdivisions = subdivisions;

        double t = (1.0 + Math.sqrt(5.0)) / 2.0;

        List<double[]> vertsTmp = new ArrayList<>(12);
        vertsTmp.add(new double[]{-1,  t,  0});
        vertsTmp.add(new double[]{ 1,  t,  0});
        vertsTmp.add(new double[]{-1, -t,  0});
        vertsTmp.add(new double[]{ 1, -t,  0});
        vertsTmp.add(new double[]{ 0, -1,  t});
        vertsTmp.add(new double[]{ 0,  1,  t});
        vertsTmp.add(new double[]{ 0, -1, -t});
        vertsTmp.add(new double[]{ 0,  1, -t});
        vertsTmp.add(new double[]{ t,  0, -1});
        vertsTmp.add(new double[]{ t,  0,  1});
        vertsTmp.add(new double[]{-t,  0, -1});
        vertsTmp.add(new double[]{-t,  0,  1});

        // Normalize to sphere
        for (double[] v : vertsTmp) {
            double len = Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
            v[0] = v[0] / len * radius;
            v[1] = v[1] / len * radius;
            v[2] = v[2] / len * radius;
        }

        int[][] facesData = {
            {0,11,5}, {0,5,1}, {0,1,7}, {0,7,10}, {0,10,11},
            {1,5,9}, {5,11,4}, {11,10,2}, {10,7,6}, {7,1,8},
            {3,9,4}, {3,4,2}, {3,2,6}, {3,6,8}, {3,8,9},
            {4,9,5}, {2,4,11}, {6,2,10}, {8,6,7}, {9,8,1}
        };

        Map<Long, Integer> middlePointCache = new HashMap<>();
        List<int[]> fList = new ArrayList<>();
        for (int[] f : facesData) fList.add(f);

        for (int i = 0; i < subdivisions; i++) {
            List<int[]> faces2 = new ArrayList<>();
            for (int[] tri : fList) {
                int a = tri[0];
                int b = tri[1];
                int c = tri[2];
                int ab = getMiddlePoint(a, b, vertsTmp, middlePointCache, radius);
                int bc = getMiddlePoint(b, c, vertsTmp, middlePointCache, radius);
                int ca = getMiddlePoint(c, a, vertsTmp, middlePointCache, radius);

                faces2.add(new int[]{a, ab, ca});
                faces2.add(new int[]{b, bc, ab});
                faces2.add(new int[]{c, ca, bc});
                faces2.add(new int[]{ab, bc, ca});
            }
            fList = faces2;
        }

        // Save vertices as float[] and List<float[]>
        this.verts = new float[vertsTmp.size() * 3];
        this.vertsList = new ArrayList<>(vertsTmp.size());
        for (int i = 0; i < vertsTmp.size(); i++) {
            double[] v = vertsTmp.get(i);
            this.verts[3*i  ] = (float)v[0];
            this.verts[3*i+1] = (float)v[1];
            this.verts[3*i+2] = (float)v[2];
            this.vertsList.add(new float[] {(float)v[0], (float)v[1], (float)v[2]});
        }

        this.faces = new int[fList.size() * 6];
        this.facesList = new ArrayList<>(fList.size());
        for (int i = 0; i < fList.size(); i++) {
            int[] tri = fList.get(i);
            this.faces[i*6  ] = tri[0]; this.faces[i*6+1] = 0;
            this.faces[i*6+2] = tri[1]; this.faces[i*6+3] = 0;
            this.faces[i*6+4] = tri[2]; this.faces[i*6+5] = 0;
            this.facesList.add(tri.clone());
        }

        getPoints().setAll(verts);
        getTexCoords().addAll(0, 0);
        getFaces().setAll(faces);
        getFaceSmoothingGroups().clear();
        for (int i = 0; i < faces.length / 6; i++) getFaceSmoothingGroups().addAll(1);
    }

    private static long getEdgeKey(int a, int b) {
        return (long)Math.min(a, b) << 32 | Math.max(a, b);
    }
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
        double len = Math.sqrt(vm[0]*vm[0] + vm[1]*vm[1] + vm[2]*vm[2]);
        vm[0] = vm[0] / len * radius;
        vm[1] = vm[1] / len * radius;
        vm[2] = vm[2] / len * radius;
        verts.add(vm);
        int idx = verts.size() - 1;
        cache.put(key, idx);
        return idx;
    }
    public float[] getVertsArray() { return verts; }
    public List<float[]> getVertsList() { return vertsList; }
    public int[] getFacesArray() { return faces; }
    public List<int[]> getFacesList() { return facesList; }
}