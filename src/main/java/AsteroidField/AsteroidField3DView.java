package AsteroidField;

import AsteroidField.asteroids.AsteroidFamilyUI;
import AsteroidField.asteroids.AsteroidGenerator;
import AsteroidField.asteroids.parameters.AsteroidParameters;
import AsteroidField.asteroids.providers.AsteroidMeshProvider;
import AsteroidField.physics.CraftIntegratorContributor;
import AsteroidField.physics.PhysicsSystem;
import AsteroidField.spacecraft.FancyCraft;
import AsteroidField.spacecraft.collision.SpacecraftCollisionContributor;
import AsteroidField.spacecraft.CameraKinematicAdapter;
import AsteroidField.tether.TetherController;
import AsteroidField.tether.TetherTuningUI;
import AsteroidField.spacecraft.control.ThrusterController;
import AsteroidField.util.AsteroidUtils;
import AsteroidField.util.FpsLookController;
import AsteroidField.util.TrackBallController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * Main 3D view. Always-on PhysicsSystem orchestrates contributors; tethers are a feature controller.
 */
public class AsteroidField3DView extends Pane {

    private SubScene subScene;
    private PerspectiveCamera camera;
    private Group world;

    // Primary asteroid + wireframe
    private MeshView asteroidView;
    private MeshView asteroidLinesView;

    // Multi-asteroid support
    private final List<MeshView> asteroidViews = new ArrayList<>();
    private final List<MeshView> asteroidWireViews = new ArrayList<>();

    private AsteroidParameters params;
    private AsteroidMeshProvider currentProvider = AsteroidMeshProvider.PROVIDERS.values().iterator().next();
    private final Map<String, AsteroidParameters> familyParamsMap = new HashMap<>();
    private final Random seedRng = new Random();

    // Shared control fields
    private Slider radiusSlider;
    private Spinner<Integer> subdivSpinner;
    private Slider deformSlider;
    private TextField seedField;
    private MeshView selectedAsteroid = null;

    // Family-specific controls area
    private final HBox dynamicParamBox = new HBox(10);

    // Controllers / toggles
    private ToggleButton cameraModeToggle;
    private ToggleButton fpsLookToggle;
    private Button resetCameraBtn;
    private ComboBox<String> asteroidSelectorBox;

    private ThrusterController thrusterController;
    private TrackBallController trackballController;
    private FpsLookController fpsLook;
    private Node craftProxy;

    // Physics + features
    private PhysicsSystem physics;
    private TetherController tetherController;
    private SpacecraftCollisionContributor shipCollisions;
    private CameraKinematicAdapter cameraCraft;
    private ToggleButton tetherToggle;

    // UI state / guards
    private boolean updatingAsteroidCombo = false;
    private boolean wireframeVisible = true;

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

        // Primary asteroid (fill)
        asteroidView = new MeshView();
        asteroidView.setMaterial(new PhongMaterial(Color.GAINSBORO));
        asteroidView.setDrawMode(DrawMode.FILL);
        asteroidView.setCullFace(CullFace.NONE);
        world.getChildren().add(asteroidView);

        // Primary wireframe
        asteroidLinesView = new MeshView();
        asteroidLinesView.setMaterial(new PhongMaterial(Color.ALICEBLUE));
        asteroidLinesView.setDrawMode(DrawMode.LINE);
        asteroidLinesView.setMouseTransparent(true);
        asteroidLinesView.setCullFace(CullFace.NONE);
        world.getChildren().add(asteroidLinesView);

        // Mirror wireframe pose to mesh
        AsteroidUtils.bindPose(asteroidLinesView, asteroidView);

        regenerateAsteroid();

        // Track selectable asteroids (include primary)
        asteroidViews.add(asteroidView);
        asteroidWireViews.add(asteroidLinesView);
        selectedAsteroid = asteroidView;

        camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);
        camera.setFieldOfView(45);
        camera.setTranslateZ(-600);

        subScene = new SubScene(world, 800, 600, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.BLACK);
        subScene.setCamera(camera);

        // Camera as kinematic craft for physics
        cameraCraft = CameraKinematicAdapter.attach(subScene, world);
        cameraCraft.setMass(1.5);
        cameraCraft.setLinearDampingPerSecond(0.18);
        cameraCraft.setMaxSpeed(650);

        // FPS mouse look controller (default ON)
        fpsLook = new FpsLookController(subScene, camera);
        fpsLook.setEnabled(true);
        fpsLook.setSensitivity(0.20);
        fpsLook.setSmoothing(0.35);
        fpsLook.setPitchLimits(-85, 85);
        fpsLook.setYawPitch(0, 0);

        // Trackball controller (OFF by default since FPS look is default)
        trackballController = new TrackBallController(subScene, camera, asteroidViews);
        trackballController.setEnabled(false);

        // Craft proxy (debug vis, used in mini-cams)
        FancyCraft fancy = new FancyCraft();
        fancy.setScale(0.8);
        fancy.setVisible(false);
        world.getChildren().add(fancy);
        AsteroidUtils.bindNodePose(fancy, getCameraFrameNode());
        this.craftProxy = fancy;

        // --- PHYSICS WIRING (always on) ---
        physics = new PhysicsSystem(120); // 120 Hz fixed-step
        physics.setEnabled(true);

        // Collidables supplier (MeshViews only) – used by tethers & collisions
        java.util.function.Supplier<java.util.List<javafx.scene.Node>> collidables =
                () -> new ArrayList<>(asteroidViews);

        // 1) Thrusters (forces from input)
        thrusterController = new ThrusterController(subScene, camera, cameraCraft);
        thrusterController.setEnabled(true);
        thrusterController.setThrustPower(480);
        thrusterController.setVerticalPower(360);
        thrusterController.setBrakePower(1400);
        thrusterController.setDampenerPower(220);
        thrusterController.setLookSensitivity(0.0); // keep FPSLook in charge
        physics.addContributor(thrusterController);

        // 2) Tethers (spring forces + input gated by UI toggle)
        tetherController = new TetherController(subScene, camera, world, collidables, cameraCraft);
        tetherController.setSymmetricWingOffsets(20, 50, 5);
        tetherController.setTetherInputEnabled(false); // off by default; toggle controls this
        for (int i = 0; i < 2; i++) {
            var t = tetherController.getTether(i);
            if (t != null) {
                t.setRayFrontFaceOnly(true);
                t.setAllowAabbFallbackOnMeshMiss(false);
                t.setStiffness(160);
                t.setDampingRatio(0.9);
                t.setMaxForce(900);
                t.setSlackEps(0.02);
                t.setReelRate(240);
                t.setShowEndMarker(true);
                t.setDebugPersistOnMiss(true);
            }
        }
        physics.addContributor(tetherController);

        // 3) Craft integration (apply accumulated forces)
        physics.addContributor(new CraftIntegratorContributor(cameraCraft));

        // 4) Ship collisions (resolve & correct)
        shipCollisions = new SpacecraftCollisionContributor(world, cameraCraft, collidables, 1.5);
        shipCollisions.setFrontFaceOnly(true); // set false if your windings vary
        shipCollisions.setMaxIterations(2);
        shipCollisions.setRestitution(0.05);
        shipCollisions.setFriction(0.15);
        physics.addContributor(shipCollisions);

        // Finish scene graph
        getChildren().add(subScene);
        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());
    }

    // --- TOP BAR: Scene/Application Controls ---
    public HBox createSceneControls() {
        HBox topBar = new HBox(10);
        topBar.setId("scene-controls");

        // FPS Look toggle (default ON)
        fpsLookToggle = new ToggleButton("FPS Look");
        fpsLookToggle.setSelected(true);
        fpsLookToggle.setTooltip(new Tooltip("Mouse controls view like an FPS. Click to capture, ESC to release."));
        fpsLookToggle.selectedProperty().addListener((obs, was, is) -> {
            if (fpsLook != null) {
                fpsLook.setEnabled(is);
                if (is) fpsLook.capturePointer(); else fpsLook.releasePointer();
            }
        });

        cameraModeToggle = new ToggleButton("Trackball Mode");
        cameraModeToggle.setTooltip(new Tooltip("Toggle between FPS look + thrusters and Trackball camera"));
        cameraModeToggle.setOnAction(e -> {
            boolean trackballMode = cameraModeToggle.isSelected();
            if (trackballMode) {
                // Enable trackball; disable FPS look & thrusters
                if (fpsLook != null) { fpsLook.setEnabled(false); fpsLookToggle.setSelected(false); }
                thrusterController.setEnabled(false);
                trackballController.setEnabled(true);
                trackballController.setSelected(selectedAsteroid);
            } else {
                // Enable FPS look & thrusters; disable trackball
                trackballController.setEnabled(false);
                thrusterController.setEnabled(true);
                if (fpsLook != null) { fpsLook.setEnabled(true); fpsLookToggle.setSelected(true); fpsLook.capturePointer(); }
            }
        });

        resetCameraBtn = new Button("Reset Camera");
        resetCameraBtn.setTooltip(new Tooltip("Reset camera & tethers to default"));
        resetCameraBtn.setOnAction(e -> {
            // Reset craft position/velocity (keeps inertial model clean)
            cameraCraft.resetPosition(0, 0, -600);

            // Release all tethers
            if (tetherController != null) tetherController.releaseAll();

            // Reset FPS yaw/pitch (do NOT manipulate camera.setRotate directly)
            if (fpsLook != null) {
                fpsLook.setYawPitch(0, 0);
                if (fpsLook.isEnabled()) fpsLook.capturePointer();
            }

            // If trackball is active, also clear any selected asteroid rotation
            if (selectedAsteroid != null && trackballController.isEnabled()) {
                selectedAsteroid.setRotate(0);
            }
        });

        ToggleButton wireframeToggle = new ToggleButton("Show Wireframe");
        wireframeToggle.setSelected(true);
        wireframeToggle.setTooltip(new Tooltip("Show/hide asteroid wireframe overlay"));
        wireframeToggle.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            wireframeVisible = isSelected;
            for (MeshView mv : asteroidWireViews) mv.setVisible(isSelected);
        });
        for (MeshView mv : asteroidWireViews) mv.setVisible(wireframeToggle.isSelected());
        wireframeVisible = wireframeToggle.isSelected();

        tetherToggle = new ToggleButton("Tethers (beta)");
        tetherToggle.setTooltip(new Tooltip("Enable/disable tether controls. LMB/RMB fire. Hold SHIFT to pull. CTRL to release."));
        tetherToggle.selectedProperty().addListener((obs, was, is) -> {
            if (tetherController != null) tetherController.setTetherInputEnabled(is);
        });

        Button spawn3Btn = new Button("Spawn 3 Asteroids (test)");
        spawn3Btn.setTooltip(new Tooltip("Keep primary and add two more at spaced positions"));
        spawn3Btn.setOnAction(e -> spawnTestAsteroids());

        Button clearExtrasBtn = new Button("Clear Extras");
        clearExtrasBtn.setTooltip(new Tooltip("Remove extra asteroids and keep only the primary"));
        clearExtrasBtn.setOnAction(e -> clearExtraAsteroids());

        asteroidSelectorBox = new ComboBox<>();
        updateAsteroidSelectorBox();
        asteroidSelectorBox.setTooltip(new Tooltip("Select asteroid to rotate or highlight"));
        asteroidSelectorBox.setOnAction(e -> {
            if (updatingAsteroidCombo) return; // guard recursion
            int idx = asteroidSelectorBox.getSelectionModel().getSelectedIndex();
            if (idx >= 0 && idx < asteroidViews.size()) {
                setSelectedAsteroid(asteroidViews.get(idx));
            }
        });

        ToggleButton showCraft = new ToggleButton("Show Craft Proxy");
        showCraft.setSelected(false);
        showCraft.setTooltip(new Tooltip("Toggle ship proxy for debugging (shown in mini-cams)"));
        showCraft.selectedProperty().addListener((o, was, isSel) -> {
            if (getDebugCraft() != null) getDebugCraft().setVisible(isSel);
        });

        topBar.getChildren().addAll(
                fpsLookToggle,
                cameraModeToggle,
                resetCameraBtn,
                wireframeToggle,
                tetherToggle,
                spawn3Btn,
                clearExtrasBtn,
                showCraft,
                new Label("Asteroid:"),
                asteroidSelectorBox
        );
        return topBar;
    }

    private void updateAsteroidSelectorBox() {
        if (asteroidSelectorBox == null) return;
        updatingAsteroidCombo = true;
        try {
            asteroidSelectorBox.getItems().clear();
            for (int i = 0; i < asteroidViews.size(); i++) {
                asteroidSelectorBox.getItems().add("Asteroid " + (i + 1));
            }
            int selIndex = (selectedAsteroid != null) ? asteroidViews.indexOf(selectedAsteroid) : -1;
            if (selIndex < 0 && !asteroidViews.isEmpty()) selIndex = 0;
            asteroidSelectorBox.getSelectionModel().select(selIndex);
        } finally {
            updatingAsteroidCombo = false;
        }
    }

    public void setSelectedAsteroid(MeshView meshView) {
        if (selectedAsteroid == meshView) {
            updatingAsteroidCombo = true;
            try {
                int idx = asteroidViews.indexOf(selectedAsteroid);
                if (idx >= 0) asteroidSelectorBox.getSelectionModel().select(idx);
            } finally {
                updatingAsteroidCombo = false;
            }
            return;
        }
        if (selectedAsteroid != null) {
            selectedAsteroid.setMaterial(new PhongMaterial(Color.GAINSBORO));
        }
        selectedAsteroid = meshView;
        if (selectedAsteroid != null) {
            selectedAsteroid.setMaterial(new PhongMaterial(Color.LIGHTGOLDENRODYELLOW));
            trackballController.setSelected(selectedAsteroid);
        }
        updatingAsteroidCombo = true;
        try {
            int idx = asteroidViews.indexOf(selectedAsteroid);
            if (idx >= 0) asteroidSelectorBox.getSelectionModel().select(idx);
        } finally {
            updatingAsteroidCombo = false;
        }
    }

    /** Adds an asteroid (and a bound wireframe) to the world and UI lists. */
    public void addAsteroid(MeshView meshView) {
        MeshView wire = AsteroidUtils.createWireframeFor(meshView, Color.ALICEBLUE, wireframeVisible);
        asteroidViews.add(meshView);
        asteroidWireViews.add(wire);
        world.getChildren().addAll(meshView, wire);
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
            if (newParams == null) newParams = AsteroidUtils.buildParamsForFamily(selected, params);
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
                randomizeBtn
        );
        return hbox;
    }

    // Side Bar: Family Controls
    public VBox getDynamicFamilyBox() {
        VBox box = new VBox(10, new Label("Family Controls"), dynamicParamBox);
        box.setMinWidth(220);
        return box;
    }

    // Side Bar Tether Controls
    public VBox getTetherTuningBox() {
        // Uses TetherController rather than the old TetherSystem
        return TetherTuningUI.build(tetherController);
    }

    /** Update family-specific parameter controls. */
    private void updateDynamicParameterControls() {
        dynamicParamBox.getChildren().clear();
        if (currentProvider instanceof AsteroidFamilyUI uiProvider) {
            Node controls = uiProvider.createDynamicControls(params, newParams -> {
                params = buildMergedParams();
                familyParamsMap.put(params.getFamilyName(), params);
                updateDynamicParameterControlsValues();
                regenerateAsteroid();
            });
            dynamicParamBox.getChildren().add(controls);
        }
    }

    /** Regenerates ONLY the primary asteroid from current params. */
    public void regenerateAsteroid() {
        AsteroidMeshProvider provider = AsteroidMeshProvider.PROVIDERS.get(params.getFamilyName());
        if (provider == null) provider = new AsteroidMeshProvider.Default();
        AsteroidGenerator generator = new AsteroidGenerator(provider, params);
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
    }

    /** Always merge shared and dynamic controls before regenerating asteroid. */
    private AsteroidParameters buildMergedParams() {
        AsteroidParameters familyParams = (currentProvider instanceof AsteroidFamilyUI uiProvider)
                ? uiProvider.getParamsFromControls()
                : params;

        AsteroidParameters.Builder<?> builder = familyParams.toBuilder();
        builder.radius(radiusSlider.getValue())
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

    /** Spawn three extra asteroids (for a total of four) at fixed test positions. */
    private void spawnTestAsteroids() {
        Point3D[] positions = new Point3D[] {
            new Point3D( 500,  300,   0),
            new Point3D(-500,  -50, 300),
            new Point3D( 500, -300,   0)
        };
        double[] radiusScales = new double[] { 0.9, 0.8, 0.8 };

        while (asteroidViews.size() < 4) {
            int extraIndex = asteroidViews.size() - 1;
            int slot = Math.max(0, Math.min(extraIndex, positions.length - 1));
            AsteroidParameters p = params.toBuilder()
                    .seed(seedRng.nextLong())
                    .radius(params.getRadius() * radiusScales[slot])
                    .build();
            MeshView ax = AsteroidUtils.createAsteroidAt(p, positions[slot]);
            addAsteroid(ax);
        }

        updatingAsteroidCombo = true;
        try {
            int selIndex = asteroidViews.indexOf(selectedAsteroid);
            if (selIndex < 0) selIndex = 0;
            asteroidSelectorBox.getSelectionModel().select(selIndex);
        } finally {
            updatingAsteroidCombo = false;
        }
    }

    /** Remove any extra asteroids and keep only the primary. */
    private void clearExtraAsteroids() {
        while (asteroidViews.size() > 1) {
            int last = asteroidViews.size() - 1;
            MeshView mesh = asteroidViews.remove(last);
            MeshView wire = asteroidWireViews.remove(last);
            world.getChildren().removeAll(mesh, wire);
        }
        updatingAsteroidCombo = true;
        try {
            selectedAsteroid = asteroidViews.get(0);
            asteroidSelectorBox.getSelectionModel().select(0);
        } finally {
            updatingAsteroidCombo = false;
        }
    }

    // --- Exposed getters for other UI parts ---
    public SubScene getSubScene() { return subScene; }
    public Group getWorldRoot() { return world; }
    public PerspectiveCamera getMainCamera() { return camera; }
    public void addCameraNode(Node cam) { world.getChildren().add(cam); }
    public Node getCameraFrameNode() { return cameraCraft.getRigNode(); }
    public Node getDebugCraft() { return craftProxy; }
    public void setDebugCraft(Node proxy) { this.craftProxy = proxy; }
}
