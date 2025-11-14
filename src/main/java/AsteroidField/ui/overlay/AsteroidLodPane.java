package AsteroidField.ui.overlay;

import AsteroidField.asteroids.AsteroidLodManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Floating LOD debug/tuning pane for asteroids.
 * - Extends LitPathPane so it can live in your overlay "desktop".
 * - Exposes the usual knobs: enable, near/mid distances, hysteresis, budget, forward cone, tint-by-tier.
 * - Displays a live stats string supplied by the caller (keeps this class decoupled from manager internals).
 *
 * Usage:
 *   AsteroidLodPane p = new AsteroidLodPane(scene, overlayDesktop, lodManager, () -> lodManager.debugSummary());
 *   overlayDesktop.getChildren().add(p);
 *   p.slideInPane();
 */
public class AsteroidLodPane extends LitPathPane {

    // UI
    private final CheckBox enabledChk = new CheckBox("Enable LOD");
    private final Slider nearS = new Slider(200, 12000, 2000);
    private final Slider midS  = new Slider(500, 20000, 6000);
    private final Slider hystS = new Slider(0.0, 0.6, 0.20);
    private final Slider budgetS = new Slider(10, 1000, 240);
    private final Slider coneS = new Slider(0, 180, 160);
    private final CheckBox tintChk = new CheckBox("Tint by tier (debug)");
    private final CheckBox onScreenOnlyChk = new CheckBox("On-screen only");

    private final Label nearLbl = new Label();
    private final Label midLbl = new Label();
    private final Label hystLbl = new Label();
    private final Label budgetLbl = new Label();
    private final Label coneLbl = new Label();

    private final TextArea statsArea = new TextArea();

    private final AsteroidLodManager lod;
    private final Supplier<String> statsSupplier;

    // Pane sizing
    private static final int PANE_WIDTH = 420;
    private static final int PANE_HEIGHT = 420;

    public AsteroidLodPane(Scene scene, Pane overlayDesktop,
                           AsteroidLodManager lod,
                           Supplier<String> statsSupplier) {
        super(scene, overlayDesktop,
              PANE_WIDTH, PANE_HEIGHT,
              buildContent(),
              "Asteroid LOD", "Debug / Tuning",
              250.0, 350.0);

        this.lod = Objects.requireNonNull(lod, "lod");
        this.statsSupplier = (statsSupplier != null) ? statsSupplier : () -> "N/A";

        // Initial UI values from manager (adjust names to your API if different)
        trySet(() -> enabledChk.setSelected(lod.isEnabled()));
        trySet(() -> nearS.setValue(lod.getNearDistance()));
        trySet(() -> midS.setValue(lod.getMidDistance()));
        trySet(() -> hystS.setValue(lod.getHysteresis()));
        trySet(() -> budgetS.setValue(lod.getBudgetPerFrame()));
        trySet(() -> coneS.setValue(lod.getForwardConeDeg()));
        trySet(() -> tintChk.setSelected(lod.isTintByTierEnabled()));
        trySet(() -> onScreenOnlyChk.setSelected(lod.isOnScreenOnly()));

        // Labels
        refreshValueLabels();

        // Wire listeners → manager setters (adjust names if your API differs)
        enabledChk.selectedProperty().addListener((o, ov, nv) -> trySet(() -> lod.setEnabled(nv)));

        nearS.valueProperty().addListener((o, ov, nv) -> {
            // Keep mid >= near + 100
            if (midS.getValue() < nv.doubleValue() + 100) {
                midS.setValue(nv.doubleValue() + 100);
            }
            refreshValueLabels();
            trySet(() -> lod.setNearDistance(nearS.getValue()));
        });

        midS.valueProperty().addListener((o, ov, nv) -> {
            // Ensure mid >= near + 100
            if (nv.doubleValue() < nearS.getValue() + 100) {
                midS.setValue(nearS.getValue() + 100);
                return;
            }
            refreshValueLabels();
            trySet(() -> lod.setMidDistance(midS.getValue()));
        });

        hystS.valueProperty().addListener((o, ov, nv) -> {
            refreshValueLabels();
            trySet(() -> lod.setHysteresis(hystS.getValue()));
        });

        budgetS.valueProperty().addListener((o, ov, nv) -> {
            refreshValueLabels();
            trySet(() -> lod.setBudgetPerFrame((int) budgetS.getValue()));
        });

        coneS.valueProperty().addListener((o, ov, nv) -> {
            refreshValueLabels();
            trySet(() -> lod.setForwardConeDegrees(coneS.getValue()));
        });

        tintChk.selectedProperty().addListener((o, ov, nv) -> trySet(() -> lod.setTintByTierEnabled(nv)));
        onScreenOnlyChk.selectedProperty().addListener((o, ov, nv) -> trySet(() -> lod.setOnScreenOnly(nv)));

        // Stats area setup
        statsArea.setEditable(false);
        statsArea.setWrapText(true);
        statsArea.setPrefRowCount(6);
        statsArea.setStyle("-fx-control-inner-background: rgba(0,0,0,0.65); -fx-text-fill: cyan;");

        // Periodic refresh of stats
        var statsRefresh = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.millis(250),
                e -> statsArea.setText(safeGet(statsSupplier)))
        );
        statsRefresh.setCycleCount(javafx.animation.Animation.INDEFINITE);
        statsRefresh.play();

        // Put content into LitPathPane
        ((BorderPane) this.contentPane).setCenter(buildForm());
        setOpaqueEnabled(true); // darker background by default
    }

    private static BorderPane buildContent() {
        BorderPane root = new BorderPane();
        root.setBackground(new Background(new BackgroundFill(
            Color.color(0, 0.1, 0.12, 0.35),
            CornerRadii.EMPTY, Insets.EMPTY)));
        return root;
    }

    private Pane buildForm() {
        // Rows
        var g = new GridPane();
        g.setHgap(8);
        g.setVgap(8);
        g.setPadding(new Insets(10));

        // Sliders styling
        nearS.setShowTickMarks(true); nearS.setShowTickLabels(true); nearS.setBlockIncrement(100);
        midS.setShowTickMarks(true);  midS.setShowTickLabels(true);  midS.setBlockIncrement(200);
        hystS.setShowTickMarks(true); hystS.setShowTickLabels(true); hystS.setBlockIncrement(0.05);
        budgetS.setShowTickMarks(true); budgetS.setShowTickLabels(true); budgetS.setBlockIncrement(10);
        coneS.setShowTickMarks(true); coneS.setShowTickLabels(true); coneS.setBlockIncrement(10);

        // Row 0: enable + debug
        HBox toggles = new HBox(12, enabledChk, tintChk, onScreenOnlyChk);
        toggles.setAlignment(Pos.CENTER_LEFT);
        g.add(new Label("Mode:"), 0, 0);
        g.add(toggles, 1, 0);

        // Row 1: Near
        g.add(new Label("Near distance:"), 0, 1);
        g.add(nearS, 1, 1);
        g.add(nearLbl, 2, 1);

        // Row 2: Mid
        g.add(new Label("Mid distance:"), 0, 2);
        g.add(midS, 1, 2);
        g.add(midLbl, 2, 2);

        // Row 3: Hysteresis
        g.add(new Label("Hysteresis:"), 0, 3);
        g.add(hystS, 1, 3);
        g.add(hystLbl, 2, 3);

        // Row 4: Budget/frame
        g.add(new Label("Checks/frame:"), 0, 4);
        g.add(budgetS, 1, 4);
        g.add(budgetLbl, 2, 4);

        // Row 5: Forward cone (deg)
        g.add(new Label("Forward cone (°):"), 0, 5);
        g.add(coneS, 1, 5);
        g.add(coneLbl, 2, 5);

        // Row 6: Buttons
        Button resetBtn = new Button("Reset Defaults");
        resetBtn.setOnAction(e -> resetToDefaults());
        HBox btns = new HBox(10, resetBtn);
        btns.setAlignment(Pos.CENTER_LEFT);
        g.add(btns, 1, 6);

        // Stats (bottom)
        VBox box = new VBox(10, g, new Label("Stats:"), statsArea);
        box.setPadding(new Insets(8));
        return box;
    }

    private void resetToDefaults() {
        enabledChk.setSelected(true);
        nearS.setValue(2000);
        midS.setValue(6000);
        hystS.setValue(0.20);
        budgetS.setValue(240);
        coneS.setValue(160);
        tintChk.setSelected(false);
        onScreenOnlyChk.setSelected(true);
    }

    private void refreshValueLabels() {
        nearLbl.setText(String.format("%.0f", nearS.getValue()));
        midLbl.setText(String.format("%.0f", midS.getValue()));
        hystLbl.setText(String.format("%.2f", hystS.getValue()));
        budgetLbl.setText(String.format("%.0f", budgetS.getValue()));
        coneLbl.setText(String.format("%.0f°", coneS.getValue()));
    }

    private static void trySet(Runnable r) {
        try { r.run(); } catch (Throwable ignored) {}
    }

    private static String safeGet(Supplier<String> s) {
        try { return s.get(); } catch (Throwable t) { return "N/A"; }
    }
}
