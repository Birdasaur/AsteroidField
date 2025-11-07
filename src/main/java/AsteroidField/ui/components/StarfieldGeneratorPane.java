package AsteroidField.ui.components;

import AsteroidField.ui.scene3d.StarAtlasGenerator;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.util.Random;
import java.util.function.DoubleConsumer;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

public class StarfieldGeneratorPane extends BorderPane {

    private final StarAtlasGenerator.Params params = new StarAtlasGenerator.Params();
    private final ImageView preview = new ImageView();
    private final Random rng = new Random();

    private CheckBox autoToggle;

    public StarfieldGeneratorPane() {
        setPadding(new Insets(8));
        setBackground(new Background(new BackgroundFill(Color.web("#111"), CornerRadii.EMPTY, Insets.EMPTY)));

        GridPane controls = buildControls();
        ScrollPane scroller = new ScrollPane(controls);
        scroller.setFitToWidth(true);
        scroller.setPannable(true);
        scroller.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scroller.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroller.setMinWidth(360);       // tweak as you like
        scroller.setPrefWidth(380);      // keeps labels readable
        setLeft(scroller);
        BorderPane.setMargin(scroller, new Insets(8, 6, 8, 8));

        preview.setPreserveRatio(true);
        preview.setSmooth(true);
        StackPane previewPane = new StackPane(preview);
        previewPane.setPadding(new Insets(8));
        setCenter(previewPane);

        regenerate();
    }

    // --------- Public API ----------

    public StarAtlasGenerator.Params getParams() { return params; }

    /** Always returns a fresh, full-resolution 4x3 atlas render (NOT the preview image). */
    public WritableImage getAtlasImage() {
        return StarAtlasGenerator.generate(params);
    }

    /** Latest six faces sliced from a fresh full-res atlas. */
    public StarAtlasGenerator.Faces getFaces() {
        return StarAtlasGenerator.generateFaces(params);
    }

    /** Force a new render (used by host apps as well). */
    public void regenerate() {
        WritableImage atlas = StarAtlasGenerator.generate(params);
        preview.setImage(atlas);
        preview.setFitWidth(820);
        preview.setFitHeight(820 * (params.height / (double) params.width));
    }

    // --------- UI setup ----------

    private GridPane buildControls() {
        GridPane g = new GridPane();
        g.setHgap(8);
        g.setVgap(8);
        g.setPadding(new Insets(12));
        int row = 0;

        // Resolution (must remain 4x3, square cells)
        TextField tfW = new TextField(String.valueOf(params.width));
        TextField tfH = new TextField(String.valueOf(params.height));
        tfW.setPrefColumnCount(6);
        tfH.setPrefColumnCount(6);
        Button btnApplyWH = new Button("Apply WxH");
        btnApplyWH.setOnAction(e -> {
            try {
                int w = Integer.parseInt(tfW.getText().trim());
                int h = Integer.parseInt(tfH.getText().trim());
                if (w % 4 != 0 || h % 3 != 0 || (w / 4) != (h / 3)) {
                    throw new IllegalArgumentException("Width multiple of 4, height multiple of 3, cells square (width/4 == height/3).");
                }
                params.width = w;
                params.height = h;
                regenerate();
            } catch (Exception ex) {
                showError("Invalid WxH: " + ex.getMessage());
            }
        });

        g.add(new Label("Width:"), 0, row);  g.add(tfW, 1, row);
        g.add(new Label("Height:"), 2, row); g.add(tfH, 3, row);
        g.add(btnApplyWH, 4, row++);

        // Seed
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

        // Star density
        g.add(new Label("Star density:"), 0, row);
        Slider sDensity = slider(0.00005, 0.0005, params.starDensity, v -> { params.starDensity = v; regenIfAuto(); });
        g.add(sDensity, 1, row, 3, 1); g.add(valLabel(sDensity), 4, row++);

        // Color bias
        g.add(new Label("Color bias:"), 0, row);
        Slider sColor = slider(0.0, 1.0, params.colorBias, v -> { params.colorBias = v; regenIfAuto(); });
        g.add(sColor, 1, row, 3, 1); g.add(valLabel(sColor), 4, row++);

        // Radius mean / sigma / max
        g.add(new Label("Radius mean (px):"), 0, row);
        Slider sRMean = slider(0.05, 1.25, params.radiusMean, v -> { params.radiusMean = v; regenIfAuto(); });
        g.add(sRMean, 1, row, 3, 1); g.add(valLabel(sRMean), 4, row++);

        g.add(new Label("Radius sigma (px):"), 0, row);
        Slider sRSig = slider(0.05, 0.6, params.radiusSigma, v -> { params.radiusSigma = v; regenIfAuto(); });
        g.add(sRSig, 1, row, 3, 1); g.add(valLabel(sRSig), 4, row++);

        g.add(new Label("Radius max (px):"), 0, row);
        Slider sRMax = slider(0.5, 2.0, params.radiusMax, v -> { params.radiusMax = v; regenIfAuto(); });
        g.add(sRMax, 1, row, 3, 1); g.add(valLabel(sRMax), 4, row++);

        // Bright points
        g.add(new Label("Bright chance:"), 0, row);
        Slider sBChance = slider(0.0, 0.1, params.brightChance, v -> { params.brightChance = v; regenIfAuto(); });
        g.add(sBChance, 1, row, 3, 1); g.add(valLabel(sBChance), 4, row++);

        g.add(new Label("Bright multiplier:"), 0, row);
        Slider sBMul = slider(1.0, 2.0, params.brightMul, v -> { params.brightMul = v; regenIfAuto(); });
        g.add(sBMul, 1, row, 3, 1); g.add(valLabel(sBMul), 4, row++);

        // Nebula
        g.add(new Label("Nebula blobs:"), 0, row);
        Slider sNBlobs = slider(0, 120, params.nebulaBlobs, v -> { params.nebulaBlobs = (int)Math.round(v); regenIfAuto(); });
        g.add(sNBlobs, 1, row, 3, 1); g.add(valLabel(sNBlobs), 4, row++);

        g.add(new Label("Nebula alpha:"), 0, row);
        Slider sNAlpha = slider(0.0, 0.10, params.nebulaAlpha, v -> { params.nebulaAlpha = v; regenIfAuto(); });
        g.add(sNAlpha, 1, row, 3, 1); g.add(valLabel(sNAlpha), 4, row++);

        // Internal bleed
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

        // Debug grid
        g.add(new Separator(), 0, row++, 5, 1);
        CheckBox cbDbg = new CheckBox("Show debug grid");
        cbDbg.setSelected(params.debugGrid);
        cbDbg.selectedProperty().addListener((o,ov,nv)-> { params.debugGrid = nv; regenIfAuto(); });
        g.add(cbDbg, 0, row++, 3, 1);

        g.add(new Label("Grid opacity:"), 0, row);
        Slider sDbgA = slider(0.05, 1.0, params.debugAlpha, v -> { params.debugAlpha = v; regenIfAuto(); });
        g.add(sDbgA, 1, row, 3, 1); g.add(valLabel(sDbgA), 4, row++);

        // Auto & quick action
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
