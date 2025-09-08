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
        // Good cinematic defaults
        cockpit.setMeridians(8);
        cockpit.setLatitudeRings(2);
        cockpit.setLatitudeRingsLower(2);
        cockpit.setFull360(true);
        cockpit.setIncludeBaseRing(true);
        cockpit.setFrontBias(2.0, 28.0);
        cockpit.setIncludeAPillars(true);
        cockpit.setAPillarParams(24.0, 18.0, 62.0, 2.2);
        root3D.getChildren().add(cockpit);

        // SubScene (route input to this, not to Scene)
        SubScene subScene = new SubScene(root3D, 1280, 800, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.ALICEBLUE);
        subScene.setCamera(cam);

        // =======================
        // UI CONTROLS (ToolBar)
        // =======================

        // Visibility & debug
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

        CheckBox fullWrap = new CheckBox("360° Wrap");
        fullWrap.setSelected(true);
        fullWrap.selectedProperty().addListener((o, was, is) -> cockpit.setFull360(is));

        // Geometry: radius & bar thickness (rebuild – not scale)
        Label rLabel = new Label(" R: 200 ");
        Slider rSlider = new Slider(20, 1500, 200);
        rSlider.setPrefWidth(220);

        Label tLabel = new Label(" T: 8.0 ");
        Slider tSlider = new Slider(0.5, 60, 8);
        tSlider.setPrefWidth(180);

        rSlider.valueProperty().addListener((o, ov, nv) -> {
            cockpit.rebuild(nv.doubleValue(), tSlider.getValue());
            rLabel.setText(String.format(" R: %.0f ", nv.doubleValue()));
        });
        tSlider.valueProperty().addListener((o, ov, nv) -> {
            cockpit.rebuild(rSlider.getValue(), nv.doubleValue());
            tLabel.setText(String.format(" T: %.1f ", nv.doubleValue()));
        });

        // Structure: meridians (octagon = 8), upper/lower latitude rings
        Label mLabel = new Label(" Meridians: 8 ");
        Slider mSlider = new Slider(6, 64, 8);
        mSlider.setMajorTickUnit(2);
        mSlider.setSnapToTicks(true);
        mSlider.setPrefWidth(160);
        mSlider.valueProperty().addListener((o, ov, nv) -> {
            int m = nv.intValue();
            cockpit.setMeridians(m);
            mLabel.setText(" Meridians: " + m + " ");
        });

        Label luLabel = new Label(" Upper Rings: 2 ");
        Slider luSlider = new Slider(0, 6, 2);
        luSlider.setMajorTickUnit(1);
        luSlider.setSnapToTicks(true);
        luSlider.setPrefWidth(140);
        luSlider.valueProperty().addListener((o, ov, nv) -> {
            int r = nv.intValue();
            cockpit.setLatitudeRings(r);
            luLabel.setText(" Upper Rings: " + r + " ");
        });

        Label ldLabel = new Label(" Lower Rings: 2 ");
        Slider ldSlider = new Slider(0, 6, 2);
        ldSlider.setMajorTickUnit(1);
        ldSlider.setSnapToTicks(true);
        ldSlider.setPrefWidth(140);
        ldSlider.valueProperty().addListener((o, ov, nv) -> {
            int r = nv.intValue();
            cockpit.setLatitudeRingsLower(r);
            ldLabel.setText(" Lower Rings: " + r + " ");
        });

        // Material controls: opacity + color
        Label aLabel = new Label(" Opacity: 1.00 ");
        Slider aSlider = new Slider(0.1, 1.0, 1.0);
        aSlider.setPrefWidth(140);
        aSlider.valueProperty().addListener((o, ov, nv) -> {
            cockpit.setBarOpacity(nv.doubleValue());
            aLabel.setText(String.format(" Opacity: %.2f ", nv.doubleValue()));
        });

        ColorPicker colorPicker = new ColorPicker(Color.CYAN);
        colorPicker.setOnAction(e -> cockpit.setBarColor(colorPicker.getValue()));

        // Front bias (windshield feel)
        Label fbLabel = new Label(" Front Bias: 2.0× ");
        Slider fbSlider = new Slider(1.0, 4.0, 2.0);
        fbSlider.setPrefWidth(140);
        Label sigmaLabel = new Label(" Width: 28° ");
        Slider sigmaSlider = new Slider(5.0, 90.0, 28.0);
        sigmaSlider.setPrefWidth(140);
        fbSlider.valueProperty().addListener((o, ov, nv) -> {
            cockpit.setFrontBias(nv.doubleValue(), sigmaSlider.getValue());
            fbLabel.setText(String.format(" Front Bias: %.1f× ", nv.doubleValue()));
        });
        sigmaSlider.valueProperty().addListener((o, ov, nv) -> {
            cockpit.setFrontBias(fbSlider.getValue(), nv.doubleValue());
            sigmaLabel.setText(String.format(" Width: %.0f° ", nv.doubleValue()));
        });

        // A-pillars
        CheckBox aPillars = new CheckBox("A-Pillars");
        aPillars.setSelected(true);
        aPillars.selectedProperty().addListener((o, was, is) -> cockpit.setIncludeAPillars(is));

        Label azLabel = new Label(" Az: 24° ");
        Slider azSlider = new Slider(5.0, 60.0, 24.0);
        azSlider.setPrefWidth(120);
        Label topLabel = new Label(" Topθ: 18° ");
        Slider topSlider = new Slider(5.0, 45.0, 18.0);
        topSlider.setPrefWidth(120);
        Label botLabel = new Label(" Botθ: 62° ");
        Slider botSlider = new Slider(30.0, 85.0, 62.0);
        botSlider.setPrefWidth(120);
        Label amLabel = new Label(" Pillar×: 2.2 ");
        Slider amSlider = new Slider(1.0, 4.0, 2.2);
        amSlider.setPrefWidth(120);

        azSlider.valueProperty().addListener((o, ov, nv) -> {
            cockpit.setAPillarParams(nv.doubleValue(), topSlider.getValue(), botSlider.getValue(), amSlider.getValue());
            azLabel.setText(String.format(" Az: %.0f° ", nv.doubleValue()));
        });
        topSlider.valueProperty().addListener((o, ov, nv) -> {
            cockpit.setAPillarParams(azSlider.getValue(), nv.doubleValue(), botSlider.getValue(), amSlider.getValue());
            topLabel.setText(String.format(" Topθ: %.0f° ", nv.doubleValue()));
        });
        botSlider.valueProperty().addListener((o, ov, nv) -> {
            cockpit.setAPillarParams(azSlider.getValue(), topSlider.getValue(), nv.doubleValue(), amSlider.getValue());
            botLabel.setText(String.format(" Botθ: %.0f° ", nv.doubleValue()));
        });
        amSlider.valueProperty().addListener((o, ov, nv) -> {
            cockpit.setAPillarParams(azSlider.getValue(), topSlider.getValue(), botSlider.getValue(), nv.doubleValue());
            amLabel.setText(String.format(" Pillar×: %.1f ", nv.doubleValue()));
        });

        Button resetCam = new Button("Reset Cam");
        resetCam.setOnAction(e -> rig.setTranslate(0, 0, -2000));

        ToolBar toolbar = new ToolBar(
                showCockpit, markers, showAxes, wireCube, baseRing, fullWrap,
                new Separator(),
                rLabel, new Label("Radius:"), rSlider,
                tLabel, new Label("Thick:"), tSlider,
                new Separator(),
                mLabel, mSlider,
                luLabel, luSlider,
                ldLabel, ldSlider,
                new Separator(),
                aLabel, aSlider, new Label(" Color:"), colorPicker,
                new Separator(),
                fbLabel, fbSlider, sigmaLabel, sigmaSlider,
                new Separator(),
                aPillars, azLabel, azSlider, topLabel, topSlider, botLabel, botSlider, amLabel, amSlider,
                new Separator(),
                resetCam,
                new Label("   (Hold RMB or press 'M' to look; WASD/R/F, Shift=sprint)")
        );

        // Layout
        StackPane stack = new StackPane(subScene);
        BorderPane root = new BorderPane(stack);
        root.setTop(toolbar);

        Scene scene = new Scene(root, 1280, 860, true);
        stage.setScene(scene);
        stage.setTitle("TestCockpitApp — 360° Dome Cockpit + Free-Look (WASD/R/F, RMB)");
        stage.show();

        // Route input to the free-look rig (SubScene is a Node)
        rig.start(subScene);

        // Click to ensure focus for key events in the SubScene
        subScene.setOnMouseClicked(e -> root3D.requestFocus());

        // Quick UI shortcuts
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE) markers.setSelected(!markers.isSelected());
            if (e.getCode() == KeyCode.B)     baseRing.setSelected(!baseRing.isSelected());
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
