package AsteroidField.asteroids.providers;

import AsteroidField.asteroids.AsteroidFamilyUI;
import AsteroidField.asteroids.parameters.AsteroidParameters;
import AsteroidField.asteroids.parameters.CrateredAsteroidParameters;
import AsteroidField.asteroids.geometry.CrateredMesh;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.shape.TriangleMesh;
import java.util.function.Consumer;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

public class CrateredAsteroidMeshProvider implements AsteroidMeshProvider, AsteroidFamilyUI {
    // Dynamic control fields/state
    private Spinner<Integer> craterCountSpinner;
    private Slider craterDepthSlider, craterWidthSlider;
    private Consumer<AsteroidParameters> onChangeCallback;
    private CrateredAsteroidParameters lastParams = null;
    private long lastRandomSeed = System.nanoTime();

    @Override
    public TriangleMesh generateMesh(AsteroidParameters baseParams) {
        CrateredAsteroidParameters params = (CrateredAsteroidParameters) baseParams;
        CrateredMesh mesh = new CrateredMesh(params.getRadius(), params.getSubdivisions(), params);
        return mesh;
    }

    private List<double[]> generateCraterCenters(int count, long seed) {
        List<double[]> centers = new ArrayList<>(count);
        Random rng = new Random(seed);
        for (int i = 0; i < count; i++) {
            double theta = 2 * Math.PI * rng.nextDouble();
            double phi = Math.acos(2 * rng.nextDouble() - 1);
            double x = Math.sin(phi) * Math.cos(theta);
            double y = Math.sin(phi) * Math.sin(theta);
            double z = Math.cos(phi);
            centers.add(new double[]{x, y, z});
        }
        return centers;
    }

    @Override
    public Node createDynamicControls(AsteroidParameters startParams, Consumer<AsteroidParameters> onChange) {
        this.onChangeCallback = onChange;
        CrateredAsteroidParameters cur = (startParams instanceof CrateredAsteroidParameters)
                ? (CrateredAsteroidParameters) startParams
                : (CrateredAsteroidParameters) getDefaultParameters();
        lastParams = cur;

        craterCountSpinner = new Spinner<>();
        craterCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, cur.getCraterCount()));
        craterDepthSlider = new Slider(0.05, 0.5, cur.getCraterDepth());
        craterDepthSlider.setShowTickLabels(true);
        craterWidthSlider = new Slider(0.05, 0.6, cur.getCraterWidth());
        craterWidthSlider.setShowTickLabels(true);

        Consumer<Void> update = unused -> {
            lastParams = buildUpdatedParamsFromControls(lastParams, false);
            if (onChangeCallback != null) {
                onChangeCallback.accept(lastParams);
            }
        };

        craterCountSpinner.valueProperty().addListener((obs, oldV, newV) -> update.accept(null));
        craterDepthSlider.valueProperty().addListener((obs, oldV, newV) -> update.accept(null));
        craterWidthSlider.valueProperty().addListener((obs, oldV, newV) -> update.accept(null));

        Button randomizeBtn = new Button("Randomize Craters");
        randomizeBtn.setOnAction(e -> {
            lastRandomSeed = System.nanoTime();
            lastParams = buildUpdatedParamsFromControls(lastParams, true);
            if (onChangeCallback != null) {
                onChangeCallback.accept(lastParams);
            }
        });

        VBox controls = new VBox(
                new VBox(5, new Label("Crater Count:"), craterCountSpinner),
                new VBox(5, new Label("Crater Depth:"), craterDepthSlider),
                new VBox(5, new Label("Crater Width:"), craterWidthSlider),
                randomizeBtn
        );
        setControlsFromParams(cur);
        return controls;
    }

    private CrateredAsteroidParameters buildUpdatedParamsFromControls(CrateredAsteroidParameters prev, boolean randomize) {
        int craterCount = craterCountSpinner.getValue();
        List<double[]> prevCraterCenters = (prev != null) ? prev.getCraterCenters() : null;
        List<double[]> newCraterCenters = new ArrayList<>();
        if (randomize || prevCraterCenters == null) {
            newCraterCenters = generateCraterCenters(craterCount, System.nanoTime());
        } else {
            int keep = Math.min(craterCount, prevCraterCenters.size());
            for (int i = 0; i < keep; i++) {
                newCraterCenters.add(prevCraterCenters.get(i));
            }
            for (int i = keep; i < craterCount; i++) {
                newCraterCenters.add(generateCraterCenters(1, System.nanoTime() + i).get(0));
                try { Thread.sleep(1); } catch (InterruptedException e) {}
            }
        }

        return new CrateredAsteroidParameters.Builder()
                .radius(prev != null ? prev.getRadius() : 100)
                .subdivisions(prev != null ? prev.getSubdivisions() : 2)
                .deformation(prev != null ? prev.getDeformation() : 0.3)
                .seed(prev != null ? prev.getSeed() : System.nanoTime())
                .familyName("Cratered")
                .craterCount(craterCount)
                .craterDepth(craterDepthSlider.getValue())
                .craterWidth(craterWidthSlider.getValue())
                .craterCenters(newCraterCenters)
                .build();
    }

    @Override
    public void setControlsFromParams(AsteroidParameters params) {
        if (!(params instanceof CrateredAsteroidParameters)) return;
        CrateredAsteroidParameters c = (CrateredAsteroidParameters) params;
        if (craterCountSpinner != null) craterCountSpinner.getValueFactory().setValue(c.getCraterCount());
        if (craterDepthSlider != null) craterDepthSlider.setValue(c.getCraterDepth());
        if (craterWidthSlider != null) craterWidthSlider.setValue(c.getCraterWidth());
        lastParams = c;
    }

    @Override
    public AsteroidParameters getParamsFromControls() {
        return buildUpdatedParamsFromControls(lastParams, false);
    }

    @Override
    public AsteroidParameters getDefaultParameters() {
        long now = System.nanoTime();
        return new CrateredAsteroidParameters.Builder()
                .radius(100).subdivisions(2).deformation(0.3).seed(now).familyName("Cratered")
                .craterCount(5).craterDepth(0.2).craterWidth(0.2)
                .craterCenters(generateCraterCenters(5, now))
                .build();
    }

    @Override
    public AsteroidParameters buildDefaultParamsFrom(AsteroidParameters previous) {
        long now = System.nanoTime();
        int craterCount = (previous instanceof CrateredAsteroidParameters)
                ? ((CrateredAsteroidParameters) previous).getCraterCount()
                : 5;
        return new CrateredAsteroidParameters.Builder()
                .radius(previous.getRadius())
                .subdivisions(previous.getSubdivisions())
                .deformation(previous.getDeformation())
                .seed(now)
                .familyName("Cratered")
                .craterCount(craterCount)
                .craterDepth(0.2)
                .craterWidth(0.2)
                .craterCenters(generateCraterCenters(craterCount, now))
                .build();
    }

    @Override
    public String getDisplayName() { return "Cratered"; }
}
