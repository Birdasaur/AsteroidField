package AsteroidField.ui.overlay;

import AsteroidField.asteroids.field.AsteroidFieldGenerator;
import AsteroidField.asteroids.field.families.FamilyPool;
import AsteroidField.asteroids.field.families.WeightedFamilyEntry;
import AsteroidField.asteroids.field.placement.BeltPlacementStrategy;
import AsteroidField.asteroids.field.placement.PlacementStrategy;
import java.util.List;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

/**
 * Floating debug pane for field generation & placement tuning.
 * - Binds directly to BeltPlacementStrategy properties.
 * - Binds enable/weight for FamilyPool entries.
 * - Exposes generator knobs you can apply on next spawn via fillConfig(...).
 */
public class FieldDebugPane extends LitPathPane {

    // Generator knobs (applied on next spawn via fillConfig)
    private final IntegerProperty count = new SimpleIntegerProperty(150);
    private final IntegerProperty prototypeCount = new SimpleIntegerProperty(48);
    private final SimpleBooleanProperty usePrototypes = new SimpleBooleanProperty(true);

    private final PlacementStrategy placement;
    private final FamilyPool families;

    public FieldDebugPane(Scene scene, Pane desktop, PlacementStrategy placement, FamilyPool families) {
        super(scene, desktop, 480, 640, buildContent(placement, families),
              "Field Debug", "", 250.0, 350.0);
        this.placement = placement;
        this.families = families;
        setOpaqueEnabled(true); // easier to read
    }

    /** Apply current UI generator knobs to a config before spawning. */
    public void fillConfig(AsteroidFieldGenerator.Config cfg) {
        cfg.count = count.get();
        cfg.usePrototypes = usePrototypes.get();
        cfg.prototypeCount = prototypeCount.get();
    }

    private static VBox buildContent(PlacementStrategy placement, FamilyPool families) {
        VBox root = new VBox(12);
        root.setPadding(new Insets(10));
        root.setFillWidth(true);

        // --- Generator section ---
        VBox genBox = new VBox(6);
        genBox.getChildren().add(new Label("Generator"));
        Spinner<Integer> countSpin = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(10, 1000, 150, 10));
        Spinner<Integer> protoSpin = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 400, 48, 1));
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
        root.getChildren().addAll(genBox, new Separator(), placeBox, new Separator(), famBox);

        // Wire generator controls to properties on the pane instance (via lookup later)
        root.setUserData(new Object[]{countSpin, protoSpin, useProto});
        return root;
    }

    // helpers to build UI rows
    private static HBox row(String label, javafx.scene.Node control) {
        Label l = new Label(label);
        l.setMinWidth(140);
        HBox h = new HBox(8, l, control);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private static GridPane beltSliders(BeltPlacementStrategy b) {
        GridPane gp = new GridPane(); gp.setHgap(8); gp.setVgap(6);

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

        int r = 0;
        gp.addRow(r++, new Label("Inner radius"), r0);
        gp.addRow(r++, new Label("Outer radius"), r1);
        gp.addRow(r++, new Label("Thickness σ"), th);
        gp.addRow(r++, new Label("Min separation"), sep);
        gp.addRow(r++, new Label("Base scale"), sc);

        gp.addRow(r++, new Label("Ellipse X scale"), sx);
        gp.addRow(r++, new Label("Ellipse Y scale"), sy);
        gp.addRow(r++, new Label("Yaw (°)"), yaw);
        gp.addRow(r++, new Label("Pitch (°)"), pitch);
        gp.addRow(r++, new Label("Roll (°)"), roll);

        gp.addRow(r++, new Label("Center X"), cx);
        gp.addRow(r++, new Label("Center Y"), cy);
        gp.addRow(r++, new Label("Center Z"), cz);

        return gp;
    }

    private static VBox familyControls(List<WeightedFamilyEntry> entries) {
        VBox box = new VBox(4);
        for (WeightedFamilyEntry e : entries) {
            CheckBox cb = new CheckBox(e.displayName());
            cb.selectedProperty().bindBidirectional(e.enabledProperty());

            Slider w = new Slider(0.0, 5.0, e.getWeight());
            e.weightProperty().bind(w.valueProperty());

            HBox row = new HBox(8, cb, w);
            row.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().add(row);
        }
        return box;
    }

    // Expose quick accessors so caller can pull current generator knobs
    @SuppressWarnings("unchecked")
    public int getDesiredCount() {
        Object[] ud = (Object[]) ((VBox) contentPane).getUserData();
        Spinner<Integer> countSpin = (Spinner<Integer>) ud[0];
        return countSpin.getValue();
    }

    @SuppressWarnings("unchecked")
    public int getDesiredPrototypeCount() {
        Object[] ud = (Object[]) ((VBox) contentPane).getUserData();
        Spinner<Integer> protoSpin = (Spinner<Integer>) ud[1];
        return protoSpin.getValue();
    }

    @SuppressWarnings("unchecked")
    public boolean isUsePrototypes() {
        Object[] ud = (Object[]) ((VBox) contentPane).getUserData();
        CheckBox useProto = (CheckBox) ud[2];
        return useProto.isSelected();
    }
}
