package AsteroidField.asteroids.providers;

import AsteroidField.asteroids.AsteroidFamilyUI;
import AsteroidField.asteroids.geometry.HollowBaseMesh;
import AsteroidField.asteroids.parameters.AsteroidParameters;
import AsteroidField.asteroids.parameters.HollowBaseAsteroidParameters;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.VBox;
import javafx.scene.shape.TriangleMesh;

public class HollowBaseAsteroidMeshProvider implements AsteroidMeshProvider, AsteroidFamilyUI {

    private Spinner<Integer> mouthCount;
    private Slider mouthAperture, mouthJitter;
    private Slider innerRadiusRatio, innerNoiseAmp;

    private HollowBaseAsteroidParameters lastParams = null;
    private Consumer<AsteroidParameters> onChange;

    @Override
    public TriangleMesh generateMesh(AsteroidParameters baseParams) {
        HollowBaseAsteroidParameters p = (HollowBaseAsteroidParameters) baseParams;
        return new HollowBaseMesh(p.getRadius(), p.getSubdivisions(), p);
    }

    @Override
    public Node createDynamicControls(AsteroidParameters startParams, Consumer<AsteroidParameters> onChange) {
        this.onChange = onChange;
        HollowBaseAsteroidParameters cur = (startParams instanceof HollowBaseAsteroidParameters)
                ? (HollowBaseAsteroidParameters) startParams
                : (HollowBaseAsteroidParameters) getDefaultParameters();
        lastParams = cur;

        mouthCount = new Spinner<>();
        mouthCount.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 6, cur.getMouthCount()));
        mouthAperture = new Slider(10, 140, cur.getMouthApertureDeg());
        mouthAperture.setShowTickLabels(true);
        mouthJitter = new Slider(0.0, 0.5, cur.getMouthJitter());
        mouthJitter.setShowTickLabels(true);

        innerRadiusRatio = new Slider(0.2, 0.95, cur.getInnerRadiusRatio());
        innerRadiusRatio.setShowTickLabels(true);
        innerNoiseAmp = new Slider(0.0, 0.3, cur.getInnerNoiseAmp());
        innerNoiseAmp.setShowTickLabels(true);

        Runnable update = () -> {
            lastParams = buildFromControls(lastParams);
            if (this.onChange != null) this.onChange.accept(lastParams);
        };

        mouthCount.valueProperty().addListener((o, ov, nv) -> update.run());
        mouthAperture.valueProperty().addListener((o, ov, nv) -> update.run());
        mouthJitter.valueProperty().addListener((o, ov, nv) -> update.run());
        innerRadiusRatio.valueProperty().addListener((o, ov, nv) -> update.run());
        innerNoiseAmp.valueProperty().addListener((o, ov, nv) -> update.run());

        Button randomizeBtn = new Button("Randomize Mouth Orientation");
        randomizeBtn.setOnAction(e -> {
            long newSeed = (lastParams != null ? lastParams.getSeed() : System.nanoTime()) ^ System.nanoTime();
            lastParams = lastParams.toBuilder().seed(newSeed).build();
            if (this.onChange != null) this.onChange.accept(lastParams);
        });

        VBox root = new VBox(
                new Label("Hollow Base (Outer + Inner Shells)"),
                new VBox(5, new Label("Mouth Count:"), mouthCount),
                new VBox(5, new Label("Mouth Aperture (deg):"), mouthAperture),
                new VBox(5, new Label("Mouth Jitter:"), mouthJitter),
                new VBox(5, new Label("Inner Radius Ratio:"), innerRadiusRatio),
                new VBox(5, new Label("Inner Noise Amp:"), innerNoiseAmp),
                randomizeBtn
        );
        root.setSpacing(8);
        root.setPadding(new Insets(10));
        setControlsFromParams(cur);
        return root;
    }

    private HollowBaseAsteroidParameters buildFromControls(HollowBaseAsteroidParameters prev) {
        return new HollowBaseAsteroidParameters.Builder()
                .radius(prev != null ? prev.getRadius() : 800.0)
                .subdivisions(prev != null ? prev.getSubdivisions() : 3)
                .deformation(prev != null ? prev.getDeformation() : 0.12)
                .seed(prev != null ? prev.getSeed() : System.nanoTime())
                .familyName("Hollow Base")
                .mouthCount(mouthCount.getValue())
                .mouthApertureDeg(mouthAperture.getValue())
                .mouthJitter(mouthJitter.getValue())
                .innerRadiusRatio(innerRadiusRatio.getValue())
                .innerNoiseAmp(innerNoiseAmp.getValue())
                .innerNoiseFreq(1.0)
                .build();
    }

    @Override
    public void setControlsFromParams(AsteroidParameters p) {
        if (!(p instanceof HollowBaseAsteroidParameters)) return;
        HollowBaseAsteroidParameters hp = (HollowBaseAsteroidParameters) p;
        if (mouthCount != null) mouthCount.getValueFactory().setValue(hp.getMouthCount());
        if (mouthAperture != null) mouthAperture.setValue(hp.getMouthApertureDeg());
        if (mouthJitter != null) mouthJitter.setValue(hp.getMouthJitter());
        if (innerRadiusRatio != null) innerRadiusRatio.setValue(hp.getInnerRadiusRatio());
        if (innerNoiseAmp != null) innerNoiseAmp.setValue(hp.getInnerNoiseAmp());
        lastParams = hp;
    }

    @Override public AsteroidParameters getParamsFromControls() { return buildFromControls(lastParams); }

    @Override
    public AsteroidParameters getDefaultParameters() {
        return new HollowBaseAsteroidParameters.Builder()
                .radius(800).subdivisions(3).deformation(0.12).seed(System.nanoTime()).familyName("Hollow Base")
                .mouthCount(1).mouthApertureDeg(55).mouthJitter(0.15)
                .innerRadiusRatio(0.6).innerNoiseAmp(0.05).innerNoiseFreq(1.0)
                .build();
    }

    @Override
    public AsteroidParameters buildDefaultParamsFrom(AsteroidParameters previous) {
        return new HollowBaseAsteroidParameters.Builder()
                .radius(previous.getRadius())
                .subdivisions(previous.getSubdivisions())
                .deformation(previous.getDeformation())
                .seed(System.nanoTime())
                .familyName("Hollow Base")
                .mouthCount(1).mouthApertureDeg(55).mouthJitter(0.15)
                .innerRadiusRatio(0.6).innerNoiseAmp(0.05).innerNoiseFreq(1.0)
                .build();
    }

    @Override public String getDisplayName() { return "Hollow Base"; }
}
