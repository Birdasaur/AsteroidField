package AsteroidField.asteroids.providers;

import AsteroidField.asteroids.AsteroidFamilyUI;
import AsteroidField.asteroids.parameters.AsteroidParameters;
import AsteroidField.asteroids.parameters.HomeBaseAsteroidParameters;
import AsteroidField.asteroids.geometry.HomeBaseMesh;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.shape.TriangleMesh;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Button;

public class HomeBaseAsteroidMeshProvider implements AsteroidMeshProvider, AsteroidFamilyUI {

    private Spinner<Integer> mouthCountSpinner;
    private Slider mouthMajorDeg, mouthMinorDeg, mouthJitter;
    private Slider cavityRadiusRatio, cavityBlendScale, rimLift, rimSharpness;

    private Consumer<AsteroidParameters> onChangeCallback;
    private HomeBaseAsteroidParameters lastParams = null;

    @Override
    public TriangleMesh generateMesh(AsteroidParameters baseParams) {
        HomeBaseAsteroidParameters p = (HomeBaseAsteroidParameters) baseParams;
        return new HomeBaseMesh(p.getRadius(), p.getSubdivisions(), p);
    }

    @Override
    public Node createDynamicControls(AsteroidParameters startParams, Consumer<AsteroidParameters> onChange) {
        this.onChangeCallback = onChange;
        HomeBaseAsteroidParameters cur = (startParams instanceof HomeBaseAsteroidParameters)
                ? (HomeBaseAsteroidParameters) startParams
                : (HomeBaseAsteroidParameters) getDefaultParameters();
        lastParams = cur;

        mouthCountSpinner = new Spinner<>();
        mouthCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 6, cur.getMouthCount()));
        mouthMajorDeg = new Slider(20, 160, cur.getMouthMajorDeg());
        mouthMinorDeg = new Slider(20, 160, cur.getMouthMinorDeg());
        mouthJitter   = new Slider(0.0, 0.5,  cur.getMouthJitter());

        cavityRadiusRatio = new Slider(0.1, 0.9, cur.getCavityRadiusRatio());
        cavityBlendScale  = new Slider(1.0, 4.0, cur.getCavityBlendScale());
        rimLift           = new Slider(0.0, 0.2, cur.getRimLift());
        rimSharpness      = new Slider(0.5, 3.0, cur.getRimSharpness());

        mouthMajorDeg.setShowTickLabels(true);
        mouthMinorDeg.setShowTickLabels(true);
        mouthJitter.setShowTickLabels(true);
        cavityRadiusRatio.setShowTickLabels(true);
        cavityBlendScale.setShowTickLabels(true);
        rimLift.setShowTickLabels(true);
        rimSharpness.setShowTickLabels(true);

        Runnable update = () -> {
            lastParams = buildParamsFromControls(lastParams);
            if (onChangeCallback != null) onChangeCallback.accept(lastParams);
        };

        mouthCountSpinner.valueProperty().addListener((o,ov,nv) -> update.run());
        mouthMajorDeg.valueProperty().addListener((o,ov,nv) -> update.run());
        mouthMinorDeg.valueProperty().addListener((o,ov,nv) -> update.run());
        mouthJitter.valueProperty().addListener((o,ov,nv) -> update.run());
        cavityRadiusRatio.valueProperty().addListener((o,ov,nv) -> update.run());
        cavityBlendScale.valueProperty().addListener((o,ov,nv) -> update.run());
        rimLift.valueProperty().addListener((o,ov,nv) -> update.run());
        rimSharpness.valueProperty().addListener((o,ov,nv) -> update.run());

        Button randomizeBtn = new Button("Randomize Mouth Orientation");
        randomizeBtn.setOnAction(e -> {
            long newSeed = (lastParams != null ? lastParams.getSeed() : System.nanoTime()) ^ System.nanoTime();
            lastParams = lastParams.toBuilder().seed(newSeed).build();
            if (onChangeCallback != null) onChangeCallback.accept(lastParams);
        });

        VBox controls = new VBox(
            new Label("Home Base (Deform-Only)"),
            new VBox(5, new Label("Mouth Count:"), mouthCountSpinner),
            new VBox(5, new Label("Mouth Major (deg):"), mouthMajorDeg),
            new VBox(5, new Label("Mouth Minor (deg):"), mouthMinorDeg),
            new VBox(5, new Label("Mouth Jitter:"), mouthJitter),
            new VBox(5, new Label("Cavity Radius Ratio:"), cavityRadiusRatio),
            new VBox(5, new Label("Cavity Blend Scale:"), cavityBlendScale),
            new VBox(5, new Label("Rim Lift:"), rimLift),
            new VBox(5, new Label("Rim Sharpness:"), rimSharpness),
            randomizeBtn
        );
        controls.setSpacing(8);
        controls.setPadding(new Insets(10));
        setControlsFromParams(cur);
        return controls;
    }

    private HomeBaseAsteroidParameters buildParamsFromControls(HomeBaseAsteroidParameters prev) {
        return new HomeBaseAsteroidParameters.Builder()
                .radius(prev != null ? prev.getRadius() : 800.0)
                .subdivisions(prev != null ? prev.getSubdivisions() : 3)
                .deformation(prev != null ? prev.getDeformation() : 0.12)
                .seed(prev != null ? prev.getSeed() : System.nanoTime())
                .familyName("Home Base")
                .mouthCount(mouthCountSpinner.getValue())
                .mouthMajorDeg(mouthMajorDeg.getValue())
                .mouthMinorDeg(mouthMinorDeg.getValue())
                .mouthJitter(mouthJitter.getValue())
                .cavityRadiusRatio(cavityRadiusRatio.getValue())
                .cavityBlendScale(cavityBlendScale.getValue())
                .rimLift(rimLift.getValue())
                .rimSharpness(rimSharpness.getValue())
                .build();
    }

    @Override public void setControlsFromParams(AsteroidParameters params) {
        if (!(params instanceof HomeBaseAsteroidParameters)) return;
        HomeBaseAsteroidParameters p = (HomeBaseAsteroidParameters) params;
        if (mouthCountSpinner != null) mouthCountSpinner.getValueFactory().setValue(p.getMouthCount());
        if (mouthMajorDeg != null) mouthMajorDeg.setValue(p.getMouthMajorDeg());
        if (mouthMinorDeg != null) mouthMinorDeg.setValue(p.getMouthMinorDeg());
        if (mouthJitter   != null) mouthJitter.setValue(p.getMouthJitter());
        if (cavityRadiusRatio != null) cavityRadiusRatio.setValue(p.getCavityRadiusRatio());
        if (cavityBlendScale  != null) cavityBlendScale.setValue(p.getCavityBlendScale());
        if (rimLift != null) rimLift.setValue(p.getRimLift());
        if (rimSharpness != null) rimSharpness.setValue(p.getRimSharpness());
        lastParams = p;
    }

    @Override public AsteroidParameters getParamsFromControls() { return buildParamsFromControls(lastParams); }

    @Override
    public AsteroidParameters getDefaultParameters() {
        return new HomeBaseAsteroidParameters.Builder()
                .radius(800).subdivisions(3).deformation(0.12).seed(System.nanoTime()).familyName("Home Base")
                .mouthCount(1).mouthMajorDeg(90).mouthMinorDeg(60).mouthJitter(0.15)
                .cavityRadiusRatio(0.45).cavityBlendScale(2.2).rimLift(0.06).rimSharpness(1.1)
                .build();
    }

    @Override
    public AsteroidParameters buildDefaultParamsFrom(AsteroidParameters previous) {
        return new HomeBaseAsteroidParameters.Builder()
                .radius(previous.getRadius())
                .subdivisions(previous.getSubdivisions())
                .deformation(previous.getDeformation())
                .seed(System.nanoTime())
                .familyName("Home Base")
                .mouthCount(1).mouthMajorDeg(90).mouthMinorDeg(60).mouthJitter(0.15)
                .cavityRadiusRatio(0.45).cavityBlendScale(2.2).rimLift(0.06).rimSharpness(1.1)
                .build();
    }

    @Override public String getDisplayName() { return "Home Base"; }
}
