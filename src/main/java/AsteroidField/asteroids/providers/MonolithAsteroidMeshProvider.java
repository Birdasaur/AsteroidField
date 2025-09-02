package AsteroidField.asteroids.providers;

import AsteroidField.asteroids.AsteroidFamilyUI;
import AsteroidField.asteroids.geometry.MonolithMesh;
import AsteroidField.asteroids.parameters.AsteroidParameters;
import AsteroidField.asteroids.parameters.MonolithAsteroidParameters;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.scene.shape.TriangleMesh;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import java.util.function.Consumer;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Separator;

public class MonolithAsteroidMeshProvider implements AsteroidMeshProvider, AsteroidFamilyUI {
    // Core mesh controls
    private Spinner<Integer> subdivisionsSpinner;
    private Slider deformationSlider;

    // Tail controls
    private CheckBox[] tailFaceChecks;
    private Spinner<Integer> tailSegmentsSpinner;
    private Slider tailBlockScaleSlider, tailSpreadSlider, tailJitterSlider;

    private Consumer<AsteroidParameters> onChangeCallback;
    private MonolithAsteroidParameters lastParams = null;

    @Override
    public TriangleMesh generateMesh(AsteroidParameters baseParams) {
        MonolithAsteroidParameters params = (MonolithAsteroidParameters) baseParams;
        return new MonolithMesh(params.getRadius(), params.getSubdivisions(), params);
    }

    @Override
    public Node createDynamicControls(AsteroidParameters startParams, Consumer<AsteroidParameters> onChange) {
        this.onChangeCallback = onChange;
        MonolithAsteroidParameters cur = (startParams instanceof MonolithAsteroidParameters)
                ? (MonolithAsteroidParameters) startParams
                : (MonolithAsteroidParameters) getDefaultParameters();
        lastParams = cur;

        subdivisionsSpinner = new Spinner<>(0, 6, cur.getSubdivisions());
        deformationSlider = new Slider(0.0, 0.6, cur.getDeformation());
        deformationSlider.setShowTickLabels(true);

        // Tail face controls (one per face)
        String[] faceLabels = {"+Z (Front)", "-Z (Back)", "+X (Right)", "-X (Left)", "+Y (Top)", "-Y (Bottom)"};
        tailFaceChecks = new CheckBox[6];
        for (int i = 0; i < 6; i++) {
            tailFaceChecks[i] = new CheckBox(faceLabels[i]);
            tailFaceChecks[i].setSelected(cur.hasTail(i));
        }

        tailSegmentsSpinner = new Spinner<>(1, 16, cur.getTailSegments());
        tailBlockScaleSlider = new Slider(0.15, 1.2, cur.getTailBlockScale());
        tailBlockScaleSlider.setShowTickLabels(true);
        tailSpreadSlider = new Slider(0.0, 1.0, cur.getTailSpread());
        tailSpreadSlider.setShowTickLabels(true);
        tailJitterSlider = new Slider(0.0, 1.0, cur.getTailJitter());
        tailJitterSlider.setShowTickLabels(true);

        ChangeListener<Object> update = (obs, oldV, newV) -> fireParamsChanged();
        subdivisionsSpinner.valueProperty().addListener(update);
        deformationSlider.valueProperty().addListener(update);
        tailSegmentsSpinner.valueProperty().addListener(update);
        tailBlockScaleSlider.valueProperty().addListener(update);
        tailSpreadSlider.valueProperty().addListener(update);
        tailJitterSlider.valueProperty().addListener(update);
        for (CheckBox cb : tailFaceChecks) cb.selectedProperty().addListener(update);

        VBox tailFacesBox = new VBox(2, new Label("Tails on Faces:"));
        tailFacesBox.getChildren().addAll(tailFaceChecks);

        VBox controls = new VBox(
            new Label("Monolith Asteroid Controls"),
            new VBox(5, new Label("Subdivisions:"), subdivisionsSpinner),
            new VBox(5, new Label("Deformation:"), deformationSlider),
            new Separator(),
            tailFacesBox,
            new VBox(5, new Label("Tail Segments:"), tailSegmentsSpinner),
            new VBox(5, new Label("Tail Block Scale:"), tailBlockScaleSlider),
            new VBox(5, new Label("Tail Spread:"), tailSpreadSlider),
            new VBox(5, new Label("Tail Jitter:"), tailJitterSlider)
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

    private MonolithAsteroidParameters buildUpdatedParamsFromControls() {
        boolean[] tailFaces = new boolean[6];
        for (int i = 0; i < 6; i++)
            tailFaces[i] = (tailFaceChecks != null && tailFaceChecks[i] != null) ? tailFaceChecks[i].isSelected() : false;

        int subdivisions = (subdivisionsSpinner != null)
            ? subdivisionsSpinner.getValue()
            : (lastParams != null ? lastParams.getSubdivisions() : 1);

        double deformation = (deformationSlider != null)
            ? deformationSlider.getValue()
            : (lastParams != null ? lastParams.getDeformation() : 0.12);

        int tailSegments = (tailSegmentsSpinner != null)
            ? tailSegmentsSpinner.getValue()
            : (lastParams != null ? lastParams.getTailSegments() : 4);

        double blockScale = (tailBlockScaleSlider != null)
            ? tailBlockScaleSlider.getValue()
            : (lastParams != null ? lastParams.getTailBlockScale() : 0.7);

        double tailSpread = (tailSpreadSlider != null)
            ? tailSpreadSlider.getValue()
            : (lastParams != null ? lastParams.getTailSpread() : 0.28);

        double tailJitter = (tailJitterSlider != null)
            ? tailJitterSlider.getValue()
            : (lastParams != null ? lastParams.getTailJitter() : 0.22);

        MonolithAsteroidParameters.Builder b = new MonolithAsteroidParameters.Builder()
            .radius(lastParams != null ? lastParams.getRadius() : 100)
            .subdivisions(subdivisions)
            .deformation(deformation)
            .seed(lastParams != null ? lastParams.getSeed() : System.nanoTime())
            .familyName("Monolith")
            .tailFaces(tailFaces)
            .tailSegments(tailSegments)
            .tailBlockScale(blockScale)
            .tailSpread(tailSpread)
            .tailJitter(tailJitter);
        return b.build();
    }

    @Override
    public void setControlsFromParams(AsteroidParameters params) {
        if (!(params instanceof MonolithAsteroidParameters)) return;
        MonolithAsteroidParameters c = (MonolithAsteroidParameters) params;
        if (subdivisionsSpinner != null)
            subdivisionsSpinner.getValueFactory().setValue(c.getSubdivisions());
        if (deformationSlider != null)
            deformationSlider.setValue(c.getDeformation());
        boolean[] tf = c.getTailFaces();
        if (tailFaceChecks != null && tf != null)
            for (int i = 0; i < Math.min(6, tailFaceChecks.length); i++)
                tailFaceChecks[i].setSelected(tf[i]);
        if (tailSegmentsSpinner != null)
            tailSegmentsSpinner.getValueFactory().setValue(c.getTailSegments());
        if (tailBlockScaleSlider != null)
            tailBlockScaleSlider.setValue(c.getTailBlockScale());
        if (tailSpreadSlider != null)
            tailSpreadSlider.setValue(c.getTailSpread());
        if (tailJitterSlider != null)
            tailJitterSlider.setValue(c.getTailJitter());
        lastParams = c;
    }

    @Override
    public AsteroidParameters getParamsFromControls() {
        return buildUpdatedParamsFromControls();
    }

    @Override
    public AsteroidParameters getDefaultParameters() {
        return new MonolithAsteroidParameters.Builder()
            .radius(100)
            .subdivisions(1)
            .deformation(0.13)
            .seed(System.nanoTime())
            .familyName("Monolith")
            .tailFaces(new boolean[]{true, false, false, false, false, false})
            .tailSegments(4)
            .tailBlockScale(0.7)
            .tailSpread(0.28)
            .tailJitter(0.22)
            .build();
    }

    @Override
    public AsteroidParameters buildDefaultParamsFrom(AsteroidParameters previous) {
        return new MonolithAsteroidParameters.Builder()
            .radius(previous.getRadius())
            .subdivisions(1)
            .deformation(0.13)
            .seed(System.nanoTime())
            .familyName("Monolith")
            .tailFaces(new boolean[]{true, false, false, false, false, false})
            .tailSegments(4)
            .tailBlockScale(0.7)
            .tailSpread(0.28)
            .tailJitter(0.22)
            .build();
    }

    @Override
    public String getDisplayName() {
        return "Monolith";
    }
}