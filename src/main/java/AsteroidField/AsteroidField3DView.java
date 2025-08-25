package AsteroidField;

import java.util.ArrayList;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.SceneAntialiasing;
import javafx.scene.shape.TriangleMesh;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Tooltip;

public class AsteroidField3DView extends Pane {

    private SubScene subScene;
    private PerspectiveCamera camera;
    private Group world;
    private MeshView asteroidView;
    private MeshView asteroidLinesView;
    private AsteroidParameters params;
    private AsteroidMeshProvider currentProvider = AsteroidMeshProvider.PROVIDERS.values().iterator().next();

    // Store parameters for each family
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

    // Hold cratered controls for reference in onChange (if present)
    private Spinner<Integer> craterCountSpinner;
    private Slider craterDepthSlider;
    private Slider craterWidthSlider;
    
    // Camera/app controls
    private ToggleButton cameraModeToggle;
    private Button resetCameraBtn;
    private ComboBox<String> asteroidSelectorBox;
    // Controllers
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
        // modify only mesh1.getTransforms() from now on

        regenerateAsteroid();
        selectedAsteroid = asteroidView; // default select first

        camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);
        camera.setTranslateZ(-600);

        subScene = new SubScene(world, 800, 600, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.BLACK);
        subScene.setCamera(camera);

        // Camera controllers
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

        asteroidSelectorBox = new ComboBox<>();
        updateAsteroidSelectorBox();
        asteroidSelectorBox.setTooltip(new Tooltip("Select asteroid to rotate or highlight"));

        // Camera toggling logic
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
            // Optionally, reset asteroid rotations if in trackball mode
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

        topBar.getChildren().addAll(cameraModeToggle, resetCameraBtn, new Label("Asteroid:"), asteroidSelectorBox);
        return topBar;
    }

    // When a new asteroid is created/removed, update the ComboBox
    private void updateAsteroidSelectorBox() {
        asteroidSelectorBox.getItems().clear();
        for (int i = 0; i < asteroidViews.size(); i++) {
            asteroidSelectorBox.getItems().add("Asteroid " + (i + 1));
        }
        if (!asteroidViews.isEmpty()) {
            asteroidSelectorBox.getSelectionModel().select(asteroidViews.indexOf(selectedAsteroid));
        }
    }

    // Selection logic for both picking and ComboBox
    public void setSelectedAsteroid(MeshView meshView) {
        if (selectedAsteroid != null) {
            // Un-highlight previous
            selectedAsteroid.setMaterial(new PhongMaterial(Color.GAINSBORO));
        }
        selectedAsteroid = meshView;
        if (selectedAsteroid != null) {
            selectedAsteroid.setMaterial(new PhongMaterial(Color.LIGHTGOLDENRODYELLOW)); // highlight
            trackballController.setSelected(selectedAsteroid);
            updateAsteroidSelectorBox();
        }
    }

    // Example: method to create/add new asteroids (call this when generating a new one)
    public void addAsteroid(MeshView meshView) {
        asteroidViews.add(meshView);
        world.getChildren().add(meshView);
        updateAsteroidSelectorBox();
    }

    // --- BOTTOM BAR: Asteroid Controls (as before) ---
    public HBox createControls() {
        HBox hbox = new HBox(10);

        // --- Family ComboBox ---
        Label familyLabel = new Label("Family:");
        ComboBox<String> familyBox = new ComboBox<>();
        familyBox.getItems().addAll(AsteroidMeshProvider.PROVIDERS.keySet());
        familyBox.getSelectionModel().select("Classic Rocky");

        // --- Shared controls (as fields!) ---
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

        // Listeners for shared controls: always build params from ALL controls (shared + custom)
        Consumer<Void> sharedControlUpdate = unused -> {
            params = buildCurrentParamsForCurrentFamily();
            familyParamsMap.put(params.getFamilyName(), params);
            updateDynamicParameterControlsValues();
            regenerateAsteroid();
        };

        radiusSlider.valueProperty().addListener((obs, oldV, newV) -> sharedControlUpdate.accept(null));
        subdivSpinner.valueProperty().addListener((obs, oldV, newV) -> sharedControlUpdate.accept(null));
        deformSlider.valueProperty().addListener((obs, oldV, newV) -> sharedControlUpdate.accept(null));
        seedField.setOnAction(e -> sharedControlUpdate.accept(null));
        randomizeBtn.setOnAction(e -> {
            long seed = seedRng.nextLong();
            seedField.setText(Long.toString(seed));
            sharedControlUpdate.accept(null);
        });

        // Handle family switching
        familyBox.setOnAction(e -> {
            familyParamsMap.put(params.getFamilyName(), buildCurrentParamsForCurrentFamily());

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

    /** Update family-specific parameter controls. */
    private void updateDynamicParameterControls() {
        dynamicParamBox.getChildren().clear();

        if (params instanceof CrateredAsteroidParameters) {
            CrateredAsteroidParameters c = (CrateredAsteroidParameters) params;

            craterCountSpinner = new Spinner<>();
            craterCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, c.getCraterCount()));
            craterCountSpinner.valueProperty().addListener((obs, oldV, newV) -> {
                params = buildCurrentParamsForCurrentFamily();
                familyParamsMap.put(params.getFamilyName(), params);
                regenerateAsteroid();
            });

            craterDepthSlider = new Slider(0.05, 0.5, c.getCraterDepth());
            craterDepthSlider.setShowTickLabels(true);
            craterDepthSlider.valueProperty().addListener((obs, oldV, newV) -> {
                params = buildCurrentParamsForCurrentFamily();
                familyParamsMap.put(params.getFamilyName(), params);
                regenerateAsteroid();
            });

            craterWidthSlider = new Slider(0.05, 0.6, c.getCraterWidth());
            craterWidthSlider.setShowTickLabels(true);
            craterWidthSlider.valueProperty().addListener((obs, oldV, newV) -> {
                params = buildCurrentParamsForCurrentFamily();
                familyParamsMap.put(params.getFamilyName(), params);
                regenerateAsteroid();
            });

            dynamicParamBox.getChildren().addAll(
                    new VBox(5, new Label("Craters:"), craterCountSpinner),
                    new VBox(5, new Label("Crater Depth:"), craterDepthSlider),
                    new VBox(5, new Label("Crater Width:"), craterWidthSlider)
            );
        }
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
        if (params instanceof CrateredAsteroidParameters && craterCountSpinner != null) {
            CrateredAsteroidParameters c = (CrateredAsteroidParameters) params;
            craterCountSpinner.getValueFactory().setValue(c.getCraterCount());
            craterDepthSlider.setValue(c.getCraterDepth());
            craterWidthSlider.setValue(c.getCraterWidth());
        }
    }

    private AsteroidParameters buildCurrentParamsForCurrentFamily() {
        String family = currentProvider == null ? params.getFamilyName() : currentProvider.getClass().getSimpleName();
        family = params.getFamilyName();

        double radius = radiusSlider.getValue();
        int subdivisions = subdivSpinner.getValue();
        double deform = deformSlider.getValue();
        long seed;
        try { seed = Long.parseLong(seedField.getText()); }
        catch (Exception e) { seed = params.getSeed(); }

        if ("Cratered".equals(family) && craterCountSpinner != null && craterDepthSlider != null && craterWidthSlider != null) {
            int craterCount = craterCountSpinner.getValue();
            double craterDepth = craterDepthSlider.getValue();
            double craterWidth = craterWidthSlider.getValue();
            return new CrateredAsteroidParameters.Builder()
                    .radius(radius)
                    .subdivisions(subdivisions)
                    .deformation(deform)
                    .seed(seed)
                    .familyName(family)
                    .craterCount(craterCount)
                    .craterDepth(craterDepth)
                    .craterWidth(craterWidth)
                    .build();
        } else {
            return new AsteroidParameters.Builder<>()
                    .radius(radius)
                    .subdivisions(subdivisions)
                    .deformation(deform)
                    .seed(seed)
                    .familyName(family)
                    .build();
        }
    }

    private AsteroidParameters buildParamsForFamily(String family, AsteroidParameters oldParams) {
        if ("Cratered".equals(family)) {
            int craterCount = 5;
            double craterDepth = 0.2;
            double craterWidth = 0.2;
            if (oldParams instanceof CrateredAsteroidParameters) {
                CrateredAsteroidParameters c = (CrateredAsteroidParameters) oldParams;
                craterCount = c.getCraterCount();
                craterDepth = c.getCraterDepth();
                craterWidth = c.getCraterWidth();
            }
            return new CrateredAsteroidParameters.Builder()
                    .radius(oldParams.getRadius())
                    .subdivisions(oldParams.getSubdivisions())
                    .deformation(oldParams.getDeformation())
                    .seed(oldParams.getSeed())
                    .familyName(family)
                    .craterCount(craterCount)
                    .craterDepth(craterDepth)
                    .craterWidth(craterWidth)
                    .build();
        } else {
            return new AsteroidParameters.Builder<>()
                    .radius(oldParams.getRadius())
                    .subdivisions(oldParams.getSubdivisions())
                    .deformation(oldParams.getDeformation())
                    .seed(oldParams.getSeed())
                    .familyName(family)
                    .build();
        }
    }
}
