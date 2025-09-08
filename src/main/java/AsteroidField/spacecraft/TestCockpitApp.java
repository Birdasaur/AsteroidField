package AsteroidField.spacecraft;

import javafx.application.Application;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;

public class TestCockpitApp extends Application {

    @Override
    public void start(Stage stage) {
        // --- 3D world root
        Group root3D = new Group();
        root3D.setFocusTraversable(true);

        // Reference cube at origin (20x20x20)
        Box cube = new Box(20, 20, 20);
        cube.setMaterial(new PhongMaterial(Color.DODGERBLUE));
        cube.setOpacity(0.85);
        root3D.getChildren().add(cube);

        // Axes gizmo (toggleable)
        Group axes = buildAxes(2000, 4);
        axes.setVisible(false);
        root3D.getChildren().add(axes);

        // Lights
        AmbientLight ambient = new AmbientLight(Color.WHITE);
        PointLight key = new PointLight(Color.WHITE);
        key.setTranslateX(800);
        key.setTranslateY(-600);
        key.setTranslateZ(-600);
        root3D.getChildren().addAll(ambient, key);

        // Camera + free-look rig
        PerspectiveCamera cam = new PerspectiveCamera(true);
        cam.setNearClip(0.1);
        cam.setFarClip(20000);
        cam.setFieldOfView(60);

        FreeLookCameraRig rig = new FreeLookCameraRig(cam);
        rig.setTranslate(0, 0, -2000); // start behind origin
        root3D.getChildren().add(rig.getRoot());

        // --- Dome cockpit centered at origin
        CockpitDome3D cockpit = new CockpitDome3D();
        root3D.getChildren().add(cockpit);

        // SubScene
        SubScene subScene = new SubScene(root3D, 1200, 800, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.ALICEBLUE);
        subScene.setCamera(cam);

        // =======================
        // UI CONTROLS (ToolBar)
        // =======================

        // Visibility & debug toggles
        ToggleButton showCockpit = new ToggleButton("Show Cockpit");
        showCockpit.setSelected(true);
        showCockpit.setOnAction(e -> cockpit.setVisible(showCockpit.isSelected()));

        CheckBox markers = new CheckBox("Markers");
        markers.setSelected(false);
        markers.selectedProperty().addListener((o, was, is) -> cockpit.setShowMarkers(is));

        CheckBox showAxes = new CheckBox("Axes");
        showAxes.selectedProperty().addListener((o, was, is) -> axes.setVisible(is));

        CheckBox wireCube = new CheckBox("Wireframe cube");
        wireCube.selectedProperty().addListener((o, was, is) -> {
            cube.setDrawMode(is ? DrawMode.LINE : DrawMode.FILL);
            cube.setOpacity(is ? 1.0 : 0.85);
        });

        CheckBox baseRing = new CheckBox("Base Ring");
        baseRing.setSelected(true);
        baseRing.selectedProperty().addListener((o, was, is) -> cockpit.setIncludeBaseRing(is));

        // Geometry: radius & bar thickness (rebuild – not scale)
        Label rLabel = new Label(" R: 200 ");
        Slider rSlider = new Slider(20, 1500, 200);
        rSlider.setPrefWidth(260);

        Label tLabel = new Label(" T: 8.0 ");
        Slider tSlider = new Slider(0.5, 60, 8);
        tSlider.setPrefWidth(220);

        rSlider.valueProperty().addListener((o, ov, nv) -> {
            cockpit.rebuild(nv.doubleValue(), tSlider.getValue());
            rLabel.setText(String.format(" R: %.0f ", nv.doubleValue()));
        });
        tSlider.valueProperty().addListener((o, ov, nv) -> {
            cockpit.rebuild(rSlider.getValue(), nv.doubleValue());
            tLabel.setText(String.format(" T: %.1f ", nv.doubleValue()));
        });

        // Structure: meridians (octagon = 8), latitude rings (0..6)
        Label mLabel = new Label(" Meridians: 8 ");
        Slider mSlider = new Slider(6, 64, 8);
        mSlider.setMajorTickUnit(2);
        mSlider.setSnapToTicks(true);
        mSlider.setPrefWidth(220);
        mSlider.valueProperty().addListener((o, ov, nv) -> {
            int m = nv.intValue();
            cockpit.setMeridians(m);
            mLabel.setText(" Meridians: " + m + " ");
        });

        Label lLabel = new Label(" Lat Rings: 2 ");
        Slider lSlider = new Slider(0, 6, 2);
        lSlider.setMajorTickUnit(1);
        lSlider.setSnapToTicks(true);
        lSlider.setPrefWidth(180);
        lSlider.valueProperty().addListener((o, ov, nv) -> {
            int r = nv.intValue();
            cockpit.setLatitudeRings(r);
            lLabel.setText(" Lat Rings: " + r + " ");
        });

        // Material controls: opacity + color
        Label aLabel = new Label(" Opacity: 1.00 ");
        Slider aSlider = new Slider(0.1, 1.0, 1.0);
        aSlider.setPrefWidth(160);
        aSlider.valueProperty().addListener((o, ov, nv) -> {
            cockpit.setBarOpacity(nv.doubleValue());
            aLabel.setText(String.format(" Opacity: %.2f ", nv.doubleValue()));
        });

        ColorPicker colorPicker = new ColorPicker(Color.CYAN);
        colorPicker.setOnAction(e -> cockpit.setBarColor(colorPicker.getValue()));

        // Camera reset
        Button resetCam = new Button("Reset Cam");
        resetCam.setOnAction(e -> rig.setTranslate(0, 0, -2000));

        ToolBar toolbar = new ToolBar(
                showCockpit, markers, showAxes, wireCube, baseRing,
                new Separator(),
                rLabel, new Label("Radius:"), rSlider,
                tLabel, new Label("Thick:"), tSlider,
                new Separator(),
                mLabel, mSlider,
                lLabel, lSlider,
                new Separator(),
                aLabel, aSlider, new Label(" Color:"), colorPicker,
                new Separator(),
                resetCam,
                new Label("   (Hold RMB or press 'M' to look; WASD/R/F, Shift = sprint)")
        );

        // Layout
        StackPane stack = new StackPane(subScene);
        BorderPane root = new BorderPane(stack);
        root.setTop(toolbar);

        Scene scene = new Scene(root, 1200, 860, true);
        stage.setScene(scene);
        stage.setTitle("TestCockpitApp — Dome Cockpit + Free-Look (WASD/R/F, RMB)");
        stage.show();

        // Route input to the free-look rig
        rig.start(subScene);

        // Click to ensure focus for key events
        scene.setOnMouseClicked(e -> root3D.requestFocus());

        // Quick UI shortcut: space toggles markers, B toggles base ring
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE) {
                boolean next = !markers.isSelected();
                markers.setSelected(next);
            } else if (e.getCode() == KeyCode.B) {
                boolean next = !baseRing.isSelected();
                baseRing.setSelected(next);
            }
        });
    }

    // RGB axes centered at origin (explicit per-node transforms)
    private Group buildAxes(double length, double radius) {
        Cylinder x = new Cylinder(radius, length);
        x.setMaterial(new PhongMaterial(Color.RED));
        x.setRotationAxis(Rotate.Z_AXIS);
        x.setRotate(90);
        x.setTranslateX(length / 2);

        Cylinder y = new Cylinder(radius, length);
        y.setMaterial(new PhongMaterial(Color.LIMEGREEN));
        y.setTranslateY(-length / 2);

        Cylinder z = new Cylinder(radius, length);
        z.setMaterial(new PhongMaterial(Color.CORNFLOWERBLUE));
        z.setRotationAxis(Rotate.X_AXIS);
        z.setRotate(90);
        z.setTranslateZ(length / 2);

        return new Group(x, y, z);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
