package AsteroidField;

import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.shape.TriangleMesh;
import java.util.function.Consumer;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

public class SpikyAsteroidMeshProvider implements AsteroidMeshProvider, AsteroidFamilyUI {

    private Spinner<Integer> spikeCountSpinner;
    private Slider spikeLengthSlider, spikeWidthSlider, randomnessSlider, spikeSpacingJitterSlider;
    private Consumer<AsteroidParameters> onChangeCallback;
    private SpikyAsteroidParameters lastParams = null;

    @Override
    public TriangleMesh generateMesh(AsteroidParameters baseParams) {
        SpikyAsteroidParameters params = (SpikyAsteroidParameters) baseParams;
        SpikyMesh mesh = new SpikyMesh(params.getRadius(), params.getSubdivisions(), params);
        return mesh;
    }

    @Override
    public Node createDynamicControls(AsteroidParameters startParams, Consumer<AsteroidParameters> onChange) {
        this.onChangeCallback = onChange;
        SpikyAsteroidParameters cur = (startParams instanceof SpikyAsteroidParameters)
                ? (SpikyAsteroidParameters) startParams
                : (SpikyAsteroidParameters) getDefaultParameters();
        lastParams = cur;

        spikeCountSpinner = new Spinner<>();
        spikeCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 200, cur.getSpikeCount()));
        spikeLengthSlider = new Slider(0.0, 3.0, cur.getSpikeLength());
        spikeLengthSlider.setShowTickLabels(true);
        spikeWidthSlider = new Slider(0.01, 0.5, cur.getSpikeWidth());
        spikeWidthSlider.setShowTickLabels(true);
        randomnessSlider = new Slider(0, 1.0, cur.getRandomness());
        randomnessSlider.setShowTickLabels(true);
        spikeSpacingJitterSlider = new Slider(0, 0.5, cur.getSpikeSpacingJitter());
        spikeSpacingJitterSlider.setShowTickLabels(true);

        Consumer<Void> update = unused -> {
            lastParams = buildUpdatedParamsFromControls();
            if (onChangeCallback != null) {
                onChangeCallback.accept(lastParams);
            }
        };

        spikeCountSpinner.valueProperty().addListener((obs, oldV, newV) -> update.accept(null));
        spikeLengthSlider.valueProperty().addListener((obs, oldV, newV) -> update.accept(null));
        spikeWidthSlider.valueProperty().addListener((obs, oldV, newV) -> update.accept(null));
        randomnessSlider.valueProperty().addListener((obs, oldV, newV) -> update.accept(null));
        spikeSpacingJitterSlider.valueProperty().addListener((obs, oldV, newV) -> update.accept(null));

        VBox controls = new VBox(
                new VBox(5, new Label("Spike Count:"), spikeCountSpinner),
                new VBox(5, new Label("Spike Length:"), spikeLengthSlider),
                new VBox(5, new Label("Spike Width:"), spikeWidthSlider),
                new VBox(5, new Label("Randomness:"), randomnessSlider),
                new VBox(5, new Label("Spike Spacing Jitter:"), spikeSpacingJitterSlider)
        );
        setControlsFromParams(cur);
        return controls;
    }

    private SpikyAsteroidParameters buildUpdatedParamsFromControls() {
        return new SpikyAsteroidParameters.Builder()
                .radius(lastParams != null ? lastParams.getRadius() : 100)
                .subdivisions(lastParams != null ? lastParams.getSubdivisions() : 2)
                .deformation(lastParams != null ? lastParams.getDeformation() : 0.3)
                .seed(lastParams != null ? lastParams.getSeed() : System.nanoTime())
                .familyName("Spiky")
                .spikeCount(spikeCountSpinner.getValue())
                .spikeLength(spikeLengthSlider.getValue())
                .spikeWidth(spikeWidthSlider.getValue())
                .randomness(randomnessSlider.getValue())
                .spikeSpacingJitter(spikeSpacingJitterSlider.getValue())
                .build();
    }

    @Override
    public void setControlsFromParams(AsteroidParameters params) {
        if (!(params instanceof SpikyAsteroidParameters)) {
            return;
        }
        SpikyAsteroidParameters c = (SpikyAsteroidParameters) params;
        if (spikeCountSpinner != null) {
            spikeCountSpinner.getValueFactory().setValue(c.getSpikeCount());
        }
        if (spikeLengthSlider != null) {
            spikeLengthSlider.setValue(c.getSpikeLength());
        }
        if (spikeWidthSlider != null) {
            spikeWidthSlider.setValue(c.getSpikeWidth());
        }
        if (randomnessSlider != null) {
            randomnessSlider.setValue(c.getRandomness());
        }
        if (spikeSpacingJitterSlider != null) {
            spikeSpacingJitterSlider.setValue(c.getSpikeSpacingJitter());
        }
        lastParams = c;
    }

    @Override
    public AsteroidParameters getParamsFromControls() {
        return buildUpdatedParamsFromControls();
    }

    @Override
    public AsteroidParameters getDefaultParameters() {
        return new SpikyAsteroidParameters.Builder()
                .radius(100).subdivisions(2).deformation(0.3).seed(System.nanoTime()).familyName("Spiky")
                .spikeCount(4).spikeLength(1.5).spikeWidth(0.25).randomness(0.4).spikeSpacingJitter(0.1)
                .build();
    }

    @Override
    public AsteroidParameters buildDefaultParamsFrom(AsteroidParameters previous) {
        return new SpikyAsteroidParameters.Builder()
                .radius(previous.getRadius())
                .subdivisions(previous.getSubdivisions())
                .deformation(previous.getDeformation())
                .seed(System.nanoTime())
                .familyName("Spiky")
                .spikeCount(4).spikeLength(1.5).spikeWidth(0.25).randomness(0.4).spikeSpacingJitter(0.1)
                .build();
    }

    @Override
    public String getDisplayName() {
        return "Spiky";
    }
}
