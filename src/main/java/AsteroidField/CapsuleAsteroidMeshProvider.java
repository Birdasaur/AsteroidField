package AsteroidField;

import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.shape.TriangleMesh;
import java.util.*;
import java.util.function.Consumer;

public class CapsuleAsteroidMeshProvider implements AsteroidMeshProvider, AsteroidFamilyUI {

    // --- UI controls ---
    private Slider lengthSlider, widthSlider;
    private Spinner<Integer> craterCountSpinner;
    private Slider craterDepthSlider, craterRadiusSlider;
    private Spinner<Integer> bumpCountSpinner;
    private Slider bumpHeightSlider, bumpRadiusSlider;
    private Consumer<AsteroidParameters> onChangeCallback;
    // For parameter state persistence
    private CapsuleAsteroidParameters lastParams = null;
    private long lastRandomSeed = System.nanoTime();

    // --- MESH GENERATION ---
    @Override
    public TriangleMesh generateMesh(AsteroidParameters base) {
        CapsuleAsteroidParameters params = (CapsuleAsteroidParameters) base;
        int slices = Math.max(12, params.getSubdivisions() * 8);
        int stacks = Math.max(8, params.getSubdivisions() * 4);
        double width = params.getWidth();
        double length = params.getLength();
        CapsuleMesh mesh = new CapsuleMesh(slices, stacks, width, length);
        // Now, deform mesh points directly using mesh.getVertsList() and mesh.getVertsArray()
        return deformCapsuleMesh(mesh, params);
    }

    public TriangleMesh deformCapsuleMesh(CapsuleMesh mesh, CapsuleAsteroidParameters params){
        double width = params.getWidth();
        double length = params.getLength();

        float[] meshVerts = mesh.getVertsArray();
        List<float[]> verts = mesh.getVertsList();
        List<double[]> craters = params.getCraterCenters();
        List<double[]> bumps = params.getBumpCenters();
        if (craters == null) {
            craters = Collections.emptyList();
        }
        if (bumps == null) {
            bumps = Collections.emptyList();
        }

        // Deform mesh as before
        for (int idx = 0; idx < verts.size(); idx++) {
            float[] v = verts.get(idx);
            double[] p = {v[0], v[1], v[2]};
            double disp = 0;
            for (double[] c : craters) {
                double d = CapsuleMesh.distOnCapsule(p, c, width, length);
                double normD = d / (params.getCraterRadius() * width);
                if (normD < 1.0) {
                    disp -= params.getCraterDepth() * width * (1 - normD * normD);
                }
            }
            for (double[] b : bumps) {
                double d = CapsuleMesh.distOnCapsule(p, b, width, length);
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
        mesh.getPoints().setAll(meshVerts);
        return mesh;        
    }

    private List<double[]> generateFeatureCenters(int count, double width, double length, long seed) {
        List<double[]> centers = new ArrayList<>(count);
        Random rng = new Random(seed);
        for (int i = 0; i < count; i++) {
            centers.add(CapsuleMesh.randomCapsuleSurfacePoint(rng, width, length));
        }
        return centers;
    }

    // --- UI LOGIC ---
    @Override
    public Node createDynamicControls(AsteroidParameters startParams, Consumer<AsteroidParameters> onChange) {
        this.onChangeCallback = onChange;
        CapsuleAsteroidParameters cur = (startParams instanceof CapsuleAsteroidParameters)
                ? (CapsuleAsteroidParameters) startParams
                : (CapsuleAsteroidParameters) getDefaultParameters();
        lastParams = cur;

        // Build controls as before ...
        lengthSlider = new Slider(40, 400, cur.getLength());
        widthSlider = new Slider(15, 120, cur.getWidth());
        craterCountSpinner = new Spinner<>();
        craterCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 32, cur.getCraterCount()));
        craterDepthSlider = new Slider(0.02, 0.45, cur.getCraterDepth());
        craterRadiusSlider = new Slider(0.05, 0.5, cur.getCraterRadius());
        bumpCountSpinner = new Spinner<>();
        bumpCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 24, cur.getBumpCount()));
        bumpHeightSlider = new Slider(0.02, 0.40, cur.getBumpHeight());
        bumpRadiusSlider = new Slider(0.03, 0.45, cur.getBumpRadius());

        // Attach listeners
        Consumer<Void> update = unused -> {
            lastParams = buildUpdatedParamsFromControls(lastParams, false);
            if (onChangeCallback != null) {
                onChangeCallback.accept(lastParams);
            }
        };

        lengthSlider.valueProperty().addListener((obs, oldV, newV) -> update.accept(null));
        widthSlider.valueProperty().addListener((obs, oldV, newV) -> update.accept(null));
        craterCountSpinner.valueProperty().addListener((obs, oldV, newV) -> update.accept(null));
        craterDepthSlider.valueProperty().addListener((obs, oldV, newV) -> update.accept(null));
        craterRadiusSlider.valueProperty().addListener((obs, oldV, newV) -> update.accept(null));
        bumpCountSpinner.valueProperty().addListener((obs, oldV, newV) -> update.accept(null));
        bumpHeightSlider.valueProperty().addListener((obs, oldV, newV) -> update.accept(null));
        bumpRadiusSlider.valueProperty().addListener((obs, oldV, newV) -> update.accept(null));

        Button randomizeBtn = new Button("Randomize Features");
        randomizeBtn.setOnAction(e -> {
            lastRandomSeed = System.nanoTime();
            lastParams = buildUpdatedParamsFromControls(lastParams, true);
            if (onChangeCallback != null) {
                onChangeCallback.accept(lastParams);
            }
        });

        VBox controls = new VBox(
                new VBox(4, new Label("Length:"), lengthSlider, new Label("Width:"), widthSlider),
                new VBox(4, new Label("Craters:"), craterCountSpinner, new Label("Crater Depth:"), craterDepthSlider, new Label("Crater Radius:"), craterRadiusSlider),
                new VBox(4, new Label("Bumps:"), bumpCountSpinner, new Label("Bump Height:"), bumpHeightSlider, new Label("Bump Radius:"), bumpRadiusSlider),
                randomizeBtn
        );
        setControlsFromParams(cur);
        return controls;
    }

    /**
     * When a param changes, update persistent locations or randomize as
     * requested.
     */
    private CapsuleAsteroidParameters buildUpdatedParamsFromControls(CapsuleAsteroidParameters prev, boolean randomize) {
        int craterCount = craterCountSpinner.getValue();
        int bumpCount = bumpCountSpinner.getValue();
        double width = widthSlider.getValue();
        double length = lengthSlider.getValue();

        // ---- CRATERS ----
        List<double[]> prevCraterCenters = (prev != null) ? prev.getCraterCenters() : null;
        List<double[]> newCraterCenters = new ArrayList<>();
        if (randomize || prevCraterCenters == null) {
            // New random locations for all
            newCraterCenters = generateFeatureCenters(craterCount, width, length, System.nanoTime());
        } else {
            // Reuse old, add new random for extras
            int keep = Math.min(craterCount, prevCraterCenters.size());
            for (int i = 0; i < keep; i++) {
                newCraterCenters.add(prevCraterCenters.get(i));
            }
            for (int i = keep; i < craterCount; i++) {
                // Use unique random seed for each new center
                newCraterCenters.add(CapsuleMesh.randomCapsuleSurfacePoint(new Random(System.nanoTime() + i), width, length));
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                } // ensure unique nanoTime
            }
        }

        // ---- BUMPS ----
        List<double[]> prevBumpCenters = (prev != null) ? prev.getBumpCenters() : null;
        List<double[]> newBumpCenters = new ArrayList<>();
        if (randomize || prevBumpCenters == null) {
            newBumpCenters = generateFeatureCenters(bumpCount, width, length, System.nanoTime() + 123456);
        } else {
            int keep = Math.min(bumpCount, prevBumpCenters.size());
            for (int i = 0; i < keep; i++) {
                newBumpCenters.add(prevBumpCenters.get(i));
            }
            for (int i = keep; i < bumpCount; i++) {
                newBumpCenters.add(CapsuleMesh.randomCapsuleSurfacePoint(new Random(System.nanoTime() + i + 50000), width, length));
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                }
            }
        }

        return new CapsuleAsteroidParameters.Builder()
                .radius(prev != null ? prev.getRadius() : 100)
                .subdivisions(prev != null ? prev.getSubdivisions() : 2)
                .deformation(prev != null ? prev.getDeformation() : 0.3)
                .seed(prev != null ? prev.getSeed() : System.nanoTime())
                .familyName("Capsule")
                .length(length)
                .width(width)
                .craterCount(craterCount)
                .craterDepth(craterDepthSlider.getValue())
                .craterRadius(craterRadiusSlider.getValue())
                .bumpCount(bumpCount)
                .bumpHeight(bumpHeightSlider.getValue())
                .bumpRadius(bumpRadiusSlider.getValue())
                .craterCenters(newCraterCenters)
                .bumpCenters(newBumpCenters)
                .build();
    }

    @Override
    public void setControlsFromParams(AsteroidParameters params) {
        if (!(params instanceof CapsuleAsteroidParameters)) {
            return;
        }
        CapsuleAsteroidParameters c = (CapsuleAsteroidParameters) params;
        if (lengthSlider != null) {
            lengthSlider.setValue(c.getLength());
        }
        if (widthSlider != null) {
            widthSlider.setValue(c.getWidth());
        }
        if (craterCountSpinner != null) {
            craterCountSpinner.getValueFactory().setValue(c.getCraterCount());
        }
        if (craterDepthSlider != null) {
            craterDepthSlider.setValue(c.getCraterDepth());
        }
        if (craterRadiusSlider != null) {
            craterRadiusSlider.setValue(c.getCraterRadius());
        }
        if (bumpCountSpinner != null) {
            bumpCountSpinner.getValueFactory().setValue(c.getBumpCount());
        }
        if (bumpHeightSlider != null) {
            bumpHeightSlider.setValue(c.getBumpHeight());
        }
        if (bumpRadiusSlider != null) {
            bumpRadiusSlider.setValue(c.getBumpRadius());
        }
        lastParams = c;
    }

    @Override
    public AsteroidParameters getParamsFromControls() {
        // Always update from the lastParams' crater/bump lists unless counts increase.
        return buildUpdatedParamsFromControls(lastParams, false);
    }

    @Override
    public AsteroidParameters getDefaultParameters() {
        long now = System.nanoTime();
        return new CapsuleAsteroidParameters.Builder()
                .radius(100).subdivisions(2).deformation(0.3).seed(now).familyName("Capsule")
                .length(200).width(60)
                .craterCount(5).craterDepth(0.25).craterRadius(0.2)
                .bumpCount(4).bumpHeight(0.22).bumpRadius(0.18)
                .craterCenters(generateFeatureCenters(5, 60, 200, now))
                .bumpCenters(generateFeatureCenters(4, 60, 200, now + 1))
                .build();
    }

    @Override
    public AsteroidParameters buildDefaultParamsFrom(AsteroidParameters previous) {
        long now = System.nanoTime();
        double width = (previous instanceof CapsuleAsteroidParameters) ? ((CapsuleAsteroidParameters) previous).getWidth() : 60;
        double length = (previous instanceof CapsuleAsteroidParameters) ? ((CapsuleAsteroidParameters) previous).getLength() : 200;
        int craterCount = (previous instanceof CapsuleAsteroidParameters) ? ((CapsuleAsteroidParameters) previous).getCraterCount() : 5;
        int bumpCount = (previous instanceof CapsuleAsteroidParameters) ? ((CapsuleAsteroidParameters) previous).getBumpCount() : 4;
        return new CapsuleAsteroidParameters.Builder()
                .radius(previous.getRadius())
                .subdivisions(previous.getSubdivisions())
                .deformation(previous.getDeformation())
                .seed(now)
                .familyName("Capsule")
                .length(length)
                .width(width)
                .craterCount(craterCount).craterDepth(0.25).craterRadius(0.2)
                .bumpCount(bumpCount).bumpHeight(0.22).bumpRadius(0.18)
                .craterCenters(generateFeatureCenters(craterCount, width, length, now))
                .bumpCenters(generateFeatureCenters(bumpCount, width, length, now + 1))
                .build();
    }

    @Override
    public String getDisplayName() {
        return "Capsule";
    }
}
