package AsteroidField.spacecraft;

import AsteroidField.spacecraft.cockpit.CockpitDome3D;
import AsteroidField.spacecraft.cockpit.BaseCockpit;
import AsteroidField.spacecraft.cockpit.TetrahedronCockpit;
import AsteroidField.spacecraft.cockpit.OctahedronCockpit;
import AsteroidField.spacecraft.cockpit.IcosahedronCockpit;
import AsteroidField.spacecraft.cockpit.IcosaDomeCockpit;

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

    // ---- selection options ----
    private enum CockpitOption {
        ORIGINAL_DOME("Original Dome"),
        TETRA("Tetrahedron (Bars+Glass)"),
        OCTA("Octahedron (Bars+Glass)"),
        ICOSAHEDRON("Icosahedron (Bars+Glass)"),
        ICOSA_DOME("Icosa Dome (Hemisphere)");

        final String label;
        CockpitOption(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

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

        // --- Cockpit holder centered at origin (we’ll swap its only child)
        Group cockpitHolder = new Group();
        root3D.getChildren().add(cockpitHolder);

        // SubScene (route input to this, not to Scene)
        SubScene subScene = new SubScene(root3D, 1280, 800, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.ALICEBLUE);
        subScene.setCamera(cam);

        // =======================
        // UI CONTROLS (ToolBar)
        // =======================

        // Selection
        Label pickerLbl = new Label(" Cockpit: ");
        ComboBox<CockpitOption> cockpitPicker = new ComboBox<>();
        cockpitPicker.getItems().addAll(
                CockpitOption.ORIGINAL_DOME,
                CockpitOption.TETRA,
                CockpitOption.OCTA,
                CockpitOption.ICOSAHEDRON,
                CockpitOption.ICOSA_DOME
        );
        cockpitPicker.getSelectionModel().select(CockpitOption.ORIGINAL_DOME);

        // Visibility & debug
        ToggleButton showCockpit = new ToggleButton("Show Cockpit");
        showCockpit.setSelected(true);

        CheckBox markers = new CheckBox("Markers");
        markers.setSelected(false);

        CheckBox showAxes = new CheckBox("Axes");
        showAxes.selectedProperty().addListener((o, was, is) -> axes.setVisible(is));

        CheckBox wireCube = new CheckBox("Wireframe cube");
        wireCube.selectedProperty().addListener((o, was, is) -> {
            cube.setDrawMode(is ? DrawMode.LINE : DrawMode.FILL);
            cube.setOpacity(is ? 1.0 : 0.85);
        });

        CheckBox baseRing = new CheckBox("Base Ring");
        baseRing.setSelected(true);

        CheckBox fullWrap = new CheckBox("360° Wrap");
        fullWrap.setSelected(true);

        // Geometry: radius & bar thickness (rebuild vs recreate)
        Label rLabel = new Label(" R: 200 ");
        Slider rSlider = new Slider(20, 1500, 200);
        rSlider.setPrefWidth(220);

        Label tLabel = new Label(" T: 8.0 ");
        Slider tSlider = new Slider(0.5, 60, 8);
        tSlider.setPrefWidth(180);

        // Structure: meridians & rings (dome-only)
        Label mLabel = new Label(" Meridians: 8 ");
        Slider mSlider = new Slider(6, 64, 8);
        mSlider.setMajorTickUnit(2);
        mSlider.setSnapToTicks(true);
        mSlider.setPrefWidth(160);

        Label luLabel = new Label(" Upper Rings: 2 ");
        Slider luSlider = new Slider(0, 6, 2);
        luSlider.setMajorTickUnit(1);
        luSlider.setSnapToTicks(true);
        luSlider.setPrefWidth(140);

        Label ldLabel = new Label(" Lower Rings: 2 ");
        Slider ldSlider = new Slider(0, 6, 2);
        ldSlider.setMajorTickUnit(1);
        ldSlider.setSnapToTicks(true);
        ldSlider.setPrefWidth(140);

        // Material controls: opacity + color
        Label aLabel = new Label(" Opacity: 1.00 ");
        Slider aSlider = new Slider(0.1, 1.0, 1.0);
        aSlider.setPrefWidth(140);

        ColorPicker colorPicker = new ColorPicker(Color.CYAN);

        // Front bias (windshield feel) – dome-only
        Label fbLabel = new Label(" Front Bias: 2.0× ");
        Slider fbSlider = new Slider(1.0, 4.0, 2.0);
        fbSlider.setPrefWidth(140);
        Label sigmaLabel = new Label(" Width: 28° ");
        Slider sigmaSlider = new Slider(5.0, 90.0, 28.0);
        sigmaSlider.setPrefWidth(140);

        // A-pillars – dome-only
        CheckBox aPillars = new CheckBox("A-Pillars");
        aPillars.setSelected(true);
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

        Button resetCam = new Button("Reset Cam");
        resetCam.setOnAction(e -> rig.setTranslate(0, 0, -2000));

        ToolBar toolbar = new ToolBar(
                pickerLbl, cockpitPicker,
                new Separator(),
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
        stage.setTitle("TestCockpitApp — Cockpit Selector + Free-Look (WASD/R/F, RMB)");
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

        // -------------------------
        // State + Wiring
        // -------------------------

        // central state mirrored by sliders
        final double[] stateRadius = {200};
        final double[] stateThick  = {8.0};
        final Color[]  stateColor  = {Color.CYAN};
        final double[] stateAlpha  = {1.0};

        // build initial cockpit
        Runnable rebuildSelectedCockpit = () -> {
            cockpitHolder.getChildren().clear();
            Group newNode = createCockpitNode(
                    cockpitPicker.getValue(),
                    stateRadius[0], stateThick[0],
                    /*addGlass*/ true
            );
            cockpitHolder.getChildren().add(newNode);
            applyColorAndOpacity(newNode, stateColor[0], stateAlpha[0]);
        };
        rebuildSelectedCockpit.run();

        // visibility
        showCockpit.setOnAction(e -> cockpitHolder.setVisible(showCockpit.isSelected()));

        // enable/disable dome-only controls based on selection
        Runnable refreshControlEnablement = () -> {
            boolean isDome = cockpitPicker.getValue() == CockpitOption.ORIGINAL_DOME;
            markers.setDisable(!isDome);
            baseRing.setDisable(!isDome);
            fullWrap.setDisable(!isDome);
            mSlider.setDisable(!isDome);
            luSlider.setDisable(!isDome);
            ldSlider.setDisable(!isDome);
            fbSlider.setDisable(!isDome);
            sigmaSlider.setDisable(!isDome);
            aPillars.setDisable(!isDome);
            azSlider.setDisable(!isDome);
            topSlider.setDisable(!isDome);
            botSlider.setDisable(!isDome);
            amSlider.setDisable(!isDome);
        };
        refreshControlEnablement.run();

        // selection change
        cockpitPicker.valueProperty().addListener((obs, oldOpt, newOpt) -> {
            rebuildSelectedCockpit.run();
            refreshControlEnablement.run();
        });

        // radius / thickness
        rSlider.valueProperty().addListener((o, ov, nv) -> {
            stateRadius[0] = nv.doubleValue();
            rLabel.setText(String.format(" R: %.0f ", nv.doubleValue()));
            Group current = (Group) cockpitHolder.getChildren().get(0);
            if (current instanceof CockpitDome3D) {
                ((CockpitDome3D) current).rebuild(stateRadius[0], stateThick[0]);
            } else {
                rebuildSelectedCockpit.run();
            }
        });
        tSlider.valueProperty().addListener((o, ov, nv) -> {
            stateThick[0] = nv.doubleValue();
            tLabel.setText(String.format(" T: %.1f ", nv.doubleValue()));
            Group current = (Group) cockpitHolder.getChildren().get(0);
            if (current instanceof CockpitDome3D) {
                ((CockpitDome3D) current).rebuild(stateRadius[0], stateThick[0]);
            } else {
                rebuildSelectedCockpit.run();
            }
        });

        // color + opacity
        aSlider.valueProperty().addListener((o, ov, nv) -> {
            stateAlpha[0] = nv.doubleValue();
            aLabel.setText(String.format(" Opacity: %.2f ", nv.doubleValue()));
            applyColorAndOpacity((Group) cockpitHolder.getChildren().get(0), stateColor[0], stateAlpha[0]);
        });
        colorPicker.setOnAction(e -> {
            stateColor[0] = colorPicker.getValue();
            applyColorAndOpacity((Group) cockpitHolder.getChildren().get(0), stateColor[0], stateAlpha[0]);
        });

        // dome-only listeners (guarded by instanceof)
        markers.selectedProperty().addListener((o, was, is) -> {
            Group g = (Group) cockpitHolder.getChildren().get(0);
            if (g instanceof CockpitDome3D) ((CockpitDome3D) g).setShowMarkers(is);
        });
        baseRing.selectedProperty().addListener((o, was, is) -> {
            Group g = (Group) cockpitHolder.getChildren().get(0);
            if (g instanceof CockpitDome3D) ((CockpitDome3D) g).setIncludeBaseRing(is);
        });
        fullWrap.selectedProperty().addListener((o, was, is) -> {
            Group g = (Group) cockpitHolder.getChildren().get(0);
            if (g instanceof CockpitDome3D) ((CockpitDome3D) g).setFull360(is);
        });

        mSlider.valueProperty().addListener((o, ov, nv) -> {
            int m = nv.intValue();
            mLabel.setText(" Meridians: " + m + " ");
            Group g = (Group) cockpitHolder.getChildren().get(0);
            if (g instanceof CockpitDome3D) ((CockpitDome3D) g).setMeridians(m);
        });
        luSlider.valueProperty().addListener((o, ov, nv) -> {
            int r = nv.intValue();
            luLabel.setText(" Upper Rings: " + r + " ");
            Group g = (Group) cockpitHolder.getChildren().get(0);
            if (g instanceof CockpitDome3D) ((CockpitDome3D) g).setLatitudeRings(r);
        });
        ldSlider.valueProperty().addListener((o, ov, nv) -> {
            int r = nv.intValue();
            ldLabel.setText(" Lower Rings: " + r + " ");
            Group g = (Group) cockpitHolder.getChildren().get(0);
            if (g instanceof CockpitDome3D) ((CockpitDome3D) g).setLatitudeRingsLower(r);
        });

        fbSlider.valueProperty().addListener((o, ov, nv) -> {
            fbLabel.setText(String.format(" Front Bias: %.1f× ", nv.doubleValue()));
            Group g = (Group) cockpitHolder.getChildren().get(0);
            if (g instanceof CockpitDome3D) ((CockpitDome3D) g).setFrontBias(nv.doubleValue(), sigmaSlider.getValue());
        });
        sigmaSlider.valueProperty().addListener((o, ov, nv) -> {
            sigmaLabel.setText(String.format(" Width: %.0f° ", nv.doubleValue()));
            Group g = (Group) cockpitHolder.getChildren().get(0);
            if (g instanceof CockpitDome3D) ((CockpitDome3D) g).setFrontBias(fbSlider.getValue(), nv.doubleValue());
        });

        aPillars.selectedProperty().addListener((o, was, is) -> {
            Group g = (Group) cockpitHolder.getChildren().get(0);
            if (g instanceof CockpitDome3D) ((CockpitDome3D) g).setIncludeAPillars(is);
        });
        azSlider.valueProperty().addListener((o, ov, nv) -> {
            azLabel.setText(String.format(" Az: %.0f° ", nv.doubleValue()));
            Group g = (Group) cockpitHolder.getChildren().get(0);
            if (g instanceof CockpitDome3D) ((CockpitDome3D) g)
                    .setAPillarParams(nv.doubleValue(), topSlider.getValue(), botSlider.getValue(), amSlider.getValue());
        });
        topSlider.valueProperty().addListener((o, ov, nv) -> {
            topLabel.setText(String.format(" Topθ: %.0f° ", nv.doubleValue()));
            Group g = (Group) cockpitHolder.getChildren().get(0);
            if (g instanceof CockpitDome3D) ((CockpitDome3D) g)
                    .setAPillarParams(azSlider.getValue(), nv.doubleValue(), botSlider.getValue(), amSlider.getValue());
        });
        botSlider.valueProperty().addListener((o, ov, nv) -> {
            botLabel.setText(String.format(" Botθ: %.0f° ", nv.doubleValue()));
            Group g = (Group) cockpitHolder.getChildren().get(0);
            if (g instanceof CockpitDome3D) ((CockpitDome3D) g)
                    .setAPillarParams(azSlider.getValue(), topSlider.getValue(), nv.doubleValue(), amSlider.getValue());
        });
        amSlider.valueProperty().addListener((o, ov, nv) -> {
            amLabel.setText(String.format(" Pillar×: %.1f ", nv.doubleValue()));
            Group g = (Group) cockpitHolder.getChildren().get(0);
            if (g instanceof CockpitDome3D) ((CockpitDome3D) g)
                    .setAPillarParams(azSlider.getValue(), topSlider.getValue(), botSlider.getValue(), nv.doubleValue());
        });
    }

    // Factory for cockpit nodes
    private Group createCockpitNode(CockpitOption opt, double radius, double thickness, boolean addGlass) {
        switch (opt) {
            case TETRA:         return new TetrahedronCockpit(radius, thickness, addGlass);
            case OCTA:          return new OctahedronCockpit(radius, thickness, addGlass);
            case ICOSAHEDRON:   return new IcosahedronCockpit(radius, thickness, addGlass);
            case ICOSA_DOME:    return new IcosaDomeCockpit(radius, thickness, addGlass);
            case ORIGINAL_DOME:
            default: {
                CockpitDome3D dome = new CockpitDome3D();
                // Good cinematic defaults
                dome.setMeridians(8);
                dome.setLatitudeRings(2);
                dome.setLatitudeRingsLower(2);
                dome.setFull360(true);
                dome.setIncludeBaseRing(true);
                dome.setFrontBias(2.0, 28.0);
                dome.setIncludeAPillars(true);
                dome.setAPillarParams(24.0, 18.0, 62.0, 2.2);
                // radius/thickness mapping
                dome.rebuild(radius, thickness);
                return dome;
            }
        }
    }

    // Apply color & opacity to both the dome and BaseCockpit variants
    private void applyColorAndOpacity(Group node, Color color, double opacity) {
        if (node instanceof CockpitDome3D) {
            CockpitDome3D dome = (CockpitDome3D) node;
            dome.setBarColor(color);
            dome.setBarOpacity(opacity);
            return;
        }
        if (node instanceof BaseCockpit) {
            BaseCockpit base = (BaseCockpit) node;
            base.setFrameMaterial(new PhongMaterial(Color.color(
                    color.getRed(), color.getGreen(), color.getBlue(), 1.0
            )));
            base.getFrameGroup().setOpacity(opacity);
            // Optionally tint glass lightly:
            // base.setGlassMaterial(new PhongMaterial(Color.color(color.getRed(), color.getGreen(), color.getBlue(), 0.15)));
        }
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
