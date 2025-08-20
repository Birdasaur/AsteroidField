package AsteroidField;

import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.SceneAntialiasing;
import java.util.Random;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.TriangleMesh;

public class AsteroidField3DView extends Pane {

    private SubScene subScene;
    private PerspectiveCamera camera;
    private Group world;
    private MeshView asteroidView;
    private MeshView asteroidLinesView;
    private AsteroidParameters params;
    private AsteroidMeshProvider currentProvider = AsteroidMeshProvider.PROVIDERS.values().iterator().next(); // default

    private final Random seedRng = new Random();

    public AsteroidField3DView() {
        world = new Group();

        // Initial parameters
        params = new AsteroidParameters.Builder<>()
                .radius(100)
                .subdivisions(2)
                .deformation(0.3)
                .seed(System.nanoTime())
                .familyName("Demo")
                .build();

        // Generate the asteroid mesh and add it to the scene
        asteroidView = new MeshView();
        asteroidView.setMaterial(new PhongMaterial(Color.GAINSBORO));
        asteroidView.setDrawMode(DrawMode.FILL);
        world.getChildren().add(asteroidView);

        asteroidLinesView = new MeshView();
        asteroidLinesView.setMaterial(new PhongMaterial(Color.ALICEBLUE));
        asteroidLinesView.setDrawMode(DrawMode.LINE);
        world.getChildren().add(asteroidLinesView);

        regenerateAsteroid();

        // Camera setup
        camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);
        camera.setTranslateZ(-600);

        subScene = new SubScene(world, 800, 600, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.BLACK);
        subScene.setCamera(camera);

        FlyCameraController controls3D = new FlyCameraController(camera, subScene);
        controls3D.setSpeed(150);
        controls3D.setBoostMultiplier(3);
        controls3D.setSensitivity(0.2);

        getChildren().add(subScene);
        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());
    }

    /**
     * Regenerates the MeshView with the current parameters.
     */
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

    /**
     * Returns a fully configured HBox with controls for all parameters.
     */
    public HBox createControls() {
        HBox hbox = new HBox(10);
        // --- Family ComboBox ---
        Label familyLabel = new Label("Family:");
        ComboBox<String> familyBox = new ComboBox<>();
        familyBox.getItems().addAll(AsteroidMeshProvider.PROVIDERS.keySet());
        familyBox.getSelectionModel().selectFirst();

        familyBox.setOnAction(e -> {
            String selected = familyBox.getValue();
            currentProvider = AsteroidMeshProvider.PROVIDERS.get(selected);
            params = new AsteroidParameters.Builder<>()
                    .radius(params.getRadius())
                    .subdivisions(params.getSubdivisions())
                    .deformation(params.getDeformation())
                    .seed(params.getSeed())
                    .familyName(selected)
                    .build();
            regenerateAsteroid();
        });
        // Radius slider
        Label radiusLabel = new Label("Radius:");
        Slider radiusSlider = new Slider(20, 300, params.getRadius());
        radiusSlider.setShowTickLabels(true);
        radiusSlider.valueProperty().addListener((obs, oldV, newV) -> {
            params = new AsteroidParameters.Builder<>()
                    .radius(newV.doubleValue())
                    .subdivisions(params.getSubdivisions())
                    .deformation(params.getDeformation())
                    .seed(params.getSeed())
                    .familyName(params.getFamilyName())
                    .build();
            regenerateAsteroid();
        });

        // Subdivisions spinner
        Label subdivLabel = new Label("Subdiv:");
        Spinner<Integer> subdivSpinner = new Spinner<>();
        subdivSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 5, params.getSubdivisions()));
        subdivSpinner.valueProperty().addListener((obs, oldV, newV) -> {
            params = new AsteroidParameters.Builder<>()
                    .radius(params.getRadius())
                    .subdivisions(newV)
                    .deformation(params.getDeformation())
                    .seed(params.getSeed())
                    .familyName(params.getFamilyName())
                    .build();
            regenerateAsteroid();
        });

        // Deformation slider
        Label deformLabel = new Label("Deform:");
        Slider deformSlider = new Slider(0, 1, params.getDeformation());
        deformSlider.setShowTickLabels(true);
        deformSlider.valueProperty().addListener((obs, oldV, newV) -> {
            params = new AsteroidParameters.Builder<>()
                    .radius(params.getRadius())
                    .subdivisions(params.getSubdivisions())
                    .deformation(newV.doubleValue())
                    .seed(params.getSeed())
                    .familyName(params.getFamilyName())
                    .build();
            regenerateAsteroid();
        });

        // Seed field and randomize button
        Label seedLabel = new Label("Seed:");
        TextField seedField = new TextField(Long.toString(params.getSeed()));
        seedField.setPrefWidth(100);
        seedField.setOnAction(e -> {
            try {
                long seed = Long.parseLong(seedField.getText());
                params = new AsteroidParameters.Builder<>()
                        .radius(params.getRadius())
                        .subdivisions(params.getSubdivisions())
                        .deformation(params.getDeformation())
                        .seed(seed)
                        .familyName(params.getFamilyName())
                        .build();
                regenerateAsteroid();
            } catch (NumberFormatException ex) {
                seedField.setText(Long.toString(params.getSeed())); // revert
            }
        });

        Button randomizeBtn = new Button("Randomize");
        randomizeBtn.setOnAction(e -> {
            long seed = seedRng.nextLong();
            seedField.setText(Long.toString(seed));
            params = new AsteroidParameters.Builder<>()
                    .radius(params.getRadius())
                    .subdivisions(params.getSubdivisions())
                    .deformation(params.getDeformation())
                    .seed(seed)
                    .familyName(params.getFamilyName())
                    .build();
            regenerateAsteroid();
        });

        hbox.getChildren().addAll(
                new VBox(10, familyLabel, familyBox),
                new VBox(10, radiusLabel, radiusSlider),
                new VBox(10, subdivLabel, subdivSpinner),
                new VBox(10, deformLabel, deformSlider),
                new VBox(10, seedLabel, seedField),
                randomizeBtn
        );

        return hbox;
    }
}
