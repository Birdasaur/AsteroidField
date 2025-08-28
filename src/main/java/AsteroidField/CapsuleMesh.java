package AsteroidField;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.scene.shape.TriangleMesh;

public class CapsuleMesh extends TriangleMesh {
    private final float[] verts;
    private final List<float[]> vertsList;
    private final int[] faces;
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

    /** Helper Utility Methods **/

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

    public static double distOnCapsule(double[] p, double[] c, double width, double length) {
        double dx = p[0] - c[0];
        double dy = p[1] - c[1];
        double dz = p[2] - c[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }    
    
    /** Public access for deformation code. */
    public float[] getVertsArray() { return verts; }
    public List<float[]> getVertsList() { return vertsList; }
    public int[] getFacesArray() { return faces; }
}
