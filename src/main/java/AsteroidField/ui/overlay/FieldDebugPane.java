package AsteroidField.ui.overlay;

import AsteroidField.asteroids.field.AsteroidFieldGenerator;
import AsteroidField.asteroids.field.families.FamilyPool;
import AsteroidField.asteroids.field.families.WeightedFamilyEntry;
import AsteroidField.asteroids.field.placement.BeltPlacementStrategy;
import AsteroidField.asteroids.field.placement.PlacementStrategy;
import AsteroidField.events.AsteroidFieldEvent;

import java.util.List;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;

/**
 * Floating debug pane for field generation, placement tuning, and render overrides.
 * - Binds directly to BeltPlacementStrategy properties.
 * - Controls enable/weight for FamilyPool entries.
 * - "Regenerate Field" / "Clear Field" buttons fire AsteroidFieldEvent requests.
 * - Render toolbar publishes RENDER_MODE_REQUEST and CULLFACE_REQUEST.
 * - Content is wrapped in a ScrollPane; sliders grow to full width.
 */
public class FieldDebugPane extends LitPathPane {

    // Generator knobs (applied on next spawn via fillConfig)
    private final IntegerProperty count = new SimpleIntegerProperty(150);
    private final IntegerProperty prototypeCount = new SimpleIntegerProperty(48);
    private final SimpleBooleanProperty usePrototypes = new SimpleBooleanProperty(true);

    private final PlacementStrategy placement;
    private final FamilyPool families;

    public FieldDebugPane(Scene scene, Pane desktop, PlacementStrategy placement, FamilyPool families) {
        super(
            scene,
            desktop,
            540, 700,                          // comfortable viewport for scrolled content
            buildContainer(scene, placement, families),
            "Field Debug",
            "",
            250.0, 350.0
        );
        this.placement = placement;
        this.families = families;
        setOpaqueEnabled(true);
    }

    /** Apply current UI generator knobs to a config before spawning. */
    public void fillConfig(AsteroidFieldGenerator.Config cfg) {
        cfg.count = getDesiredCount();
        cfg.prototypeCount = getDesiredPrototypeCount();
        cfg.usePrototypes = isUsePrototypes();
    }

    // ---------- Builders ----------

    /** Root container must be a Pane → we use a VBox that hosts a ScrollPane. */
    private static VBox buildContainer(Scene ownerScene, PlacementStrategy placement, FamilyPool families) {
        VBox container = new VBox();
        container.setFillWidth(true);

        VBox content = buildContent(ownerScene, placement, families);

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setPannable(true);
        sp.setPrefViewportHeight(660);

        container.getChildren().add(sp);
        VBox.setVgrow(sp, Priority.ALWAYS);
        return container;
    }

    private static VBox buildContent(Scene ownerScene, PlacementStrategy placement, FamilyPool families) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(10));
        root.setFillWidth(true);

        // === Toolbar (top): Regenerate / Clear / Render Controls ===
        HBox toolbar = new HBox(10);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        HBox drawToolbar = new HBox(10);
        drawToolbar.setAlignment(Pos.CENTER_LEFT);

        Button btnRegenerate = new Button("Regenerate Field");
        Button btnClear = new Button("Clear Field");

        // Render controls
        Label lblDraw = new Label("Draw:");
        ComboBox<DrawMode> cbDraw = new ComboBox<>(FXCollections.observableArrayList(DrawMode.FILL, DrawMode.LINE));
        cbDraw.setValue(DrawMode.FILL);

        Label lblCull = new Label("Cull:");
        ComboBox<CullFace> cbCull = new ComboBox<>(FXCollections.observableArrayList(CullFace.BACK, CullFace.FRONT, CullFace.NONE));
        cbCull.setValue(CullFace.BACK);

        // Actions: Regenerate
        btnRegenerate.setOnAction(ev -> {
            AsteroidFieldGenerator.Config cfg = new AsteroidFieldGenerator.Config();

            Object[] ud = (Object[]) root.getUserData();
            @SuppressWarnings("unchecked") Spinner<Integer> countSpin = (Spinner<Integer>) ud[0];
            @SuppressWarnings("unchecked") Spinner<Integer> protoSpin = (Spinner<Integer>) ud[1];
            CheckBox useProto = (CheckBox) ud[2];

            cfg.count = countSpin.getValue();
            cfg.prototypeCount = protoSpin.getValue();
            cfg.usePrototypes = useProto.isSelected();

            Scene sceneRef = ownerScene != null ? ownerScene : btnRegenerate.getScene();
            if (sceneRef != null && sceneRef.getRoot() != null) {
                sceneRef.getRoot().fireEvent(AsteroidFieldEvent.regenerateRequest(btnRegenerate, sceneRef.getRoot(), cfg));
            } else {
                btnRegenerate.fireEvent(AsteroidFieldEvent.regenerateRequest(btnRegenerate, btnRegenerate, cfg));
            }
        });

        // Actions: Clear
        btnClear.setOnAction(ev -> {
            Scene sceneRef = ownerScene != null ? ownerScene : btnClear.getScene();
            if (sceneRef != null && sceneRef.getRoot() != null) {
                sceneRef.getRoot().fireEvent(AsteroidFieldEvent.clearRequest(btnClear, sceneRef.getRoot()));
            } else {
                btnClear.fireEvent(AsteroidFieldEvent.clearRequest(btnClear, btnClear));
            }
        });

        // Actions: DrawMode change
        cbDraw.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;
            Scene sceneRef = ownerScene != null ? ownerScene : cbDraw.getScene();
            if (sceneRef != null && sceneRef.getRoot() != null) {
                sceneRef.getRoot().fireEvent(AsteroidFieldEvent.renderModeRequest(cbDraw, sceneRef.getRoot(), newV));
            } else {
                cbDraw.fireEvent(AsteroidFieldEvent.renderModeRequest(cbDraw, cbDraw, newV));
            }
        });

        // Actions: CullFace change
        cbCull.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV == null) return;
            Scene sceneRef = ownerScene != null ? ownerScene : cbCull.getScene();
            if (sceneRef != null && sceneRef.getRoot() != null) {
                sceneRef.getRoot().fireEvent(AsteroidFieldEvent.cullFaceRequest(cbCull, sceneRef.getRoot(), newV));
            } else {
                cbCull.fireEvent(AsteroidFieldEvent.cullFaceRequest(cbCull, cbCull, newV));
            }
        });

        // Assemble toolbar
        toolbar.getChildren().addAll(
            btnRegenerate,
            btnClear
        );

        // Assemble toolbar
        drawToolbar.getChildren().addAll(
            lblDraw, cbDraw,
            lblCull, cbCull
        );
        
        // --- Generator section ---
        VBox genBox = new VBox(6);
        genBox.getChildren().add(new Label("Generator"));

        Spinner<Integer> countSpin = new Spinner<>(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 3000, 150, 10));
        countSpin.setEditable(true);
        countSpin.setMaxWidth(Double.MAX_VALUE);

        Spinner<Integer> protoSpin = new Spinner<>(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 400, 48, 1));
        protoSpin.setEditable(true);
        protoSpin.setMaxWidth(Double.MAX_VALUE);

        CheckBox useProto = new CheckBox("Use Prototypes (faster at high count)");
        useProto.setSelected(true);

        HBox h1 = row("Count", countSpin);
        HBox h2 = row("Prototype Count", protoSpin);
        genBox.getChildren().addAll(h1, h2, useProto);

        // --- Placement section (bind if BeltPlacementStrategy) ---
        VBox placeBox = new VBox(6);
        placeBox.getChildren().add(new Label("Placement (Belt)"));
        if (placement instanceof BeltPlacementStrategy belt) {
            placeBox.getChildren().add(beltSliders(belt));
        } else {
            placeBox.getChildren().add(new Label("Unsupported placement type: " + placement.getName()));
        }

        // --- Families section ---
        VBox famBox = new VBox(6);
        famBox.getChildren().add(new Label("Families"));
        famBox.getChildren().add(familyControls(families.entries()));

        // Glue
        root.getChildren().addAll(
            toolbar,
            new Separator(),
            drawToolbar,    
            new Separator(),
            genBox,
            new Separator(),
            placeBox,
            new Separator(),
            famBox
        );

        // Store control refs for quick access by actions
        root.setUserData(new Object[]{countSpin, protoSpin, useProto});
        return root;
    }

    // ---------- helpers ----------

    private static HBox row(String label, javafx.scene.Node control) {
        Label l = new Label(label);
        l.setMinWidth(140);
        l.setPrefWidth(140);
        HBox h = new HBox(8, l, control);
        h.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(control, Priority.ALWAYS);
        control.maxWidth(Double.MAX_VALUE);
        return h;
    }

    private static GridPane beltSliders(BeltPlacementStrategy b) {
        GridPane gp = new GridPane();
        gp.setHgap(8);
        gp.setVgap(6);
        gp.setPadding(new Insets(0));

        ColumnConstraints c0 = new ColumnConstraints();
        c0.setMinWidth(140);
        c0.setPrefWidth(140);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        c1.setPercentWidth(100);

        gp.getColumnConstraints().addAll(c0, c1);

        java.util.function.BiConsumer<String, Slider> add = (text, slider) -> {
            slider.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHgrow(slider, Priority.ALWAYS);
            int r = gp.getRowCount();
            gp.addRow(r, new Label(text), slider);
        };

        Slider r0 = new Slider(100, 100000, b.getInnerRadius());
        Slider r1 = new Slider(100, 100000, b.getOuterRadius());
        Slider th = new Slider(0, 5000, b.getThicknessSigma());
        Slider sep= new Slider(0, 1500, b.getMinSeparation());
        Slider sc = new Slider(0.1, 5.0, b.getBaseScale());

        Slider sx = new Slider(0.2, 5.0, b.getScaleX());
        Slider sy = new Slider(0.2, 5.0, b.getScaleY());
        Slider yaw = new Slider(-180, 180, b.getYawDeg());
        Slider pitch = new Slider(-89, 89, b.getPitchDeg());
        Slider roll = new Slider(-180, 180, b.getRollDeg());

        Slider cx = new Slider(-20000, 20000, b.getCenterX());
        Slider cy = new Slider(-20000, 20000, b.getCenterY());
        Slider cz = new Slider(-20000, 20000, b.getCenterZ());

        // bind bi-directionally
        b.innerRadiusProperty().bind(r0.valueProperty());
        b.outerRadiusProperty().bind(r1.valueProperty());
        b.thicknessSigmaProperty().bind(th.valueProperty());
        b.minSeparationProperty().bind(sep.valueProperty());
        b.baseScaleProperty().bind(sc.valueProperty());

        b.scaleXProperty().bind(sx.valueProperty());
        b.scaleYProperty().bind(sy.valueProperty());
        b.yawDegProperty().bind(yaw.valueProperty());
        b.pitchDegProperty().bind(pitch.valueProperty());
        b.rollDegProperty().bind(roll.valueProperty());

        b.centerXProperty().bind(cx.valueProperty());
        b.centerYProperty().bind(cy.valueProperty());
        b.centerZProperty().bind(cz.valueProperty());

        add.accept("Inner radius", r0);
        add.accept("Outer radius", r1);
        add.accept("Thickness σ", th);
        add.accept("Min separation", sep);
        add.accept("Base scale", sc);

        add.accept("Ellipse X scale", sx);
        add.accept("Ellipse Y scale", sy);
        add.accept("Yaw (°)", yaw);
        add.accept("Pitch (°)", pitch);
        add.accept("Roll (°)", roll);

        add.accept("Center X", cx);
        add.accept("Center Y", cy);
        add.accept("Center Z", cz);

        return gp;
    }

    private static VBox familyControls(List<WeightedFamilyEntry> entries) {
        VBox box = new VBox(4);
        for (WeightedFamilyEntry e : entries) {
            CheckBox cb = new CheckBox(e.displayName());
            cb.selectedProperty().bindBidirectional(e.enabledProperty());

            Slider w = new Slider(0.0, 5.0, e.getWeight());
            e.weightProperty().bind(w.valueProperty());

            w.setMaxWidth(Double.MAX_VALUE);
            HBox row = new HBox(8, cb, w);
            row.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(w, Priority.ALWAYS);

            box.getChildren().add(row);
        }
        return box;
    }

    // ---------- Accessors (content is inside ScrollPane inside this pane's VBox) ----------

    private ScrollPane getScrollPane() {
        return (ScrollPane) ((VBox) contentPane).getChildren().get(0);
    }

    @SuppressWarnings("unchecked")
    public int getDesiredCount() {
        Object[] ud = (Object[]) ((VBox) getScrollPane().getContent()).getUserData();
        Spinner<Integer> countSpin = (Spinner<Integer>) ud[0];
        return countSpin.getValue();
    }

    @SuppressWarnings("unchecked")
    public int getDesiredPrototypeCount() {
        Object[] ud = (Object[]) ((VBox) getScrollPane().getContent()).getUserData();
        Spinner<Integer> protoSpin = (Spinner<Integer>) ud[1];
        return protoSpin.getValue();
    }

    @SuppressWarnings("unchecked")
    public boolean isUsePrototypes() {
        Object[] ud = (Object[]) ((VBox) getScrollPane().getContent()).getUserData();
        CheckBox useProto = (CheckBox) ud[2];
        return useProto.isSelected();
    }
}
