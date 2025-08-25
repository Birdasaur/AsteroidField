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

        List<float[]> verts = new ArrayList<>();
        List<int[]> faces = new ArrayList<>();
        int capStacks = stacks / 2; // hemisphere stacks
        int cols = slices + 1;

        // --- 1. Top cap: unique pole vertex per longitude ---
        int topPoleStart = verts.size();
        double yTop = (length / 2.0) + width;
        for (int j = 0; j < slices; j++) {
            verts.add(new float[]{
                0f,
                (float) yTop,
                0f
            }); // All at same location
        }
        // First ring below top
        int topRingStart = verts.size();
        double phi = Math.PI / 2 - Math.PI / (2 * capStacks);
        double y = (length / 2.0) + width * Math.sin(phi);
        double r = width * Math.cos(phi);
        for (int j = 0; j < cols; j++) {
            double theta = 2 * Math.PI * j / slices;
            verts.add(new float[]{
                (float) (r * Math.cos(theta)),
                (float) y,
                (float) (r * Math.sin(theta))
            });
        }

        // --- 2. Top hemisphere between rings ---
        int prevRingStart = topRingStart;
        for (int i = 1; i < capStacks; i++) {
            int thisRingStart = verts.size();
            phi = Math.PI / 2 - Math.PI * i / (2 * capStacks);
            y = (length / 2.0) + width * Math.sin(phi);
            r = width * Math.cos(phi);
            for (int j = 0; j < cols; j++) {
                double theta = 2 * Math.PI * j / slices;
                verts.add(new float[]{
                    (float) (r * Math.cos(theta)),
                    (float) y,
                    (float) (r * Math.sin(theta))
                });
            }
            // Connect prev ring to this ring
            for (int j = 0; j < slices; j++) {
                int a = prevRingStart + j;
                int b = prevRingStart + j + 1;
                int c = thisRingStart + j;
                int d = thisRingStart + j + 1;
                faces.add(new int[]{a, c, b});
                faces.add(new int[]{b, c, d});
            }
            prevRingStart = thisRingStart;
        }

        // --- 3. Cylinder ---
        int cylRings = stacks - 1;
        for (int i = 0; i < cylRings; i++) {
            int thisRingStart = verts.size();
            y = (length / 2.0) - ((i + 1.0) / stacks) * length;
            for (int j = 0; j < cols; j++) {
                double theta = 2 * Math.PI * j / slices;
                verts.add(new float[]{
                    (float) (width * Math.cos(theta)),
                    (float) y,
                    (float) (width * Math.sin(theta))
                });
            }
            // Connect prev ring to this ring
            for (int j = 0; j < slices; j++) {
                int a = prevRingStart + j;
                int b = prevRingStart + j + 1;
                int c = thisRingStart + j;
                int d = thisRingStart + j + 1;
                faces.add(new int[]{a, c, b});
                faces.add(new int[]{b, c, d});
            }
            prevRingStart = thisRingStart;
        }

        // --- 4. Bottom hemisphere ---
        for (int i = 1; i <= capStacks; i++) {
            int thisRingStart = verts.size();
            phi = -Math.PI * i / (2 * capStacks);
            y = -(length / 2.0) + width * Math.sin(phi);
            r = width * Math.cos(phi);
            for (int j = 0; j < cols; j++) {
                double theta = 2 * Math.PI * j / slices;
                verts.add(new float[]{
                    (float) (r * Math.cos(theta)),
                    (float) y,
                    (float) (r * Math.sin(theta))
                });
            }
            // Connect prev ring to this ring
            for (int j = 0; j < slices; j++) {
                int a = prevRingStart + j;
                int b = prevRingStart + j + 1;
                int c = thisRingStart + j;
                int d = thisRingStart + j + 1;
                faces.add(new int[]{a, c, b});
                faces.add(new int[]{b, c, d});
            }
            prevRingStart = thisRingStart;
        }

        // --- 5. Bottom cap: unique pole vertex per longitude ---
        int botPoleStart = verts.size();
        double yBot = -(length / 2.0) - width;
        for (int j = 0; j < slices; j++) {
            verts.add(new float[]{
                0f,
                (float) yBot,
                0f
            });
        }
        int lastRingStart = prevRingStart;
        // Connect last ring to each bottom pole vertex
        for (int j = 0; j < slices; j++) {
            int pole = botPoleStart + j;
            int a = lastRingStart + j;
            int b = lastRingStart + j + 1;
            // (Winding order may need to be [a, b, pole] or [b, a, pole] depending on orientation)
            faces.add(new int[]{a, b, pole});
//            faces.add(new int[]{b, a, pole});
        }

        // Connect top pole to top ring
        for (int j = 0; j < slices; j++) {
            int pole = topPoleStart + j;
            int a = topRingStart + j;
            int b = topRingStart + j + 1;
            faces.add(new int[]{pole, a, b});
//            faces.add(new int[]{pole, b, a});
        }

        //Bumps/Crater deformation 
        Random rng = new Random(params.getSeed());
        List<double[]> craters = new ArrayList<>();
        for (int i = 0; i < params.getCraterCount(); i++) {
            craters.add(randomCapsuleSurfacePoint(rng, width, length));
        }
        List<double[]> bumps = new ArrayList<>();
        for (int i = 0; i < params.getBumpCount(); i++) {
            bumps.add(randomCapsuleSurfacePoint(rng, width, length));
        }

        float[] meshVerts = new float[verts.size() * 3];
        for (int idx = 0; idx < verts.size(); idx++) {
            float[] v = verts.get(idx);
            double[] p = {v[0], v[1], v[2]};
            double disp = 0;
            for (double[] c : craters) {
                double d = distOnCapsule(p, c, width, length);
                double normD = d / (params.getCraterRadius() * width);
                if (normD < 1.0) {
                    disp -= params.getCraterDepth() * width * (1 - normD * normD);
                }
            }
            for (double[] b : bumps) {
                double d = distOnCapsule(p, b, width, length);
                double normD = d / (params.getBumpRadius() * width);
                if (normD < 1.0) {
                    disp += params.getBumpHeight() * width * (1 - normD * normD);
                }
            }
            double r0 = Math.sqrt(p[0] * p[0] + p[2] * p[2]);
            double nx = r0 > 1e-6 ? p[0] / r0 : 0, ny = 0, nz = r0 > 1e-6 ? p[2] / r0 : 0;
            if (Math.abs(p[1]) > (length / 2.0) - 1e-2) {
                ny = p[1] > 0 ? 1 : -1;
            }
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
        for (int i = 0; i < faces.size(); i++) {
            mesh.getFaceSmoothingGroups().addAll(1);
        }

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
    public String getDisplayName() {
        return "Capsule";
    }
}
