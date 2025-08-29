package AsteroidField;

import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.shape.TriangleMesh;
import java.util.function.Consumer;
import javafx.scene.control.*;
import javafx.beans.value.ChangeListener;

public class CrystallineAsteroidMeshProvider implements AsteroidMeshProvider, AsteroidFamilyUI {

    // GUI controls
    private Spinner<Integer> crystalCountSpinner, prismSidesSpinner, maxClusterSizeSpinner, clusterSpreadSpinner;
    private CheckBox capBaseCheck, capTipCheck;
    private Slider minCrystalLengthSlider, maxCrystalLengthSlider;
    private Slider minCrystalRadiusSlider, maxCrystalRadiusSlider;
    private Slider tipRadiusScaleSlider, maxTiltAngleSlider, lengthJitterSlider, radiusJitterSlider, embedDepthSlider;
    private Consumer<AsteroidParameters> onChangeCallback;
    private CrystallineAsteroidParameters lastParams = null;

    @Override
    public TriangleMesh generateMesh(AsteroidParameters baseParams) {
        CrystallineAsteroidParameters params = (CrystallineAsteroidParameters) baseParams;
        return new CrystallineMesh(params.getRadius(), params.getSubdivisions(), params);
    }

    @Override
    public Node createDynamicControls(AsteroidParameters startParams, Consumer<AsteroidParameters> onChange) {
        this.onChangeCallback = onChange;
        CrystallineAsteroidParameters cur = (startParams instanceof CrystallineAsteroidParameters)
                ? (CrystallineAsteroidParameters) startParams
                : (CrystallineAsteroidParameters) getDefaultParameters();
        lastParams = cur;

        // Build controls
        crystalCountSpinner = new Spinner<>(1, 200, cur.getCrystalCount());
        prismSidesSpinner = new Spinner<>(3, 16, cur.getPrismSides());
        maxClusterSizeSpinner = new Spinner<>(1, 12, cur.getMaxClusterSize());
        clusterSpreadSpinner = new Spinner<>(1, 6, cur.getClusterSpread());

        capBaseCheck = new CheckBox("Cap Base");
        capBaseCheck.setSelected(cur.isCapBase());
        capTipCheck = new CheckBox("Cap Tip");
        capTipCheck.setSelected(cur.isCapTip());

        minCrystalLengthSlider = new Slider(0.01, 0.2, cur.getMinCrystalLength());
        maxCrystalLengthSlider = new Slider(0.01, 0.3, cur.getMaxCrystalLength());
        minCrystalRadiusSlider = new Slider(0.003, 0.07, cur.getMinCrystalRadius());
        maxCrystalRadiusSlider = new Slider(0.003, 0.13, cur.getMaxCrystalRadius());
        tipRadiusScaleSlider = new Slider(0.0, 1.0, cur.getTipRadiusScale());
        maxTiltAngleSlider = new Slider(0.0, Math.toRadians(30.0), cur.getMaxTiltAngleRadians());
        lengthJitterSlider = new Slider(0.0, 0.5, cur.getLengthJitter());
        radiusJitterSlider = new Slider(0.0, 0.2, cur.getRadiusJitter());
        embedDepthSlider = new Slider(0.0, 0.4, cur.getEmbedDepth()); // NEW

        minCrystalLengthSlider.setShowTickLabels(true);
        maxCrystalLengthSlider.setShowTickLabels(true);
        minCrystalRadiusSlider.setShowTickLabels(true);
        maxCrystalRadiusSlider.setShowTickLabels(true);
        tipRadiusScaleSlider.setShowTickLabels(true);
        maxTiltAngleSlider.setShowTickLabels(true);
        lengthJitterSlider.setShowTickLabels(true);
        radiusJitterSlider.setShowTickLabels(true);
        embedDepthSlider.setShowTickLabels(true);

        ChangeListener<Object> update = (obs, oldV, newV) -> fireParamsChanged();

        crystalCountSpinner.valueProperty().addListener(update);
        prismSidesSpinner.valueProperty().addListener(update);
        maxClusterSizeSpinner.valueProperty().addListener(update);
        clusterSpreadSpinner.valueProperty().addListener(update);
        capBaseCheck.selectedProperty().addListener(update);
        capTipCheck.selectedProperty().addListener(update);
        minCrystalLengthSlider.valueProperty().addListener(update);
        maxCrystalLengthSlider.valueProperty().addListener(update);
        minCrystalRadiusSlider.valueProperty().addListener(update);
        maxCrystalRadiusSlider.valueProperty().addListener(update);
        tipRadiusScaleSlider.valueProperty().addListener(update);
        maxTiltAngleSlider.valueProperty().addListener(update);
        lengthJitterSlider.valueProperty().addListener(update);
        radiusJitterSlider.valueProperty().addListener(update);
        embedDepthSlider.valueProperty().addListener(update);

        VBox controls = new VBox(
            new VBox(5, new Label("Crystal Count:"), crystalCountSpinner),
            new VBox(5, new Label("Prism Sides:"), prismSidesSpinner),
            new VBox(5, new Label("Cluster Size:"), maxClusterSizeSpinner),
            new VBox(5, new Label("Cluster Spread:"), clusterSpreadSpinner),
            new VBox(5, capBaseCheck, capTipCheck),
            new VBox(5, new Label("Min Crystal Length (×core radius):"), minCrystalLengthSlider),
            new VBox(5, new Label("Max Crystal Length (×core radius):"), maxCrystalLengthSlider),
            new VBox(5, new Label("Min Crystal Radius (×core radius):"), minCrystalRadiusSlider),
            new VBox(5, new Label("Max Crystal Radius (×core radius):"), maxCrystalRadiusSlider),
            new VBox(5, new Label("Tip Radius Scale:"), tipRadiusScaleSlider),
            new VBox(5, new Label("Max Tilt Angle (radians):"), maxTiltAngleSlider),
            new VBox(5, new Label("Length Jitter:"), lengthJitterSlider),
            new VBox(5, new Label("Radius Jitter:"), radiusJitterSlider),
            new VBox(5, new Label("Embed Depth (×core radius):"), embedDepthSlider) // NEW
        );
        setControlsFromParams(cur);
        return controls;
    }

    private void fireParamsChanged() {
        lastParams = buildUpdatedParamsFromControls();
        if (onChangeCallback != null) {
            onChangeCallback.accept(lastParams);
        }
    }

    private CrystallineAsteroidParameters buildUpdatedParamsFromControls() {
        double minLen = minCrystalLengthSlider.getValue();
        double maxLen = Math.max(minLen, maxCrystalLengthSlider.getValue());
        double minRad = minCrystalRadiusSlider.getValue();
        double maxRad = Math.max(minRad, maxCrystalRadiusSlider.getValue());
        return new CrystallineAsteroidParameters.Builder()
            .radius(lastParams != null ? lastParams.getRadius() : 100)
            .subdivisions(lastParams != null ? lastParams.getSubdivisions() : 2)
            .deformation(lastParams != null ? lastParams.getDeformation() : 0.3)
            .seed(lastParams != null ? lastParams.getSeed() : System.nanoTime())
            .familyName("Crystalline")
            .crystalCount(crystalCountSpinner.getValue())
            .prismSides(prismSidesSpinner.getValue())
            .capBase(capBaseCheck.isSelected())
            .capTip(capTipCheck.isSelected())
            .maxClusterSize(maxClusterSizeSpinner.getValue())
            .clusterSpread(clusterSpreadSpinner.getValue())
            .minCrystalLength(minLen)
            .maxCrystalLength(maxLen)
            .minCrystalRadius(minRad)
            .maxCrystalRadius(maxRad)
            .tipRadiusScale(tipRadiusScaleSlider.getValue())
            .maxTiltAngleRadians(maxTiltAngleSlider.getValue())
            .lengthJitter(lengthJitterSlider.getValue())
            .radiusJitter(radiusJitterSlider.getValue())
            .embedDepth(embedDepthSlider.getValue())
            .build();
    }

    @Override
    public void setControlsFromParams(AsteroidParameters params) {
        if (!(params instanceof CrystallineAsteroidParameters)) return;
        CrystallineAsteroidParameters c = (CrystallineAsteroidParameters) params;
        if (crystalCountSpinner != null) crystalCountSpinner.getValueFactory().setValue(c.getCrystalCount());
        if (prismSidesSpinner != null) prismSidesSpinner.getValueFactory().setValue(c.getPrismSides());
        if (maxClusterSizeSpinner != null) maxClusterSizeSpinner.getValueFactory().setValue(c.getMaxClusterSize());
        if (clusterSpreadSpinner != null) clusterSpreadSpinner.getValueFactory().setValue(c.getClusterSpread());
        if (capBaseCheck != null) capBaseCheck.setSelected(c.isCapBase());
        if (capTipCheck != null) capTipCheck.setSelected(c.isCapTip());
        if (minCrystalLengthSlider != null) minCrystalLengthSlider.setValue(c.getMinCrystalLength());
        if (maxCrystalLengthSlider != null) maxCrystalLengthSlider.setValue(c.getMaxCrystalLength());
        if (minCrystalRadiusSlider != null) minCrystalRadiusSlider.setValue(c.getMinCrystalRadius());
        if (maxCrystalRadiusSlider != null) maxCrystalRadiusSlider.setValue(c.getMaxCrystalRadius());
        if (tipRadiusScaleSlider != null) tipRadiusScaleSlider.setValue(c.getTipRadiusScale());
        if (maxTiltAngleSlider != null) maxTiltAngleSlider.setValue(c.getMaxTiltAngleRadians());
        if (lengthJitterSlider != null) lengthJitterSlider.setValue(c.getLengthJitter());
        if (radiusJitterSlider != null) radiusJitterSlider.setValue(c.getRadiusJitter());
        if (embedDepthSlider != null) embedDepthSlider.setValue(c.getEmbedDepth());
        lastParams = c;
    }

    @Override
    public AsteroidParameters getParamsFromControls() {
        return buildUpdatedParamsFromControls();
    }

    @Override
    public AsteroidParameters getDefaultParameters() {
        return new CrystallineAsteroidParameters.Builder()
            .radius(100)
            .subdivisions(2)
            .deformation(0.3)
            .seed(System.nanoTime())
            .familyName("Crystalline")
            .crystalCount(12)
            .prismSides(6)
            .capBase(true)
            .capTip(true)
            .maxClusterSize(2)
            .clusterSpread(1)
            .minCrystalLength(0.04)
            .maxCrystalLength(0.11)
            .minCrystalRadius(0.012)
            .maxCrystalRadius(0.022)
            .tipRadiusScale(0.25)
            .maxTiltAngleRadians(Math.toRadians(12.0))
            .lengthJitter(0.08)
            .radiusJitter(0.06)
            .embedDepth(0.18)
            .build();
    }

    @Override
    public AsteroidParameters buildDefaultParamsFrom(AsteroidParameters previous) {
        return new CrystallineAsteroidParameters.Builder()
            .radius(previous.getRadius())
            .subdivisions(previous.getSubdivisions())
            .deformation(previous.getDeformation())
            .seed(System.nanoTime())
            .familyName("Crystalline")
            .crystalCount(12)
            .prismSides(6)
            .capBase(true)
            .capTip(true)
            .maxClusterSize(2)
            .clusterSpread(1)
            .minCrystalLength(0.04)
            .maxCrystalLength(0.11)
            .minCrystalRadius(0.012)
            .maxCrystalRadius(0.022)
            .tipRadiusScale(0.25)
            .maxTiltAngleRadians(Math.toRadians(12.0))
            .lengthJitter(0.08)
            .radiusJitter(0.06)
            .embedDepth(0.18)
            .build();
    }

    @Override
    public String getDisplayName() {
        return "Crystalline";
    }
}