package AsteroidField;

import javafx.beans.binding.Bindings;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.SceneAntialiasing;

import java.util.*;
import java.util.function.Consumer;
import javafx.scene.Node;

public class AsteroidField3DView extends Pane {

    private SubScene subScene;
    private PerspectiveCamera camera;
    private Group world;
    private MeshView asteroidView;
    private MeshView asteroidLinesView;
    private AsteroidParameters params;
    private AsteroidMeshProvider currentProvider = AsteroidMeshProvider.PROVIDERS.values().iterator().next();

    private final Map<String, AsteroidParameters> familyParamsMap = new HashMap<>();
    private final Random seedRng = new Random();

    // Shared control fields
    private Slider radiusSlider;
    private Spinner<Integer> subdivSpinner;
    private Slider deformSlider;
    private TextField seedField;
    private final List<MeshView> asteroidViews = new ArrayList<>();
    private MeshView selectedAsteroid = null;

    // For dynamic family-specific controls
    private final HBox dynamicParamBox = new HBox(10);

    // Camera/app controls
    private ToggleButton cameraModeToggle;
    private Button resetCameraBtn;
    private ComboBox<String> asteroidSelectorBox;
    private FlyCameraController flyController;
    private TrackBallController trackballController;

    public AsteroidField3DView() {
        world = new Group();

        params = new AsteroidParameters.Builder<>()
                .radius(100)
                .subdivisions(2)
                .deformation(0.3)
                .seed(System.nanoTime())
                .familyName("Classic Rocky")
                .build();
        familyParamsMap.put(params.getFamilyName(), params);

        asteroidView = new MeshView();
        asteroidView.setMaterial(new PhongMaterial(Color.GAINSBORO));
        asteroidView.setDrawMode(DrawMode.FILL);
        world.getChildren().add(asteroidView);

        asteroidLinesView = new MeshView();
        asteroidLinesView.setMaterial(new PhongMaterial(Color.ALICEBLUE));
        asteroidLinesView.setDrawMode(DrawMode.LINE);
        world.getChildren().add(asteroidLinesView);
        Bindings.bindContent(asteroidLinesView.getTransforms(), asteroidView.getTransforms());

        regenerateAsteroid();
        selectedAsteroid = asteroidView;

        camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);
        camera.setTranslateZ(-600);

        subScene = new SubScene(world, 800, 600, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.BLACK);
        subScene.setCamera(camera);

        flyController = new FlyCameraController(camera, subScene);
        flyController.setSpeed(150);
        flyController.setBoostMultiplier(3);
        flyController.setSensitivity(0.2);

        trackballController = new TrackBallController(subScene, camera, asteroidViews);
        trackballController.setEnabled(false);

        getChildren().add(subScene);
        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());
    }

    // --- TOP BAR: Scene/Application Controls ---
    public HBox createSceneControls() {
        HBox topBar = new HBox(10);
        topBar.setId("scene-controls");

        cameraModeToggle = new ToggleButton("Trackball Mode");
        cameraModeToggle.setTooltip(new Tooltip("Toggle between Fly and Trackball camera"));

        resetCameraBtn = new Button("Reset Camera");
        resetCameraBtn.setTooltip(new Tooltip("Reset camera to default position"));

        ToggleButton wireframeToggle = new ToggleButton("Show Wireframe");
        wireframeToggle.setSelected(true);
        wireframeToggle.setTooltip(new Tooltip("Show/hide asteroid wireframe overlay"));
        wireframeToggle.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (asteroidLinesView != null) asteroidLinesView.setVisible(isSelected);
        });
        if (asteroidLinesView != null) asteroidLinesView.setVisible(wireframeToggle.isSelected());

        asteroidSelectorBox = new ComboBox<>();
        updateAsteroidSelectorBox();
        asteroidSelectorBox.setTooltip(new Tooltip("Select asteroid to rotate or highlight"));

        cameraModeToggle.setOnAction(e -> {
            boolean trackballMode = cameraModeToggle.isSelected();
            if (trackballMode) {
                flyController.setEnabled(false);
                trackballController.setEnabled(true);
                trackballController.setSelected(selectedAsteroid);
            } else {
                trackballController.setEnabled(false);
                flyController.setEnabled(true);
            }
        });

        resetCameraBtn.setOnAction(e -> {
            camera.setTranslateX(0);
            camera.setTranslateY(0);
            camera.setTranslateZ(-600);
            camera.setRotationAxis(javafx.geometry.Point3D.ZERO.add(0, 1, 0));
            camera.setRotate(0);
            if (selectedAsteroid != null) {
                selectedAsteroid.setRotationAxis(javafx.geometry.Point3D.ZERO.add(1, 0, 0));
                selectedAsteroid.setRotate(0);
                selectedAsteroid.setRotationAxis(javafx.geometry.Point3D.ZERO.add(0, 1, 0));
                selectedAsteroid.setRotate(0);
            }
        });

        asteroidSelectorBox.setOnAction(e -> {
            int idx = asteroidSelectorBox.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < asteroidViews.size()) {
                setSelectedAsteroid(asteroidViews.get(idx));
            }
        });

        topBar.getChildren().addAll(cameraModeToggle, resetCameraBtn, wireframeToggle, new Label("Asteroid:"), asteroidSelectorBox);
        return topBar;
    }

    private void updateAsteroidSelectorBox() {
        asteroidSelectorBox.getItems().clear();
        for (int i = 0; i < asteroidViews.size(); i++) {
            asteroidSelectorBox.getItems().add("Asteroid " + (i + 1));
        }
        if (!asteroidViews.isEmpty()) {
            asteroidSelectorBox.getSelectionModel().select(asteroidViews.indexOf(selectedAsteroid));
        }
    }

    public void setSelectedAsteroid(MeshView meshView) {
        if (selectedAsteroid != null) {
            selectedAsteroid.setMaterial(new PhongMaterial(Color.GAINSBORO));
        }
        selectedAsteroid = meshView;
        if (selectedAsteroid != null) {
            selectedAsteroid.setMaterial(new PhongMaterial(Color.LIGHTGOLDENRODYELLOW));
            trackballController.setSelected(selectedAsteroid);
            updateAsteroidSelectorBox();
        }
    }

    public void addAsteroid(MeshView meshView) {
        asteroidViews.add(meshView);
        world.getChildren().add(meshView);
        updateAsteroidSelectorBox();
    }

    // --- BOTTOM BAR: Asteroid Controls (with merged updater) ---
    public HBox createControls() {
        HBox hbox = new HBox(10);

        Label familyLabel = new Label("Family:");
        ComboBox<String> familyBox = new ComboBox<>();
        familyBox.getItems().addAll(AsteroidMeshProvider.PROVIDERS.keySet());
        familyBox.getSelectionModel().select("Classic Rocky");

        radiusSlider = new Slider(20, 300, params.getRadius());
        VBox radiusBox = new VBox(5, new Label("Radius:"), radiusSlider);

        subdivSpinner = new Spinner<>();
        subdivSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 6, params.getSubdivisions()));
        VBox subdivBox = new VBox(5, new Label("Subdiv:"), subdivSpinner);

        deformSlider = new Slider(0, 1, params.getDeformation());
        VBox deformBox = new VBox(5, new Label("Deform:"), deformSlider);

        seedField = new TextField(Long.toString(params.getSeed()));
        seedField.setPrefWidth(100);
        VBox seedBox = new VBox(5, new Label("Seed:"), seedField);

        Button randomizeBtn = new Button("Randomize");

        // --- Unified update for all controls (shared + dynamic) ---
        Consumer<Void> mergedControlUpdate = unused -> {
            params = buildMergedParams();
            familyParamsMap.put(params.getFamilyName(), params);
            updateDynamicParameterControlsValues();
            regenerateAsteroid();
        };

        radiusSlider.valueProperty().addListener((obs, oldV, newV) -> mergedControlUpdate.accept(null));
        subdivSpinner.valueProperty().addListener((obs, oldV, newV) -> mergedControlUpdate.accept(null));
        deformSlider.valueProperty().addListener((obs, oldV, newV) -> mergedControlUpdate.accept(null));
        seedField.setOnAction(e -> mergedControlUpdate.accept(null));
        randomizeBtn.setOnAction(e -> {
            long seed = seedRng.nextLong();
            seedField.setText(Long.toString(seed));
            mergedControlUpdate.accept(null);
        });

        familyBox.setOnAction(e -> {
            familyParamsMap.put(params.getFamilyName(), buildMergedParams());

            String selected = familyBox.getValue();
            currentProvider = AsteroidMeshProvider.PROVIDERS.get(selected);

            AsteroidParameters newParams = familyParamsMap.get(selected);
            if (newParams == null) {
                newParams = buildParamsForFamily(selected, params);
            }
            params = newParams;

            updateSharedControlValues();
            updateDynamicParameterControls();
            regenerateAsteroid();
        });

        dynamicParamBox.setSpacing(10);
        updateDynamicParameterControls();

        hbox.getChildren().addAll(
                new VBox(10, familyLabel, familyBox),
                radiusBox, subdivBox, deformBox,
                seedBox,
                randomizeBtn,
                dynamicParamBox
        );
        return hbox;
    }

    /**
     * Update family-specific parameter controls.
     */
    private void updateDynamicParameterControls() {
        dynamicParamBox.getChildren().clear();

        if (currentProvider instanceof AsteroidFamilyUI uiProvider) {
            // Pass the merged update as callback
            Node controls = uiProvider.createDynamicControls(params, newParams -> {
                params = buildMergedParams();
                familyParamsMap.put(params.getFamilyName(), params);
                updateDynamicParameterControlsValues();
                regenerateAsteroid();
            });
            dynamicParamBox.getChildren().add(controls);
        }
        // (If you have families with ad-hoc controls, add here as needed)
    }

    public void regenerateAsteroid() {
        AsteroidMeshProvider meshProvider = AsteroidMeshProvider.PROVIDERS.get(params.getFamilyName());
        if (meshProvider == null) {
            meshProvider = new AsteroidMeshProvider.Default();
        }
        AsteroidGenerator generator = new AsteroidGenerator(meshProvider, params);
        TriangleMesh mesh = generator.generateAsteroid();
        asteroidView.setMesh(mesh);
        asteroidLinesView.setMesh(mesh);
    }

    private void updateSharedControlValues() {
        radiusSlider.setValue(params.getRadius());
        subdivSpinner.getValueFactory().setValue(params.getSubdivisions());
        deformSlider.setValue(params.getDeformation());
        seedField.setText(Long.toString(params.getSeed()));
        updateDynamicParameterControlsValues();
    }

    private void updateDynamicParameterControlsValues() {
        if (currentProvider instanceof AsteroidFamilyUI uiProvider) {
            uiProvider.setControlsFromParams(params);
        }
        // Add logic for non-UI family controls if needed
    }

    /** Always merge shared and dynamic controls before regenerating asteroid. */
    private AsteroidParameters buildMergedParams() {
        AsteroidParameters familyParams = (currentProvider instanceof AsteroidFamilyUI uiProvider)
                ? uiProvider.getParamsFromControls()
                : params;

        AsteroidParameters.Builder<?> builder = familyParams.toBuilder();
        builder
                .radius(radiusSlider.getValue())
                .subdivisions(subdivSpinner.getValue())
                .deformation(deformSlider.getValue())
                .seed(getCurrentSeed())
                .familyName(familyParams.getFamilyName());
        return builder.build();
    }

    private long getCurrentSeed() {
        try {
            return Long.parseLong(seedField.getText());
        } catch (Exception e) {
            return params.getSeed();
        }
    }

    private AsteroidParameters buildParamsForFamily(String family, AsteroidParameters oldParams) {
        AsteroidMeshProvider provider = AsteroidMeshProvider.PROVIDERS.get(family);
        if (provider instanceof AsteroidFamilyUI uiProvider) {
            return uiProvider.buildDefaultParamsFrom(oldParams);
        }
        // ...handle other family types here if needed...
        return new AsteroidParameters.Builder<>()
                .radius(oldParams.getRadius())
                .subdivisions(oldParams.getSubdivisions())
                .deformation(oldParams.getDeformation())
                .seed(oldParams.getSeed())
                .familyName(family)
                .build();
    }
}
