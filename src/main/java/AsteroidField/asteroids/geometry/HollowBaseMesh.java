package AsteroidField.asteroids.geometry;

import AsteroidField.asteroids.parameters.HollowBaseAsteroidParameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Builds a hollow asteroid by:
 *  1) Starting from IcosphereMesh (outer shell base).
 *  2) Removing faces inside mouth cones on the OUTER shell.
 *  3) Adding an INNER shell: duplicate all vertices scaled to innerRadius,
 *     add faces with REVERSED winding (inward normals),
 *     and also remove inner faces inside the mouth cones (creating an actual hole through).
 *
 * UVs: single (0,0) coord for all faces (like your baselines).
 * NOTE: This version intentionally does NOT add "tunnel wall" sleeves between shells yet.
 *       The opening looks like a straight port through the shell thickness.
 *       We can stitch ring quads later once you’re happy with the base look.
 */
public class HollowBaseMesh extends IcosphereMesh {

    public HollowBaseMesh(double radius, int subdivisions, HollowBaseAsteroidParameters params) {
        super(radius, subdivisions);
        build(params);
    }

    private void build(HollowBaseAsteroidParameters p) {
        // 0) exterior noise/deform on the outer shell
        deformExterior(p);

        // 1) Mouth directions + aperture
        double[][] mouthDirs = sampleDirections(p.getMouthCount(), p.getSeed(), p.getMouthJitter());
        double apertureRad = Math.toRadians(p.getMouthApertureDeg());

        // 2) OUTER faces: keep only those OUTSIDE the mouth cones
        List<int[]> outerFaces = new ArrayList<>();
        for (int[] tri : facesList) {
            if (!centroidInsideAnyCone(vertsList, tri, mouthDirs, apertureRad)) {
                outerFaces.add(new int[]{tri[0], tri[1], tri[2]});
            }
        }

        // 3) INNER vertices: duplicate each base vertex, scale toward center,
        //    and add some seeded roughness for organic interior.
        int baseVertCount = vertsList.size();
        double innerR = p.getRadius() * p.getInnerRadiusRatio();
        Random innerRng = new Random(p.getSeed() ^ 0x9E3779B97F4A7C15L);
        List<float[]> allVerts = new ArrayList<>(baseVertCount * 2);
        allVerts.addAll(vertsList); // index [0..base-1] outer

        for (int i = 0; i < baseVertCount; i++) {
            float[] v = vertsList.get(i);
            double x = v[0], y = v[1], z = v[2];
            double len = Math.sqrt(x*x + y*y + z*z);
            if (len == 0) len = 1.0;
            double nx = x / len, ny = y / len, nz = z / len;

            // inner roughness: simple deterministic jitter (use real noise later if you have it)
            double jitter = 1.0 + p.getInnerNoiseAmp() * (innerRng.nextDouble() - 0.5) * 2.0;
            double r = innerR * jitter;

            allVerts.add(new float[]{ (float)(nx * r), (float)(ny * r), (float)(nz * r) });
        }

        // 4) INNER faces: mirror of base faces BUT reversed order (to make normals face inward).
        //    Also cull any inner face whose centroid lies inside a mouth cone.
        List<int[]> innerFaces = new ArrayList<>();
        for (int[] tri : facesList) {
            int a = tri[0], b = tri[1], c = tri[2];
            int ia = a + baseVertCount;
            int ib = b + baseVertCount;
            int ic = c + baseVertCount;

            // Check cone test using inner vertex positions
            if (!centroidInsideAnyCone(allVerts, ia, ib, ic, mouthDirs, apertureRad)) {
                // reverse order (a,b,c) -> (a', c', b') to face inward
                innerFaces.add(new int[]{ ia, ic, ib });
            }
        }

        // 5) Assemble final buffers: points + faces (UV index 0 for all)
        float[] pts = new float[allVerts.size() * 3];
        for (int i = 0; i < allVerts.size(); i++) {
            float[] v = allVerts.get(i);
            pts[i*3  ] = v[0];
            pts[i*3+1] = v[1];
            pts[i*3+2] = v[2];
        }

        int totalTriCount = outerFaces.size() + innerFaces.size();
        int[] faceBuf = new int[totalTriCount * 6];
        int fi = 0;
        for (int[] t : outerFaces) {
            faceBuf[fi++] = t[0]; faceBuf[fi++] = 0;
            faceBuf[fi++] = t[1]; faceBuf[fi++] = 0;
            faceBuf[fi++] = t[2]; faceBuf[fi++] = 0;
        }
        for (int[] t : innerFaces) {
            faceBuf[fi++] = t[0]; faceBuf[fi++] = 0;
            faceBuf[fi++] = t[1]; faceBuf[fi++] = 0;
            faceBuf[fi++] = t[2]; faceBuf[fi++] = 0;
        }

        // 6) Push to TriangleMesh
        getPoints().setAll(pts);
        getTexCoords().setAll(0, 0); // single UV
        getFaces().setAll(faceBuf);
        getFaceSmoothingGroups().clear();
        int tris = faceBuf.length / 6;
        for (int i = 0; i < tris; i++) getFaceSmoothingGroups().addAll(1);
    }

    private void deformExterior(HollowBaseAsteroidParameters p) {
        Random rng = new Random(p.getSeed() ^ 0xA17E5B3DL);
        double deform = p.getDeformation();
        double R = p.getRadius();

        for (int i = 0; i < verts.length; i += 3) {
            float[] v = vertsList.get(i / 3);
            double x = v[0], y = v[1], z = v[2];
            double len = Math.sqrt(x*x + y*y + z*z);
            if (len == 0) len = 1.0;
            double nx = x / len, ny = y / len, nz = z / len;

            double bump = 1.0 + deform * (rng.nextDouble() - 0.5) * 2.0;
            double newLen = R * bump;

            verts[i  ] = (float)(nx * newLen);
            verts[i+1] = (float)(ny * newLen);
            verts[i+2] = (float)(nz * newLen);
        }
        // update outer shell points (faces remain as base for now)
        getPoints().setAll(verts);
    }

    private static double[][] sampleDirections(int count, long seed, double jitter) {
        Random rnd = new Random(seed ^ 0x6C62272E07BB0142L);
        int n = Math.max(1, count);
        double[][] dirs = new double[n][3];
        double ga = Math.PI * (3 - Math.sqrt(5));
        double jitterClamped = Math.max(0.0, Math.min(0.5, jitter));

        for (int k = 0; k < n; k++) {
            double y = (n == 1) ? 0.0 : (1.0 - 2.0 * k / (n - 1.0));
            double r = Math.sqrt(Math.max(0, 1 - y*y));
            double phi = ga * k + (rnd.nextDouble() - 0.5) * 2.0 * Math.PI * jitterClamped;
            double x = Math.cos(phi) * r;
            double z = Math.sin(phi) * r;
            double len = Math.sqrt(x*x + y*y + z*z);
            dirs[k][0] = x/len; dirs[k][1] = y/len; dirs[k][2] = z/len;
        }
        return dirs;
    }

    private static boolean centroidInsideAnyCone(List<float[]> vList, int[] tri, double[][] dirs, double apertureRad) {
        return centroidInsideAnyCone(vList, tri[0], tri[1], tri[2], dirs, apertureRad);
    }
    private static boolean centroidInsideAnyCone(List<float[]> vList, int ia, int ib, int ic, double[][] dirs, double apertureRad) {
        float[] a = vList.get(ia), b = vList.get(ib), c = vList.get(ic);
        double cx = (a[0] + b[0] + c[0]) / 3.0;
        double cy = (a[1] + b[1] + c[1]) / 3.0;
        double cz = (a[2] + b[2] + c[2]) / 3.0;
        double len = Math.sqrt(cx*cx + cy*cy + cz*cz);
        if (len == 0) return false;
        double nx = cx / len, ny = cy / len, nz = cz / len;

        for (double[] d : dirs) {
            double dot = nx*d[0] + ny*d[1] + nz*d[2];
            dot = Math.max(-1, Math.min(1, dot));
            double ang = Math.acos(dot);
            if (ang <= apertureRad) return true;
        }
        return false;
    }
}
