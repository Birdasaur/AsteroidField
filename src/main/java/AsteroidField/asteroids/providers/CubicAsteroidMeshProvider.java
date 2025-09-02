package AsteroidField.asteroids.providers;

import AsteroidField.asteroids.AsteroidFamilyUI;
import AsteroidField.asteroids.geometry.CubicMesh;
import AsteroidField.asteroids.parameters.AsteroidParameters;
import AsteroidField.asteroids.parameters.CubicAsteroidParameters;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.shape.TriangleMesh;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import java.util.function.Consumer;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;

public class CubicAsteroidMeshProvider implements AsteroidMeshProvider, AsteroidFamilyUI {
    private Spinner<Integer> subdivisionsSpinner;
    private Slider deformationSlider;
    private Consumer<AsteroidParameters> onChangeCallback;
    private CubicAsteroidParameters lastParams = null;

    @Override
    public TriangleMesh generateMesh(AsteroidParameters baseParams) {
        CubicAsteroidParameters params = (CubicAsteroidParameters) baseParams;
        return new CubicMesh(params.getRadius(), params.getSubdivisions(), params);
    }

    @Override
    public Node createDynamicControls(AsteroidParameters startParams, Consumer<AsteroidParameters> onChange) {
        this.onChangeCallback = onChange;
        CubicAsteroidParameters cur = (startParams instanceof CubicAsteroidParameters)
                ? (CubicAsteroidParameters) startParams
                : (CubicAsteroidParameters) getDefaultParameters();
        lastParams = cur;

        subdivisionsSpinner = new Spinner<>(0, 6, cur.getSubdivisions());
        deformationSlider = new Slider(0.0, 0.6, cur.getDeformation());
        deformationSlider.setShowTickLabels(true);

        ChangeListener<Object> update = (obs, oldV, newV) -> fireParamsChanged();

        subdivisionsSpinner.valueProperty().addListener(update);
        deformationSlider.valueProperty().addListener(update);

        VBox controls = new VBox(
            new Label("Cubic Asteroid Controls"),
            new VBox(5, new Label("Subdivisions:"), subdivisionsSpinner),
            new VBox(5, new Label("Deformation:"), deformationSlider)
        );
        setControlsFromParams(cur);
        controls.setSpacing(8);
        controls.setPadding(new Insets(10));
        return controls;
    }

    private void fireParamsChanged() {
        lastParams = buildUpdatedParamsFromControls();
        if (onChangeCallback != null) {
            onChangeCallback.accept(lastParams);
        }
    }

    private CubicAsteroidParameters buildUpdatedParamsFromControls() {
        return new CubicAsteroidParameters.Builder()
            .radius(lastParams != null ? lastParams.getRadius() : 100)
            .subdivisions(lastParams != null ? lastParams.getSubdivisions() : 2)
            .deformation(lastParams != null ? lastParams.getDeformation() : 0.1)
            .seed(lastParams != null ? lastParams.getSeed() : System.nanoTime())
            .familyName("Cubic")
            .build();
    }

    @Override
    public void setControlsFromParams(AsteroidParameters params) {
        if (!(params instanceof CubicAsteroidParameters)) return;
        CubicAsteroidParameters c = (CubicAsteroidParameters) params;
        if (subdivisionsSpinner != null) 
            subdivisionsSpinner.getValueFactory().setValue(c.getSubdivisions());
        if (deformationSlider != null) 
            deformationSlider.setValue(c.getDeformation());
        lastParams = c;
    }

    @Override
    public AsteroidParameters getParamsFromControls() {
        return buildUpdatedParamsFromControls();
    }

    @Override
    public AsteroidParameters getDefaultParameters() {
        return new CubicAsteroidParameters.Builder()
            .radius(100)
            .subdivisions(1)
            .deformation(0.12)
            .seed(System.nanoTime())
            .familyName("Cubic")
            .build();
    }

    @Override
    public AsteroidParameters buildDefaultParamsFrom(AsteroidParameters previous) {
        return new CubicAsteroidParameters.Builder()
            .radius(previous.getRadius())
            .subdivisions(1)
            .deformation(0.12)
            .seed(System.nanoTime())
            .familyName("Cubic")
            .build();
    }

    @Override
    public String getDisplayName() {
        return "Cubic";
    }
}
