package AsteroidField.asteroids.geometry;

import AsteroidField.asteroids.parameters.CapsuleAsteroidParameters;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.scene.shape.TriangleMesh;

public class CapsuleMesh extends TriangleMesh {
    protected final float[] verts;
    protected final List<float[]> vertsList;
    protected final int[] faces;
    public final int slices, stacks;
    public final double width, length;

    public CapsuleMesh(int slices, int stacks, double width, double length) {
        super();
        this.slices = slices;
        this.stacks = stacks;
        this.width = width;
        this.length = length;

        int capStacks = stacks / 2;
        int cols = slices + 1;
        List<float[]> tempVerts = new ArrayList<>();
        List<int[]> tempFaces = new ArrayList<>();

        // Top pole (split)
        int topPoleStart = tempVerts.size();
        double yTop = (length / 2.0) + width;
        for (int j = 0; j < slices; j++) tempVerts.add(new float[] {0f, (float)yTop, 0f});

        // First ring below top
        int topRingStart = tempVerts.size();
        double phi = Math.PI / 2 - Math.PI / (2 * capStacks);
        double y = (length / 2.0) + width * Math.sin(phi);
        double r = width * Math.cos(phi);
        for (int j = 0; j < cols; j++) {
            double theta = 2 * Math.PI * j / slices;
            tempVerts.add(new float[]{
                (float)(r * Math.cos(theta)),
                (float)y,
                (float)(r * Math.sin(theta))
            });
        }

        // Top hemisphere
        int prevRingStart = topRingStart;
        for (int i = 1; i < capStacks; i++) {
            int thisRingStart = tempVerts.size();
            phi = Math.PI / 2 - Math.PI * i / (2 * capStacks);
            y = (length / 2.0) + width * Math.sin(phi);
            r = width * Math.cos(phi);
            for (int j = 0; j < cols; j++) {
                double theta = 2 * Math.PI * j / slices;
                tempVerts.add(new float[]{
                        (float)(r * Math.cos(theta)),
                        (float)y,
                        (float)(r * Math.sin(theta))
                });
            }
            for (int j = 0; j < slices; j++) {
                int a = prevRingStart + j;
                int b = prevRingStart + j + 1;
                int c = thisRingStart + j;
                int d = thisRingStart + j + 1;
                tempFaces.add(new int[]{a, b, c});
                tempFaces.add(new int[]{b, d, c});
            }
            prevRingStart = thisRingStart;
        }

        // Cylinder
        int cylRings = stacks - 1;
        for (int i = 0; i < cylRings; i++) {
            int thisRingStart = tempVerts.size();
            y = (length / 2.0) - ((i + 1.0) / stacks) * length;
            for (int j = 0; j < cols; j++) {
                double theta = 2 * Math.PI * j / slices;
                tempVerts.add(new float[]{
                        (float)(width * Math.cos(theta)),
                        (float)y,
                        (float)(width * Math.sin(theta))
                });
            }
            for (int j = 0; j < slices; j++) {
                int a = prevRingStart + j;
                int b = prevRingStart + j + 1;
                int c = thisRingStart + j;
                int d = thisRingStart + j + 1;
                tempFaces.add(new int[]{a, b, c});
                tempFaces.add(new int[]{b, d, c});
            }
            prevRingStart = thisRingStart;
        }

        // Bottom hemisphere
        for (int i = 1; i <= capStacks; i++) {
            int thisRingStart = tempVerts.size();
            phi = -Math.PI * i / (2 * capStacks);
            y = -(length / 2.0) + width * Math.sin(phi);
            r = width * Math.cos(phi);
            for (int j = 0; j < cols; j++) {
                double theta = 2 * Math.PI * j / slices;
                tempVerts.add(new float[]{
                        (float)(r * Math.cos(theta)),
                        (float)y,
                        (float)(r * Math.sin(theta))
                });
            }
            for (int j = 0; j < slices; j++) {
                int a = prevRingStart + j;
                int b = prevRingStart + j + 1;
                int c = thisRingStart + j;
                int d = thisRingStart + j + 1;
                tempFaces.add(new int[]{a, b, c});
                tempFaces.add(new int[]{b, d, c});
            }
            prevRingStart = thisRingStart;
        }

        // Bottom cap (split)
        int botPoleStart = tempVerts.size();
        double yBot = -(length / 2.0) - width;
        for (int j = 0; j < slices; j++) tempVerts.add(new float[] {0f, (float)yBot, 0f});
        int lastRingStart = prevRingStart;
        for (int j = 0; j < slices; j++) {
            int pole = botPoleStart + j;
            int a = lastRingStart + j;
            int b = lastRingStart + j + 1;
            tempFaces.add(new int[]{b, a, pole});
        }
        for (int j = 0; j < slices; j++) {
            int pole = topPoleStart + j;
            int a = topRingStart + j;
            int b = topRingStart + j + 1;
            tempFaces.add(new int[]{pole, b, a});
        }

        // Convert to arrays
        this.vertsList = tempVerts;
        this.verts = new float[tempVerts.size() * 3];
        for (int i = 0; i < tempVerts.size(); i++) {
            float[] v = tempVerts.get(i);
            verts[i * 3] = v[0];
            verts[i * 3 + 1] = v[1];
            verts[i * 3 + 2] = v[2];
        }
        this.faces = new int[tempFaces.size() * 6];
        for (int i = 0; i < tempFaces.size(); i++) {
            int[] f = tempFaces.get(i);
            faces[i * 6] = f[0];
            faces[i * 6 + 1] = 0;
            faces[i * 6 + 2] = f[1];
            faces[i * 6 + 3] = 0;
            faces[i * 6 + 4] = f[2];
            faces[i * 6 + 5] = 0;
        }

        getPoints().setAll(verts);
        getTexCoords().addAll(0, 0);
        getFaces().setAll(faces);
        getFaceSmoothingGroups().clear();
        for (int i = 0; i < faces.length / 6; i++) getFaceSmoothingGroups().addAll(1);
    }

    /** Deform with bumps/craters. Call after construction or whenever parameters change. */
    public void deform(CapsuleAsteroidParameters params) {
        double width = params.getWidth();
        double length = params.getLength();

        float[] meshVerts = verts;
        List<float[]> vertsLocal = vertsList;
        List<double[]> craters = params.getCraterCenters();
        List<double[]> bumps = params.getBumpCenters();
        if (craters == null) craters = java.util.Collections.emptyList();
        if (bumps == null) bumps = java.util.Collections.emptyList();

        Random deformRng = new Random(params.getSeed() ^ 0xCAB51234);
        double deform = params.getDeformation();

        for (int idx = 0; idx < vertsLocal.size(); idx++) {
            float[] v = vertsLocal.get(idx);
            double[] p = {v[0], v[1], v[2]};
            double disp = 0;
            // Craters
            for (double[] c : craters) {
                double d = distOnCapsule(p, c, width, length);
                double normD = d / (params.getCraterRadius() * width);
                if (normD < 1.0) {
                    disp -= params.getCraterDepth() * width * (1 - normD * normD);
                }
            }
            // Bumps
            for (double[] b : bumps) {
                double d = distOnCapsule(p, b, width, length);
                double normD = d / (params.getBumpRadius() * width);
                if (normD < 1.0) {
                    disp += params.getBumpHeight() * width * (1 - normD * normD);
                }
            }
            // Move vertex along local normal (approx: away from Y axis)
            double r0 = Math.sqrt(p[0]*p[0] + p[2]*p[2]);
            double nx = r0 > 1e-6 ? p[0]/r0 : 0, ny = 0, nz = r0 > 1e-6 ? p[2]/r0 : 0;
            if (Math.abs(p[1]) > (length/2.0) - 1e-2) {
                ny = p[1] > 0 ? 1 : -1;
            }
            // --- Apply deformation bump ---
            double bumpRand = 1.0 + deform * (deformRng.nextDouble() - 0.5) * 2.0;
            meshVerts[idx * 3]     = (float) (p[0] + nx * disp) * (float) bumpRand;
            meshVerts[idx * 3 + 1] = (float) (p[1] + ny * disp) * (float) bumpRand;
            meshVerts[idx * 3 + 2] = (float) (p[2] + nz * disp) * (float) bumpRand;
        }
        getPoints().setAll(meshVerts);
    }

    // Helper for deformation logic
    public static double distOnCapsule(double[] p, double[] c, double width, double length) {
        double dx = p[0] - c[0];
        double dy = p[1] - c[1];
        double dz = p[2] - c[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    // Utility for random surface point
    public static double[] randomCapsuleSurfacePoint(Random rng, double width, double length) {
        double t = rng.nextDouble();
        double theta = 2 * Math.PI * rng.nextDouble();
        if (t < 0.25) {
            double phi = Math.acos(2 * rng.nextDouble() - 1) / 2;
            double y = (length / 2.0) + width * Math.sin(phi);
            double r = width * Math.cos(phi);
            return new double[]{r * Math.cos(theta), y, r * Math.sin(theta)};
        } else if (t < 0.75) {
            double y = (length / 2.0) - rng.nextDouble() * length;
            return new double[]{width * Math.cos(theta), y, width * Math.sin(theta)};
        } else {
            double phi = -Math.acos(2 * rng.nextDouble() - 1) / 2;
            double y = -(length / 2.0) + width * Math.sin(phi);
            double r = width * Math.cos(phi);
            return new double[]{r * Math.cos(theta), y, r * Math.sin(theta)};
        }
    }

    public float[] getVertsArray() { return verts; }
    public List<float[]> getVertsList() { return vertsList; }
    public int[] getFacesArray() { return faces; }
}