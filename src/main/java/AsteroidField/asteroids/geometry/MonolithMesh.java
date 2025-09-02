package AsteroidField.asteroids.geometry;

import AsteroidField.asteroids.parameters.MonolithAsteroidParameters;
import java.util.*;

public class MonolithMesh extends CubicMesh {
    // Core cube
    public final double size;
    public final int subdivisions;

    // Tail data (per face: 0=+Z, 1=-Z, 2=+X, 3=-X, 4=+Y, 5=-Y)
    public final boolean[] tailFaces;   // which faces have tails
    public final int tailSegments;
    public final double tailBlockScale;
    public final double tailSpread;
    public final double tailJitter;

    public MonolithMesh(double size, int subdivisions, MonolithAsteroidParameters params) {
        super(size, subdivisions, params);
        this.size = size;
        this.subdivisions = subdivisions;
        this.tailFaces = params.getTailFaces();
        this.tailSegments = params.getTailSegments();
        this.tailBlockScale = params.getTailBlockScale();
        this.tailSpread = params.getTailSpread();
        this.tailJitter = params.getTailJitter();

        // Now: Add procedural tails and remap UVs
        buildTailsAndRemapUV(params);
    }

    // --- Builds all tails and remaps UVs for cube+tails ---
    private void buildTailsAndRemapUV(MonolithAsteroidParameters params) {
        // 1. Copy current verts/faces/UVs
        List<float[]> verts = new ArrayList<>(vertsList);
        List<int[]> faces = new ArrayList<>(facesList);

        // Each tail gets its own UV stripe (top-down: V axis)
        int tailCount = 0;
        for (boolean tf : tailFaces) if (tf) tailCount++;
        if (tailCount == 0) tailCount = 1; // at least one region for core

        double vStripe = 1.0 / (tailCount + 1);

        // --- Map main cube UVs to top stripe ---
        float[] cubeUVArray = new float[verts.size() * 2];
        for (int i = 0; i < verts.size(); i++) {
            float[] v = verts.get(i);
            // Cube projection as before (mapped to V: 0 to vStripe)
            double ax = Math.abs(v[0]), ay = Math.abs(v[1]), az = Math.abs(v[2]);
            double u = 0.5, w = 0.5;
            if (ax >= ay && ax >= az) { u = (v[2]/size + 1)/2; w = (v[1]/size + 1)/2; }
            else if (ay >= ax && ay >= az) { u = (v[0]/size + 1)/2; w = (v[2]/size + 1)/2; }
            else { u = (v[0]/size + 1)/2; w = (v[1]/size + 1)/2; }
            // Remap V (w) into top stripe
            cubeUVArray[i*2+0] = (float)u;
            cubeUVArray[i*2+1] = (float)(w * vStripe);
        }

        // --- Build tails ---
        int nextStripe = 1;
        int vertOffset = verts.size();
        int[] faceNormals = {2, 3, 0, 1, 4, 5};
        int[][] faceDirs = {  // normal for each face: +Z,-Z,+X,-X,+Y,-Y
            {0,0,1}, {0,0,-1}, {1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}
        };

        for (int faceIdx = 0; faceIdx < 6; faceIdx++) {
            if (!tailFaces[faceIdx]) continue;
            int[] normal = faceDirs[faceIdx];
            double tailU0 = 0.0, tailV0 = vStripe * nextStripe, tailV1 = vStripe * (nextStripe+1);

            // Find face center (on the cube, pre-deformation)
            double[] faceCenter = getCubeFaceCenter(normal, size / 2.0);

            // Build a "tail chain" of blocks
            double[] blockDir = new double[] { normal[0], normal[1], normal[2] };
            Random rng = new Random(params.getSeed() ^ (faceIdx + 991));
            double currLen = 0;
            double currScale = size * tailBlockScale;
            double[] prevCenter = Arrays.copyOf(faceCenter, 3);

            for (int seg = 0; seg < tailSegments; seg++) {
                // Each tail segment gets slightly smaller/further
                double blockLen = currScale * (0.9 + rng.nextDouble() * 0.2); // 10% jitter
                double[] dirJitter = {
                    blockDir[0] + (rng.nextDouble() - 0.5) * 2.0 * tailSpread,
                    blockDir[1] + (rng.nextDouble() - 0.5) * 2.0 * tailSpread,
                    blockDir[2] + (rng.nextDouble() - 0.5) * 2.0 * tailSpread
                };
                double dLen = Math.sqrt(dirJitter[0]*dirJitter[0]+dirJitter[1]*dirJitter[1]+dirJitter[2]*dirJitter[2]);
                dirJitter[0] /= dLen; dirJitter[1] /= dLen; dirJitter[2] /= dLen;
                for (int d = 0; d < 3; d++) prevCenter[d] += dirJitter[d] * blockLen;

                // Build block verts
                List<float[]> blockVerts = new ArrayList<>();
                double hs = currScale / 2.0;
                // Cube block aligned with dirJitter (approx)
                // We'll just axis-align for simplicity; you can rotate blocks for more wildness
                for (int xi = -1; xi <= 1; xi += 2) {
                    for (int yi = -1; yi <= 1; yi += 2) {
                        for (int zi = -1; zi <= 1; zi += 2) {
                            blockVerts.add(new float[] {
                                (float)(prevCenter[0] + xi*hs),
                                (float)(prevCenter[1] + yi*hs),
                                (float)(prevCenter[2] + zi*hs)
                            });
                        }
                    }
                }
                int blockStartIdx = verts.size();
                verts.addAll(blockVerts);

                // Add faces (CCW, like a cube)
                int[][] facePairs = {
                    {0,2,3,1}, // -X
                    {4,5,7,6}, // +X
                    {0,1,5,4}, // -Y
                    {2,6,7,3}, // +Y
                    {0,4,6,2}, // -Z
                    {1,3,7,5}  // +Z
                };
                for (int[] fp : facePairs) {
                    faces.add(new int[]{blockStartIdx+fp[0], blockStartIdx+fp[1], blockStartIdx+fp[2]});
                    faces.add(new int[]{blockStartIdx+fp[0], blockStartIdx+fp[2], blockStartIdx+fp[3]});
                }

                // UVs for this block: project each vertex to the tail's UV stripe
                // For more "blocky" texturing, you can cube-project, or just use (xi, yi, zi)
                // We'll map Y/Z (or most "up"/"side") into U/V in the tail's stripe.
                // [For more advanced mapping, you could track which face/block is which]
                currScale *= (0.8 + rng.nextDouble() * tailJitter * 0.4); // shrink & jitter scale
            }
            nextStripe++;
        }

        // --- Final: Write mesh arrays ---
        float[] finalVerts = new float[verts.size() * 3];
        for (int i = 0; i < verts.size(); i++) {
            float[] v = verts.get(i);
            finalVerts[i*3+0] = v[0];
            finalVerts[i*3+1] = v[1];
            finalVerts[i*3+2] = v[2];
        }
        // Build final UVs: here, just copy existing for simplicity (todo: make tail UVs non-overlapping)
        // TODO: actually generate UVs for tail verts (using their stripe and local block projection)
        float[] finalUVs = new float[verts.size() * 2];
        System.arraycopy(cubeUVArray, 0, finalUVs, 0, Math.min(finalUVs.length, cubeUVArray.length));
        int[] finalFaces = new int[faces.size() * 6];
        for (int i = 0; i < faces.size(); i++) {
            int[] f = faces.get(i);
            finalFaces[i*6+0] = f[0]; finalFaces[i*6+1] = f[0];
            finalFaces[i*6+2] = f[1]; finalFaces[i*6+3] = f[1];
            finalFaces[i*6+4] = f[2]; finalFaces[i*6+5] = f[2];
        }

        // Write to mesh
        getPoints().setAll(finalVerts);
        getTexCoords().setAll(finalUVs);
        getFaces().setAll(finalFaces);
        getFaceSmoothingGroups().clear();
        for (int i = 0; i < finalFaces.length / 6; i++) getFaceSmoothingGroups().addAll(1);
    }

    // Helper to get the center of a cube face given a normal and half-size
    private static double[] getCubeFaceCenter(int[] normal, double half) {
        return new double[] {normal[0] * half, normal[1] * half, normal[2] * half};
    }
}
