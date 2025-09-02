package AsteroidField.asteroids.providers;

import AsteroidField.asteroids.AsteroidFamilyUI;
import AsteroidField.asteroids.geometry.KryptoniteClusterMesh;
import AsteroidField.asteroids.parameters.AsteroidParameters;
import AsteroidField.asteroids.parameters.KryptoniteClusterParameters;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.shape.TriangleMesh;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import java.util.function.Consumer;

public class KryptoniteClusterProvider implements AsteroidMeshProvider, AsteroidFamilyUI {

    // Kryptonite cluster-specific GUI controls
    private Spinner<Integer> numClustersSpinner, crystalsPerClusterSpinner;
    private Slider diskAngleSlider;

    // Crystalline family controls (as in base)
    private Spinner<Integer> crystalCountSpinner, prismSidesSpinner, maxClusterSizeSpinner, clusterSpreadSpinner, offshootRecursionSpinner;
    private CheckBox capBaseCheck, capTipCheck;
    private Slider minCrystalLengthSlider, maxCrystalLengthSlider;
    private Slider minCrystalRadiusSlider, maxCrystalRadiusSlider;
    private Slider tipRadiusScaleSlider, embedDepthSlider, facetJitterSlider;
    private Slider lengthJitterSlider, radiusJitterSlider, maxTiltAngleSlider;
    private Slider pointyTipChanceSlider, bevelTipChanceSlider, bevelDepthSlider;
    private Slider offshootChanceSlider, offshootScaleSlider;
    private Slider twistAmountSlider, fractureChanceSlider, fractureDepthSlider;

    private Consumer<AsteroidParameters> onChangeCallback;
    private KryptoniteClusterParameters lastParams = null;

@Override
public TriangleMesh generateMesh(AsteroidParameters baseParams) {
    System.out.println("KryptoniteClusterProvider.generateMesh() CALLED!");
    System.out.println("generateMesh() param type: " + baseParams.getClass().getName());

    KryptoniteClusterParameters params = (KryptoniteClusterParameters) baseParams;
    TriangleMesh mesh = new KryptoniteClusterMesh(params);
    System.out.println("Returning KryptoniteClusterMesh: " + mesh.getClass().getSimpleName());
    return mesh;
}

    @Override
    public Node createDynamicControls(AsteroidParameters startParams, Consumer<AsteroidParameters> onChange) {
        this.onChangeCallback = onChange;
        KryptoniteClusterParameters cur = (startParams instanceof KryptoniteClusterParameters)
                ? (KryptoniteClusterParameters) startParams
                : (KryptoniteClusterParameters) getDefaultParameters();
        lastParams = cur;

        // Kryptonite-specific controls
        numClustersSpinner = new Spinner<>(1, 8, cur.getNumClusters());
        crystalsPerClusterSpinner = new Spinner<>(3, 60, cur.getCrystalsPerCluster());
        diskAngleSlider = new Slider(5.0, 60.0, cur.getDiskAngleDegrees());
        diskAngleSlider.setShowTickLabels(true);

        // Crystalline controls (reuse from base provider)
        crystalCountSpinner = new Spinner<>(1, 200, cur.getCrystalCount());
        prismSidesSpinner = new Spinner<>(3, 16, cur.getPrismSides());
        maxClusterSizeSpinner = new Spinner<>(1, 12, cur.getMaxClusterSize());
        clusterSpreadSpinner = new Spinner<>(1, 6, cur.getClusterSpread());
        offshootRecursionSpinner = new Spinner<>(0, 3, cur.getOffshootRecursion());

        capBaseCheck = new CheckBox("Cap Base");
        capBaseCheck.setSelected(cur.isCapBase());
        capTipCheck = new CheckBox("Cap Tip");
        capTipCheck.setSelected(cur.isCapTip());

        minCrystalLengthSlider = new Slider(0.01, 1.0, cur.getMinCrystalLength());
        maxCrystalLengthSlider = new Slider(0.01, 1.5, cur.getMaxCrystalLength());
        minCrystalRadiusSlider = new Slider(0.003, 0.1, cur.getMinCrystalRadius());
        maxCrystalRadiusSlider = new Slider(0.003, 0.5, cur.getMaxCrystalRadius());
        tipRadiusScaleSlider = new Slider(0.0, 1.0, cur.getTipRadiusScale());
        embedDepthSlider = new Slider(0.0, 0.4, cur.getEmbedDepth());
        facetJitterSlider = new Slider(0.0, 0.3, cur.getFacetJitter());
        lengthJitterSlider = new Slider(0.0, 0.5, cur.getLengthJitter());
        radiusJitterSlider = new Slider(0.0, 0.2, cur.getRadiusJitter());
        maxTiltAngleSlider = new Slider(0.0, Math.toRadians(30.0), cur.getMaxTiltAngleRadians());

        pointyTipChanceSlider = new Slider(0.0, 1.0, cur.getPointyTipChance());
        bevelTipChanceSlider = new Slider(0.0, 1.0, cur.getBevelTipChance());
        bevelDepthSlider = new Slider(0.0, 0.25, cur.getBevelDepth());

        offshootChanceSlider = new Slider(0.0, 0.6, cur.getOffshootChance());
        offshootScaleSlider = new Slider(0.1, 0.8, cur.getOffshootScale());

        twistAmountSlider = new Slider(0.0, Math.toRadians(30.0), cur.getTwistAmount());
        fractureChanceSlider = new Slider(0.0, 1.0, cur.getFractureChance());
        fractureDepthSlider = new Slider(0.0, 0.3, cur.getFractureDepth());

        // Set tick labels for all sliders (as before)
        minCrystalLengthSlider.setShowTickLabels(true);
        maxCrystalLengthSlider.setShowTickLabels(true);
        minCrystalRadiusSlider.setShowTickLabels(true);
        maxCrystalRadiusSlider.setShowTickLabels(true);
        tipRadiusScaleSlider.setShowTickLabels(true);
        embedDepthSlider.setShowTickLabels(true);
        facetJitterSlider.setShowTickLabels(true);
        lengthJitterSlider.setShowTickLabels(true);
        radiusJitterSlider.setShowTickLabels(true);
        maxTiltAngleSlider.setShowTickLabels(true);
        pointyTipChanceSlider.setShowTickLabels(true);
        bevelTipChanceSlider.setShowTickLabels(true);
        bevelDepthSlider.setShowTickLabels(true);
        offshootChanceSlider.setShowTickLabels(true);
        offshootScaleSlider.setShowTickLabels(true);
        twistAmountSlider.setShowTickLabels(true);
        fractureChanceSlider.setShowTickLabels(true);
        fractureDepthSlider.setShowTickLabels(true);

        ChangeListener<Object> update = (obs, oldV, newV) -> fireParamsChanged();

        numClustersSpinner.valueProperty().addListener(update);
        crystalsPerClusterSpinner.valueProperty().addListener(update);
        diskAngleSlider.valueProperty().addListener(update);

        crystalCountSpinner.valueProperty().addListener(update);
        prismSidesSpinner.valueProperty().addListener(update);
        maxClusterSizeSpinner.valueProperty().addListener(update);
        clusterSpreadSpinner.valueProperty().addListener(update);
        offshootRecursionSpinner.valueProperty().addListener(update);

        capBaseCheck.selectedProperty().addListener(update);
        capTipCheck.selectedProperty().addListener(update);

        minCrystalLengthSlider.valueProperty().addListener(update);
        maxCrystalLengthSlider.valueProperty().addListener(update);
        minCrystalRadiusSlider.valueProperty().addListener(update);
        maxCrystalRadiusSlider.valueProperty().addListener(update);
        tipRadiusScaleSlider.valueProperty().addListener(update);
        embedDepthSlider.valueProperty().addListener(update);
        facetJitterSlider.valueProperty().addListener(update);
        lengthJitterSlider.valueProperty().addListener(update);
        radiusJitterSlider.valueProperty().addListener(update);
        maxTiltAngleSlider.valueProperty().addListener(update);

        pointyTipChanceSlider.valueProperty().addListener(update);
        bevelTipChanceSlider.valueProperty().addListener(update);
        bevelDepthSlider.valueProperty().addListener(update);

        offshootChanceSlider.valueProperty().addListener(update);
        offshootScaleSlider.valueProperty().addListener(update);

        twistAmountSlider.valueProperty().addListener(update);
        fractureChanceSlider.valueProperty().addListener(update);
        fractureDepthSlider.valueProperty().addListener(update);

        VBox controls = new VBox(
            new Label("Kryptonite Cluster Controls"),
            new VBox(5, new Label("Number of Clusters:"), numClustersSpinner),
            new VBox(5, new Label("Crystals Per Cluster:"), crystalsPerClusterSpinner),
            new VBox(5, new Label("Cluster Disk Angle (degrees):"), diskAngleSlider),
            new Separator(),
            new Label("Crystal Family Controls"),
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
            new VBox(5, new Label("Embed Depth (×core radius):"), embedDepthSlider),
            new VBox(5, new Label("Facet Jitter:"), facetJitterSlider),
            new VBox(5, new Label("Length Jitter:"), lengthJitterSlider),
            new VBox(5, new Label("Radius Jitter:"), radiusJitterSlider),
            new VBox(5, new Label("Max Tilt Angle (radians):"), maxTiltAngleSlider),
            new Separator(),
            new Label("Tip/Termination:"),
            new VBox(5, new Label("Pointy Tip Chance:"), pointyTipChanceSlider),
            new VBox(5, new Label("Bevel Tip Chance:"), bevelTipChanceSlider),
            new VBox(5, new Label("Bevel Depth (×crystal length):"), bevelDepthSlider),
            new Separator(),
            new Label("Crystal Offshoots:"),
            new VBox(5, new Label("Offshoot Chance:"), offshootChanceSlider),
            new VBox(5, new Label("Offshoot Scale:"), offshootScaleSlider),
            new VBox(5, new Label("Offshoot Recursion:"), offshootRecursionSpinner),
            new Separator(),
            new Label("Advanced Effects:"),
            new VBox(5, new Label("Max Twist (radians):"), twistAmountSlider),
            new VBox(5, new Label("Fracture Chance:"), fractureChanceSlider),
            new VBox(5, new Label("Fracture Depth (×core radius):"), fractureDepthSlider)
        );
        setControlsFromParams(cur);
        controls.setSpacing(8);
        controls.setPadding(new Insets(10)); 

        ScrollPane scrollPane = new ScrollPane(controls);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        return scrollPane;        
    }

    private void fireParamsChanged() {
        lastParams = buildUpdatedParamsFromControls();
        if (onChangeCallback != null) {
            onChangeCallback.accept(lastParams);
        }
    }

    private KryptoniteClusterParameters buildUpdatedParamsFromControls() {
        double minLen = minCrystalLengthSlider.getValue();
        double maxLen = Math.max(minLen, maxCrystalLengthSlider.getValue());
        double minRad = minCrystalRadiusSlider.getValue();
        double maxRad = Math.max(minRad, maxCrystalRadiusSlider.getValue());
        return new KryptoniteClusterParameters.Builder()
            .numClusters(numClustersSpinner.getValue())
            .crystalsPerCluster(crystalsPerClusterSpinner.getValue())
            .diskAngleDegrees(diskAngleSlider.getValue())
            .radius(lastParams != null ? lastParams.getRadius() : 100)
            .subdivisions(lastParams != null ? lastParams.getSubdivisions() : 2)
            .deformation(lastParams != null ? lastParams.getDeformation() : 0.3)
            .seed(lastParams != null ? lastParams.getSeed() : System.nanoTime())
            .familyName("Kryptonite")
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
            .embedDepth(embedDepthSlider.getValue())
            .facetJitter(facetJitterSlider.getValue())
            .lengthJitter(lengthJitterSlider.getValue())
            .radiusJitter(radiusJitterSlider.getValue())
            .maxTiltAngleRadians(maxTiltAngleSlider.getValue())
            .pointyTipChance(pointyTipChanceSlider.getValue())
            .bevelTipChance(bevelTipChanceSlider.getValue())
            .bevelDepth(bevelDepthSlider.getValue())
            .offshootChance(offshootChanceSlider.getValue())
            .offshootScale(offshootScaleSlider.getValue())
            .offshootRecursion(offshootRecursionSpinner.getValue())
            .twistAmount(twistAmountSlider.getValue())
            .fractureChance(fractureChanceSlider.getValue())
            .fractureDepth(fractureDepthSlider.getValue())
            .build();
    }

    @Override
    public void setControlsFromParams(AsteroidParameters params) {
        if (!(params instanceof KryptoniteClusterParameters)) return;
        KryptoniteClusterParameters c = (KryptoniteClusterParameters) params;
        if (numClustersSpinner != null) numClustersSpinner.getValueFactory().setValue(c.getNumClusters());
        if (crystalsPerClusterSpinner != null) crystalsPerClusterSpinner.getValueFactory().setValue(c.getCrystalsPerCluster());
        if (diskAngleSlider != null) diskAngleSlider.setValue(c.getDiskAngleDegrees());

        if (crystalCountSpinner != null) crystalCountSpinner.getValueFactory().setValue(c.getCrystalCount());
        if (prismSidesSpinner != null) prismSidesSpinner.getValueFactory().setValue(c.getPrismSides());
        if (maxClusterSizeSpinner != null) maxClusterSizeSpinner.getValueFactory().setValue(c.getMaxClusterSize());
        if (clusterSpreadSpinner != null) clusterSpreadSpinner.getValueFactory().setValue(c.getClusterSpread());
        if (offshootRecursionSpinner != null) offshootRecursionSpinner.getValueFactory().setValue(c.getOffshootRecursion());

        if (capBaseCheck != null) capBaseCheck.setSelected(c.isCapBase());
        if (capTipCheck != null) capTipCheck.setSelected(c.isCapTip());

        if (minCrystalLengthSlider != null) minCrystalLengthSlider.setValue(c.getMinCrystalLength());
        if (maxCrystalLengthSlider != null) maxCrystalLengthSlider.setValue(c.getMaxCrystalLength());
        if (minCrystalRadiusSlider != null) minCrystalRadiusSlider.setValue(c.getMinCrystalRadius());
        if (maxCrystalRadiusSlider != null) maxCrystalRadiusSlider.setValue(c.getMaxCrystalRadius());
        if (tipRadiusScaleSlider != null) tipRadiusScaleSlider.setValue(c.getTipRadiusScale());
        if (embedDepthSlider != null) embedDepthSlider.setValue(c.getEmbedDepth());
        if (facetJitterSlider != null) facetJitterSlider.setValue(c.getFacetJitter());
        if (lengthJitterSlider != null) lengthJitterSlider.setValue(c.getLengthJitter());
        if (radiusJitterSlider != null) radiusJitterSlider.setValue(c.getRadiusJitter());
        if (maxTiltAngleSlider != null) maxTiltAngleSlider.setValue(c.getMaxTiltAngleRadians());

        if (pointyTipChanceSlider != null) pointyTipChanceSlider.setValue(c.getPointyTipChance());
        if (bevelTipChanceSlider != null) bevelTipChanceSlider.setValue(c.getBevelTipChance());
        if (bevelDepthSlider != null) bevelDepthSlider.setValue(c.getBevelDepth());

        if (offshootChanceSlider != null) offshootChanceSlider.setValue(c.getOffshootChance());
        if (offshootScaleSlider != null) offshootScaleSlider.setValue(c.getOffshootScale());

        if (twistAmountSlider != null) twistAmountSlider.setValue(c.getTwistAmount());
        if (fractureChanceSlider != null) fractureChanceSlider.setValue(c.getFractureChance());
        if (fractureDepthSlider != null) fractureDepthSlider.setValue(c.getFractureDepth());

        lastParams = c;
    }

    @Override
    public AsteroidParameters getParamsFromControls() {
        return buildUpdatedParamsFromControls();
    }

    @Override
    public AsteroidParameters getDefaultParameters() {
        return new KryptoniteClusterParameters.Builder()
            .numClusters(1)
            .crystalsPerCluster(20)
            .diskAngleDegrees(24.0)
            .radius(100)
            .subdivisions(2)
            .deformation(0.3)
            .seed(System.nanoTime())
            .familyName("Kryptonite")
            .crystalCount(24)
            .prismSides(6)
            .capBase(true)
            .capTip(true)
            .maxClusterSize(12)
            .clusterSpread(1)
            .minCrystalLength(0.13)
            .maxCrystalLength(0.39)
            .minCrystalRadius(0.012)
            .maxCrystalRadius(0.044)
            .tipRadiusScale(0.13)
            .embedDepth(0.10)
            .facetJitter(0.23)
            .lengthJitter(0.24)
            .radiusJitter(0.17)
            .maxTiltAngleRadians(Math.toRadians(29.0))
            .pointyTipChance(0.52)
            .bevelTipChance(0.19)
            .bevelDepth(0.15)
            .offshootChance(0.19)
            .offshootScale(0.57)
            .offshootRecursion(1)
            .twistAmount(Math.toRadians(10.0))
            .fractureChance(0.14)
            .fractureDepth(0.11)
            .build();
    }

    @Override
    public AsteroidParameters buildDefaultParamsFrom(AsteroidParameters previous) {
        return new KryptoniteClusterParameters.Builder()
            .numClusters(1)
            .crystalsPerCluster(20)
            .diskAngleDegrees(24.0)
            .radius(previous.getRadius())
            .subdivisions(previous.getSubdivisions())
            .deformation(previous.getDeformation())
            .seed(System.nanoTime())
            .familyName("Kryptonite")
            .crystalCount(24)
            .prismSides(6)
            .capBase(true)
            .capTip(true)
            .maxClusterSize(12)
            .clusterSpread(1)
            .minCrystalLength(0.13)
            .maxCrystalLength(0.39)
            .minCrystalRadius(0.012)
            .maxCrystalRadius(0.044)
            .tipRadiusScale(0.13)
            .embedDepth(0.10)
            .facetJitter(0.23)
            .lengthJitter(0.24)
            .radiusJitter(0.17)
            .maxTiltAngleRadians(Math.toRadians(29.0))
            .pointyTipChance(0.52)
            .bevelTipChance(0.19)
            .bevelDepth(0.15)
            .offshootChance(0.19)
            .offshootScale(0.57)
            .offshootRecursion(1)
            .twistAmount(Math.toRadians(10.0))
            .fractureChance(0.14)
            .fractureDepth(0.11)
            .build();
    }

    @Override
    public String getDisplayName() {
        return "Kryptonite";
    }
}
