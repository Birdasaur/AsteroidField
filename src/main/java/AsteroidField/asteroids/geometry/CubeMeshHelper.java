package AsteroidField.asteroids.geometry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CubeMeshHelper {
    // === Generate a subdivided cube as triangles ===
    public static CubeMeshData generateCube(double size, int subdivisions) {
        List<float[]> verts = new ArrayList<>();
        List<int[]> faces = new ArrayList<>();
        // Directions for cube faces
        int[][] faceNormals = {{0,0,1}, {0,0,-1}, {1,0,0}, {-1,0,0}, {0,1,0}, {0,-1,0}};
        int[][] uDirs = {{1,0,0}, {-1,0,0}, {0,0,-1}, {0,0,1}, {1,0,0}, {1,0,0}};
        int[][] vDirs = {{0,1,0}, {0,1,0}, {0,1,0}, {0,1,0}, {0,0,-1}, {0,0,1}};

        double half = size/2.0;
        int grid = Math.max(1, subdivisions + 1);

        Map<String, Integer> vertMap = new HashMap<>();
        for (int f = 0; f < 6; f++) {
            int[] n = faceNormals[f];
            int[] uDir = uDirs[f];
            int[] vDir = vDirs[f];
            for (int i = 0; i <= grid; i++) {
                double u = -half + (size * i / grid);
                for (int j = 0; j <= grid; j++) {
                    double v = -half + (size * j / grid);
                    double[] pos = {
                        n[0]*half + u*uDir[0] + v*vDir[0],
                        n[1]*half + u*uDir[1] + v*vDir[1],
                        n[2]*half + u*uDir[2] + v*vDir[2]
                    };
                    String key = String.format("%.5f,%.5f,%.5f", pos[0], pos[1], pos[2]);
                    if (!vertMap.containsKey(key)) {
                        verts.add(new float[] {(float)pos[0], (float)pos[1], (float)pos[2]});
                        vertMap.put(key, verts.size() - 1);
                    }
                }
            }
        }
        // Build faces
        for (int f = 0; f < 6; f++) {
            int[] n = faceNormals[f];
            int[] uDir = uDirs[f];
            int[] vDir = vDirs[f];

            for (int i = 0; i < grid; i++) {
                for (int j = 0; j < grid; j++) {
                    double[] p00 = {
                        n[0]*half + (-half + size * i   / grid)*uDir[0] + (-half + size * j   / grid)*vDir[0],
                        n[1]*half + (-half + size * i   / grid)*uDir[1] + (-half + size * j   / grid)*vDir[1],
                        n[2]*half + (-half + size * i   / grid)*uDir[2] + (-half + size * j   / grid)*vDir[2]
                    };
                    double[] p10 = {
                        n[0]*half + (-half + size * (i+1)/grid)*uDir[0] + (-half + size * j   / grid)*vDir[0],
                        n[1]*half + (-half + size * (i+1)/grid)*uDir[1] + (-half + size * j   / grid)*vDir[1],
                        n[2]*half + (-half + size * (i+1)/grid)*uDir[2] + (-half + size * j   / grid)*vDir[2]
                    };
                    double[] p01 = {
                        n[0]*half + (-half + size * i   / grid)*uDir[0] + (-half + size * (j+1)/grid)*vDir[0],
                        n[1]*half + (-half + size * i   / grid)*uDir[1] + (-half + size * (j+1)/grid)*vDir[1],
                        n[2]*half + (-half + size * i   / grid)*uDir[2] + (-half + size * (j+1)/grid)*vDir[2]
                    };
                    double[] p11 = {
                        n[0]*half + (-half + size * (i+1)/grid)*uDir[0] + (-half + size * (j+1)/grid)*vDir[0],
                        n[1]*half + (-half + size * (i+1)/grid)*uDir[1] + (-half + size * (j+1)/grid)*vDir[1],
                        n[2]*half + (-half + size * (i+1)/grid)*uDir[2] + (-half + size * (j+1)/grid)*vDir[2]
                    };
                    int idx00 = vertMap.get(String.format("%.5f,%.5f,%.5f", p00[0], p00[1], p00[2]));
                    int idx10 = vertMap.get(String.format("%.5f,%.5f,%.5f", p10[0], p10[1], p10[2]));
                    int idx01 = vertMap.get(String.format("%.5f,%.5f,%.5f", p01[0], p01[1], p01[2]));
                    int idx11 = vertMap.get(String.format("%.5f,%.5f,%.5f", p11[0], p11[1], p11[2]));
                    // Two triangles per quad
                    faces.add(new int[]{idx00, idx10, idx11});
                    faces.add(new int[]{idx00, idx11, idx01});
                }
            }
        }
        float[] vertsArray = new float[verts.size() * 3];
        for (int i = 0; i < verts.size(); i++) {
            vertsArray[i*3+0] = verts.get(i)[0];
            vertsArray[i*3+1] = verts.get(i)[1];
            vertsArray[i*3+2] = verts.get(i)[2];
        }
        int[] facesArray = new int[faces.size() * 3];
        for (int i = 0; i < faces.size(); i++) {
            int[] f = faces.get(i);
            facesArray[i*3+0] = f[0];
            facesArray[i*3+1] = f[1];
            facesArray[i*3+2] = f[2];
        }
        return new CubeMeshData(vertsArray, verts, facesArray, faces);
    }

    // === Deform mesh vertices ===
    public static void deformVertices(List<float[]> vertsList, float[] verts, double deformation, long seed) {
        Random rng = new Random(seed);
        for (int i = 0; i < vertsList.size(); i++) {
            float[] v = vertsList.get(i);
            double scale = 1.0 + deformation * (rng.nextDouble() - 0.5) * 2.0;
            v[0] *= scale;
            v[1] *= scale;
            v[2] *= scale;
            verts[i*3+0] = v[0];
            verts[i*3+1] = v[1];
            verts[i*3+2] = v[2];
        }
    }

    // === Simple cube-projection UVs ===
    public static CubeUVMapping buildCubeUVs(CubeMeshData mesh, double size) {
        int numVerts = mesh.vertsList.size();
        float[] uvArray = new float[numVerts * 2];
        for (int i = 0; i < numVerts; i++) {
            float[] v = mesh.vertsList.get(i);
            double ax = Math.abs(v[0]), ay = Math.abs(v[1]), az = Math.abs(v[2]);
            double u = 0.5, w = 0.5;
            if (ax >= ay && ax >= az) { // X face
                u = (v[2]/size + 1)/2; w = (v[1]/size + 1)/2;
            } else if (ay >= ax && ay >= az) { // Y face
                u = (v[0]/size + 1)/2; w = (v[2]/size + 1)/2;
            } else { // Z face
                u = (v[0]/size + 1)/2; w = (v[1]/size + 1)/2;
            }
            uvArray[i*2+0] = (float)u;
            uvArray[i*2+1] = (float)w;
        }
        int[] faceArray = new int[mesh.facesList.size() * 6];
        for (int i = 0; i < mesh.facesList.size(); i++) {
            int[] f = mesh.facesList.get(i);
            faceArray[i*6+0] = f[0]; faceArray[i*6+1] = f[0];
            faceArray[i*6+2] = f[1]; faceArray[i*6+3] = f[1];
            faceArray[i*6+4] = f[2]; faceArray[i*6+5] = f[2];
        }
        return new CubeUVMapping(uvArray, faceArray);
    }
}