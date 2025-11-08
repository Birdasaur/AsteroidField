package AsteroidField.ui.components;

import AsteroidField.ui.scene3d.StarAtlasGenerator;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.Random;
import java.util.function.DoubleConsumer;

public class StarfieldGeneratorPane extends BorderPane {

    private final StarAtlasGenerator.Params params = new StarAtlasGenerator.Params();
    private final ImageView preview = new ImageView();
    private final Random rng = new Random();

    private CheckBox autoToggle;

    // ----- 4:3 Presets up to 8K (8192×6144) -----
    private static final class ResPreset {
        final String label;
        final int w, h;
        ResPreset(String label, int w, int h) { this.label = label; this.w = w; this.h = h; }
        @Override public String toString() { return label; }
    }

    private static final List<ResPreset> PRESETS = List.of(
        new ResPreset("512×384 (0.5K)",     512,  384),
        new ResPreset("1024×768 (1K)",     1024,  768),
        new ResPreset("1536×1152 (1.5K)",  1536, 1152),
        new ResPreset("2048×1536 (2K)",    2048, 1536),
        new ResPreset("2560×1920 (2.5K)",  2560, 1920),
        new ResPreset("3072×2304 (3K)",    3072, 2304),
        new ResPreset("4096×3072 (4K)",    4096, 3072),   // default
        new ResPreset("5120×3840 (5K)",    5120, 3840),
        new ResPreset("6144×4608 (6K)",    6144, 4608),
        new ResPreset("7680×5760 (7.5K)",  7680, 5760),
        new ResPreset("8192×6144 (8K)",    8192, 6144)
    );
public StarfieldGeneratorPane() {
    setPadding(new Insets(8));
    setBackground(new Background(new BackgroundFill(Color.web("#111"), CornerRadii.EMPTY, Insets.EMPTY)));

    // --- Controls (scrollable) ---
    GridPane controls = buildControls();
    controls.setPadding(new Insets(12));
    controls.setHgap(8);
    controls.setVgap(8);

    // Let the grid be naturally tall so it can overflow
    controls.setMinHeight(Region.USE_COMPUTED_SIZE);
    controls.setPrefHeight(Region.USE_COMPUTED_SIZE);
    controls.setMaxHeight(Region.USE_COMPUTED_SIZE);

    // Don’t clamp the width; let SplitPane manage it.
    // (Just give it a sensible minimum so the divider can move.)
    controls.setMinWidth(260);
    controls.setPrefWidth(360);
    controls.setMaxWidth(Double.MAX_VALUE);

    ScrollPane scroller = new ScrollPane(controls);
    scroller.setFitToWidth(true);              // fill width so no H-bar
    scroller.setFitToHeight(true);             // track viewport height (OK inside SplitPane)
    scroller.setPannable(true);
    scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
    scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

    // Keep content width == viewport width (prevents H-bar even when resized)
    scroller.viewportBoundsProperty().addListener((obs, ov, vb) -> {
        controls.setPrefWidth(vb.getWidth());
    });

    // Ensure SplitPane treats the sidebar as resizable and gives it room
    scroller.setMinWidth(260);
    scroller.setMaxWidth(Double.MAX_VALUE);
    SplitPane.setResizableWithParent(scroller, Boolean.TRUE);

    // --- Preview (resizable) ---
    preview.setPreserveRatio(true);
    preview.setSmooth(true);

    StackPane previewPane = new StackPane(preview);
    previewPane.setPadding(new Insets(8));
    previewPane.setBackground(new Background(new BackgroundFill(Color.BLACK, CornerRadii.EMPTY, Insets.EMPTY)));

    // Make the preview side fully resizable by the SplitPane
    previewPane.setMinWidth(300);
    previewPane.setPrefWidth(800);
    previewPane.setMaxWidth(Double.MAX_VALUE);
    SplitPane.setResizableWithParent(previewPane, Boolean.TRUE);

    // Bind preview to available space; ImageView chooses limiting dimension
    preview.fitWidthProperty().bind(previewPane.widthProperty().subtract(16));
    preview.fitHeightProperty().bind(previewPane.heightProperty().subtract(16));

    // --- SplitPane with draggable divider ---
    SplitPane split = new SplitPane(scroller, previewPane);
    split.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
    split.setDividerPositions(0.34);           // initial ratio; *draggable*
    // Crucial: neither child has a fixed max width; both are resizable Regions with sane mins.

    // Fill the center with the split
    setCenter(split);

    // Initial selection & render
    setResolutionFromPreset(findPreset(params.width, params.height));
    regenerate();
}

    // --------- Public API ----------

    public StarAtlasGenerator.Params getParams() { return params; }

    public WritableImage getAtlasImage() { return StarAtlasGenerator.generate(params); }

    public StarAtlasGenerator.Faces getFaces() { return StarAtlasGenerator.generateFaces(params); }

    public void regenerate() {
        WritableImage atlas = StarAtlasGenerator.generate(params);
        preview.setImage(atlas);
        // Sizing handled by bindings
    }

    // --------- UI setup ----------

    private GridPane buildControls() {
        GridPane g = new GridPane();
        g.setHgap(8);
        g.setVgap(8);
        g.setPadding(new Insets(12));

        // Make label column narrower, control columns wider (2 + 3 layout)
        ColumnConstraints c0 = new ColumnConstraints();
        c0.setPercentWidth(28);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setPercentWidth(72);
        g.getColumnConstraints().setAll(c0, c1, new ColumnConstraints(), new ColumnConstraints(), new ColumnConstraints());

        int row = 0;

        // ---- Resolution preset (4:3 only) ----
        ComboBox<ResPreset> cbRes = new ComboBox<>();
        cbRes.getItems().addAll(PRESETS);
        cbRes.setMaxWidth(Double.MAX_VALUE);
        cbRes.setEditable(false);

        ResPreset initial = findPreset(params.width, params.height);
        if (initial == null) {
            initial = PRESETS.stream().filter(p -> p.w == 4096 && p.h == 3072)
                    .findFirst().orElse(PRESETS.get(0));
        }
        cbRes.getSelectionModel().select(initial);

        cbRes.setOnAction(e -> {
            ResPreset sel = cbRes.getSelectionModel().getSelectedItem();
            if (sel != null) {
                setResolutionFromPreset(sel);
                regenIfAuto();
            }
        });

        g.add(new Label("Resolution (4:3):"), 0, row);
        g.add(cbRes, 1, row++, 4, 1);

        // ---- Seed ----
        TextField tfSeed = new TextField(String.valueOf(params.seed));
        Button btnRandSeed = new Button("Randomize");
        btnRandSeed.setOnAction(e -> {
            long s = rng.nextLong();
            params.seed = s;
            tfSeed.setText(String.valueOf(s));
            regenIfAuto();
        });
        tfSeed.setOnAction(e -> {
            try { params.seed = Long.parseLong(tfSeed.getText().trim()); regenIfAuto(); }
            catch (NumberFormatException ex) { showError("Invalid seed."); }
        });
        g.add(new Label("Seed:"), 0, row); g.add(tfSeed, 1, row); g.add(btnRandSeed, 2, row++);

        // ---- Star density ----
        g.add(new Label("Star density:"), 0, row);
        Slider sDensity = slider(0.00005, 0.0005, params.starDensity, v -> { params.starDensity = v; regenIfAuto(); });
        g.add(sDensity, 1, row, 3, 1); g.add(valLabel(sDensity), 4, row++);

        // ---- Color bias ----
        g.add(new Label("Color bias:"), 0, row);
        Slider sColor = slider(0.0, 1.0, params.colorBias, v -> { params.colorBias = v; regenIfAuto(); });
        g.add(sColor, 1, row, 3, 1); g.add(valLabel(sColor), 4, row++);

        // ---- Radius mean/sigma/max ----
        g.add(new Label("Radius mean (px):"), 0, row);
        Slider sRMean = slider(0.05, 1.25, params.radiusMean, v -> { params.radiusMean = v; regenIfAuto(); });
        g.add(sRMean, 1, row, 3, 1); g.add(valLabel(sRMean), 4, row++);

        g.add(new Label("Radius sigma (px):"), 0, row);
        Slider sRSig = slider(0.05, 0.6, params.radiusSigma, v -> { params.radiusSigma = v; regenIfAuto(); });
        g.add(sRSig, 1, row, 3, 1); g.add(valLabel(sRSig), 4, row++);

        g.add(new Label("Radius max (px):"), 0, row);
        Slider sRMax = slider(0.5, 2.0, params.radiusMax, v -> { params.radiusMax = v; regenIfAuto(); });
        g.add(sRMax, 1, row, 3, 1); g.add(valLabel(sRMax), 4, row++);

        // ---- Bright points ----
        g.add(new Label("Bright chance:"), 0, row);
        Slider sBChance = slider(0.0, 0.1, params.brightChance, v -> { params.brightChance = v; regenIfAuto(); });
        g.add(sBChance, 1, row, 3, 1); g.add(valLabel(sBChance), 4, row++);

        g.add(new Label("Bright multiplier:"), 0, row);
        Slider sBMul = slider(1.0, 2.0, params.brightMul, v -> { params.brightMul = v; regenIfAuto(); });
        g.add(sBMul, 1, row, 3, 1); g.add(valLabel(sBMul), 4, row++);

        // ---- Nebula ----
        g.add(new Label("Nebula blobs:"), 0, row);
        Slider sNBlobs = slider(0, 120, params.nebulaBlobs, v -> { params.nebulaBlobs = (int)Math.round(v); regenIfAuto(); });
        g.add(sNBlobs, 1, row, 3, 1); g.add(valLabel(sNBlobs), 4, row++);

        g.add(new Label("Nebula alpha:"), 0, row);
        Slider sNAlpha = slider(0.0, 0.10, params.nebulaAlpha, v -> { params.nebulaAlpha = v; regenIfAuto(); });
        g.add(sNAlpha, 1, row, 3, 1); g.add(valLabel(sNAlpha), 4, row++);

        // ---- Internal bleed ----
        g.add(new Label("Internal bleed (px):"), 0, row);
        Slider sBleed = slider(0, 4, params.internalBleedPx, v -> { params.internalBleedPx = (int)Math.round(v); regenIfAuto(); });
        g.add(sBleed, 1, row, 3, 1); g.add(valLabel(sBleed), 4, row++);

        // --- Milky Way ---
        g.add(new Separator(), 0, row++, 5, 1);
        Label mwLabel = new Label("Milky Way");
        mwLabel.setStyle("-fx-font-weight: bold;");
        g.add(mwLabel, 0, row++);

        CheckBox cbMW = new CheckBox("Enable Milky Way");
        cbMW.setSelected(params.mwEnabled);
        cbMW.selectedProperty().addListener((o,ov,nv)-> { params.mwEnabled = nv; regenIfAuto(); });
        g.add(cbMW, 0, row++, 5, 1);

        g.add(new Label("Intensity:"), 0, row);
        Slider sMWInt = slider(0.0, 2.0, params.mwIntensity, v -> { params.mwIntensity = v; regenIfAuto(); });
        g.add(sMWInt, 1, row, 3, 1); g.add(valLabel(sMWInt), 4, row++);

        g.add(new Label("Opacity:"), 0, row);
        Slider sMWOp = slider(0.0, 1.0, params.mwOpacity, v -> { params.mwOpacity = v; regenIfAuto(); });
        g.add(sMWOp, 1, row, 3, 1); g.add(valLabel(sMWOp), 4, row++);

        g.add(new Label("Thickness (px):"), 0, row);
        Slider sMWTh = slider(10, 200, params.mwThickness, v -> { params.mwThickness = v; regenIfAuto(); });
        g.add(sMWTh, 1, row, 3, 1); g.add(valLabel(sMWTh), 4, row++);

        g.add(new Label("Hue (°):"), 0, row);
        Slider sMWH = slider(0, 360, params.mwHue, v -> { params.mwHue = v; regenIfAuto(); });
        g.add(sMWH, 1, row, 3, 1); g.add(valLabel(sMWH), 4, row++);

        g.add(new Label("Saturation:"), 0, row);
        Slider sMWS = slider(0.0, 1.0, params.mwSaturation, v -> { params.mwSaturation = v; regenIfAuto(); });
        g.add(sMWS, 1, row, 3, 1); g.add(valLabel(sMWS), 4, row++);

        g.add(new Label("Tilt (deg):"), 0, row);
        Slider sMWTilt = slider(-45, 45, params.mwTiltDeg, v -> { params.mwTiltDeg = v; regenIfAuto(); });
        g.add(sMWTilt, 1, row, 3, 1); g.add(valLabel(sMWTilt), 4, row++);

        g.add(new Label("Vertical Offset:"), 0, row);
        Slider sMWOff = slider(-0.5, 0.5, params.mwVOffset, v -> { params.mwVOffset = v; regenIfAuto(); });
        g.add(sMWOff, 1, row, 3, 1); g.add(valLabel(sMWOff), 4, row++);

        g.add(new Label("Noise:"), 0, row);
        Slider sMWNoise = slider(0.0, 1.0, params.mwNoise, v -> { params.mwNoise = v; regenIfAuto(); });
        g.add(sMWNoise, 1, row, 3, 1); g.add(valLabel(sMWNoise), 4, row++);

        // --- Debug grid ---
        g.add(new Separator(), 0, row++, 5, 1);
        CheckBox cbDbg = new CheckBox("Show debug grid");
        cbDbg.setSelected(params.debugGrid);
        cbDbg.selectedProperty().addListener((o,ov,nv)-> { params.debugGrid = nv; regenIfAuto(); });
        g.add(cbDbg, 0, row++, 3, 1);

        g.add(new Label("Grid opacity:"), 0, row);
        Slider sDbgA = slider(0.05, 1.0, params.debugAlpha, v -> { params.debugAlpha = v; regenIfAuto(); });
        g.add(sDbgA, 1, row, 3, 1); g.add(valLabel(sDbgA), 4, row++);

        // --- Strict cross layout ---
        CheckBox cbStrict = new CheckBox("Strict cross layout (mask unused cells)");
        cbStrict.setSelected(params.strictCrossMask);
        cbStrict.selectedProperty().addListener((o,ov,nv)-> { params.strictCrossMask = nv; regenIfAuto(); });
        g.add(cbStrict, 0, row++, 5, 1);

        // --- Zoom control (preview only) ---
        Label zoomLabel = new Label("Zoom:");
        Slider zoomSlider = new Slider(0.25, 3.0, 1.0);
        zoomSlider.setShowTickMarks(true);
        zoomSlider.setShowTickLabels(true);
        zoomSlider.setMajorTickUnit(0.5);
        zoomSlider.setBlockIncrement(0.05);

        Label zoomVal = new Label("100%");
        zoomSlider.valueProperty().addListener((obs, ov, nv) -> {
            double z = nv.doubleValue();
            preview.setScaleX(z);
            preview.setScaleY(z);
            zoomVal.setText(String.format("%.0f%%", z * 100.0));
        });

        HBox zoomBox = new HBox(8, zoomLabel, zoomSlider, zoomVal);
        zoomBox.setAlignment(Pos.CENTER_LEFT);
        g.add(zoomBox, 0, row++, 5, 1);

        // Auto & Generate
        autoToggle = new CheckBox("Auto-generate on change");
        autoToggle.setSelected(true);
        Button btnGenerate = new Button("Generate Now");
        btnGenerate.setOnAction(e -> regenerate());

        HBox actions = new HBox(10, autoToggle, btnGenerate);
        actions.setAlignment(Pos.CENTER_LEFT);
        g.add(actions, 0, row, 5, 1);

        return g;
    }

    // --------- Helpers ----------

    private void setResolutionFromPreset(ResPreset p) {
        if (p == null) return;
        params.width = p.w;
        params.height = p.h;
    }

    private ResPreset findPreset(int w, int h) {
        return PRESETS.stream().filter(p -> p.w == w && p.h == h).findFirst().orElse(null);
    }

    private Slider slider(double min, double max, double init, DoubleConsumer onChange) {
        Slider s = new Slider(min, max, init);
        s.setShowTickMarks(true);
        s.setShowTickLabels(true);
        s.setBlockIncrement((max - min) / 50.0);
        ChangeListener<Number> l = (obs, o, v) -> onChange.accept(v.doubleValue());
        s.valueProperty().addListener(l);
        return s;
    }

    private Label valLabel(Slider s) {
        Label L = new Label(format(s.getValue()));
        s.valueProperty().addListener((obs, o, v) -> L.setText(format(v.doubleValue())));
        return L;
    }

    private String format(double v) {
        if (v >= 1.0) return String.format("%.3f", v);
        if (v >= 0.1) return String.format("%.4f", v);
        return String.format("%.5f", v);
    }

    private void regenIfAuto() {
        if (autoToggle != null && autoToggle.isSelected()) regenerate();
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
