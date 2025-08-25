package AsteroidField;

import javafx.scene.shape.TriangleMesh;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import java.util.*;
import java.util.function.Consumer;

public class CapsuleAsteroidMeshProvider implements AsteroidMeshProvider {

    @Override
    public TriangleMesh generateMesh(AsteroidParameters base) {
        CapsuleAsteroidParameters params = (CapsuleAsteroidParameters) base;

        int slices = Math.max(12, params.getSubdivisions() * 8);  // circular segments
        int stacks = Math.max(8, params.getSubdivisions() * 4);   // longitudinal (cylinder)
        double width = params.getWidth();
        double length = params.getLength();

        // --- 1. Build Vertices ---
        List<float[]> verts = new ArrayList<>();
        List<int[]> faces = new ArrayList<>();
        int capStacks = stacks / 2; // hemisphere stacks

        // Top hemisphere
        for (int i = 0; i <= capStacks; i++) {
            double phi = Math.PI / 2 * (1.0 - (double) i / capStacks);
            double y = (length / 2.0) + width * Math.sin(phi);
            double r = width * Math.cos(phi);
            for (int j = 0; j <= slices; j++) {
                double theta = 2 * Math.PI * j / slices;
                verts.add(new float[]{
                        (float) (r * Math.cos(theta)),
                        (float) y,
                        (float) (r * Math.sin(theta))
                });
            }
        }
        // Cylinder (middle)
        for (int i = 1; i < stacks; i++) { // NOTE: i=1 to stacks-1 (not 0 or stacks!)
            double y = (length / 2.0) - (double) i / stacks * length;
            for (int j = 0; j <= slices; j++) {
                double theta = 2 * Math.PI * j / slices;
                verts.add(new float[]{
                        (float) (width * Math.cos(theta)),
                        (float) y,
                        (float) (width * Math.sin(theta))
                });
            }
        }
        // Bottom hemisphere
        for (int i = 0; i <= capStacks; i++) {
            double phi = -Math.PI / 2 * (double) i / capStacks;
            double y = -(length / 2.0) + width * Math.sin(phi);
            double r = width * Math.cos(phi);
            for (int j = 0; j <= slices; j++) {
                double theta = 2 * Math.PI * j / slices;
                verts.add(new float[]{
                        (float) (r * Math.cos(theta)),
                        (float) y,
                        (float) (r * Math.sin(theta))
                });
            }
        }

        // --- 2. Build Faces ---
        // rows: top hemi (capStacks+1), cyl (stacks-1), bot hemi (capStacks+1)
        int rows = capStacks + 1 + (stacks - 1) + capStacks + 1;
        int cols = slices + 1;

        for (int i = 0; i < rows - 1; i++) {
            for (int j = 0; j < slices; j++) {
                int p0 = i * cols + j;
                int p1 = p0 + 1;
                int p2 = p0 + cols;
                int p3 = p2 + 1;
                faces.add(new int[]{p0, p2, p1});
                faces.add(new int[]{p1, p2, p3});
            }
        }

        // --- 3. Bumps and Craters ---
        Random rng = new Random(params.getSeed());
        List<double[]> craters = new ArrayList<>();
        for (int i = 0; i < params.getCraterCount(); i++)
            craters.add(randomCapsuleSurfacePoint(rng, width, length));
        List<double[]> bumps = new ArrayList<>();
        for (int i = 0; i < params.getBumpCount(); i++)
            bumps.add(randomCapsuleSurfacePoint(rng, width, length));

        float[] meshVerts = new float[verts.size() * 3];
        for (int idx = 0; idx < verts.size(); idx++) {
            float[] v = verts.get(idx);
            double[] p = {v[0], v[1], v[2]};

            double disp = 0;
            for (double[] c : craters) {
                double d = distOnCapsule(p, c, width, length);
                double normD = d / (params.getCraterRadius() * width);
                if (normD < 1.0)
                    disp -= params.getCraterDepth() * width * (1 - normD * normD);
            }
            for (double[] b : bumps) {
                double d = distOnCapsule(p, b, width, length);
                double normD = d / (params.getBumpRadius() * width);
                if (normD < 1.0)
                    disp += params.getBumpHeight() * width * (1 - normD * normD);
            }
            double r0 = Math.sqrt(p[0] * p[0] + p[2] * p[2]);
            double nx = r0 > 1e-6 ? p[0] / r0 : 0, ny = 0, nz = r0 > 1e-6 ? p[2] / r0 : 0;
            if (Math.abs(p[1]) > (length / 2.0) - 1e-2) ny = p[1] > 0 ? 1 : -1;
            meshVerts[idx * 3] = (float) (p[0] + nx * disp);
            meshVerts[idx * 3 + 1] = (float) (p[1] + ny * disp);
            meshVerts[idx * 3 + 2] = (float) (p[2] + nz * disp);
        }

        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().setAll(meshVerts);
        mesh.getTexCoords().addAll(0, 0);
        int[] meshFaces = new int[faces.size() * 6];
        for (int i = 0; i < faces.size(); i++) {
            int[] f = faces.get(i);
            meshFaces[i * 6] = f[0];
            meshFaces[i * 6 + 1] = 0;
            meshFaces[i * 6 + 2] = f[1];
            meshFaces[i * 6 + 3] = 0;
            meshFaces[i * 6 + 4] = f[2];
            meshFaces[i * 6 + 5] = 0;
        }
        mesh.getFaces().setAll(meshFaces);
        mesh.getFaceSmoothingGroups().clear();
        for (int i = 0; i < faces.size(); i++) mesh.getFaceSmoothingGroups().addAll(1);

        return mesh;
    }

    private double[] randomCapsuleSurfacePoint(Random rng, double width, double length) {
        // Randomly select a hemisphere or cylinder
        double t = rng.nextDouble();
        double theta = 2 * Math.PI * rng.nextDouble();
        if (t < 0.25) {
            // Top hemisphere
            double phi = Math.acos(2 * rng.nextDouble() - 1) / 2;
            double y = (length / 2.0) + width * Math.sin(phi);
            double r = width * Math.cos(phi);
            return new double[]{r * Math.cos(theta), y, r * Math.sin(theta)};
        } else if (t < 0.75) {
            // Cylinder
            double y = (length / 2.0) - rng.nextDouble() * length;
            return new double[]{width * Math.cos(theta), y, width * Math.sin(theta)};
        } else {
            // Bottom hemisphere
            double phi = -Math.acos(2 * rng.nextDouble() - 1) / 2;
            double y = -(length / 2.0) + width * Math.sin(phi);
            double r = width * Math.cos(phi);
            return new double[]{r * Math.cos(theta), y, r * Math.sin(theta)};
        }
    }

    private double distOnCapsule(double[] p, double[] c, double width, double length) {
        // Euclidean distance between p and c
        double dx = p[0] - c[0];
        double dy = p[1] - c[1];
        double dz = p[2] - c[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public List<Node> createParameterControls(Consumer<AsteroidParameters> onChange, AsteroidParameters curBase) {
        CapsuleAsteroidParameters cur = (curBase instanceof CapsuleAsteroidParameters)
                ? (CapsuleAsteroidParameters) curBase
                : new CapsuleAsteroidParameters.Builder().build();
        List<Node> controls = new ArrayList<>();

        // Capsule geometry
        Label lengthLabel = new Label("Length:");
        Slider lengthSlider = new Slider(40, 400, cur.getLength());
        lengthSlider.setShowTickLabels(true);

        Label widthLabel = new Label("Width:");
        Slider widthSlider = new Slider(15, 120, cur.getWidth());
        widthSlider.setShowTickLabels(true);

        // Craters
        Label craterCountLabel = new Label("Craters:");
        Spinner<Integer> craterCountSpinner = new Spinner<>();
        craterCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 32, cur.getCraterCount()));

        Label craterDepthLabel = new Label("Crater Depth:");
        Slider craterDepthSlider = new Slider(0.02, 0.45, cur.getCraterDepth());
        craterDepthSlider.setShowTickLabels(true);

        Label craterRadiusLabel = new Label("Crater Radius:");
        Slider craterRadiusSlider = new Slider(0.05, 0.5, cur.getCraterRadius());
        craterRadiusSlider.setShowTickLabels(true);

        // Bumps
        Label bumpCountLabel = new Label("Bumps:");
        Spinner<Integer> bumpCountSpinner = new Spinner<>();
        bumpCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 24, cur.getBumpCount()));

        Label bumpHeightLabel = new Label("Bump Height:");
        Slider bumpHeightSlider = new Slider(0.02, 0.40, cur.getBumpHeight());
        bumpHeightSlider.setShowTickLabels(true);

        Label bumpRadiusLabel = new Label("Bump Radius:");
        Slider bumpRadiusSlider = new Slider(0.03, 0.45, cur.getBumpRadius());
        bumpRadiusSlider.setShowTickLabels(true);

        Consumer<Void> update = unused -> onChange.accept(
                new CapsuleAsteroidParameters.Builder()
                        .radius(cur.getRadius())
                        .subdivisions(cur.getSubdivisions())
                        .deformation(cur.getDeformation())
                        .seed(cur.getSeed())
                        .familyName(cur.getFamilyName())
                        .length(lengthSlider.getValue())
                        .width(widthSlider.getValue())
                        .craterCount(craterCountSpinner.getValue())
                        .craterDepth(craterDepthSlider.getValue())
                        .craterRadius(craterRadiusSlider.getValue())
                        .bumpCount(bumpCountSpinner.getValue())
                        .bumpHeight(bumpHeightSlider.getValue())
                        .bumpRadius(bumpRadiusSlider.getValue())
                        .build());

        lengthSlider.valueProperty().addListener((obs, oldV, newV) -> update.accept(null));
        widthSlider.valueProperty().addListener((obs, oldV, newV) -> update.accept(null));
        craterCountSpinner.valueProperty().addListener((obs, oldV, newV) -> update.accept(null));
        craterDepthSlider.valueProperty().addListener((obs, oldV, newV) -> update.accept(null));
        craterRadiusSlider.valueProperty().addListener((obs, oldV, newV) -> update.accept(null));
        bumpCountSpinner.valueProperty().addListener((obs, oldV, newV) -> update.accept(null));
        bumpHeightSlider.valueProperty().addListener((obs, oldV, newV) -> update.accept(null));
        bumpRadiusSlider.valueProperty().addListener((obs, oldV, newV) -> update.accept(null));

        controls.add(new VBox(4, lengthLabel, lengthSlider, widthLabel, widthSlider));
        controls.add(new VBox(4, craterCountLabel, craterCountSpinner, craterDepthLabel, craterDepthSlider, craterRadiusLabel, craterRadiusSlider));
        controls.add(new VBox(4, bumpCountLabel, bumpCountSpinner, bumpHeightLabel, bumpHeightSlider, bumpRadiusLabel, bumpRadiusSlider));

        return controls;
    }

    @Override
    public String getDisplayName() { return "Capsule"; }
}
