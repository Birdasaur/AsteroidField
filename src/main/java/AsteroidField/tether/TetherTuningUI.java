package AsteroidField.tether;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.VBox;

/** Compact VBox with Spinners for tether physics (one panel, applies to all tethers). */
public final class TetherTuningUI {

    private TetherTuningUI() {}

    /** Build a VBox that edits all tethers in the system. */
    public static VBox build(TetherController controller) {
        VBox box = new VBox(8);
        box.setPadding(new Insets(8));
        box.setFillWidth(true);
        box.setId("tether-tuning");

        // Use tether[0] for initial values; fall back to sensible defaults
        Tether t0 = controller.getTether(0);
        double k0       = (t0 != null) ? t0.getStiffness()        : 160;
        double zeta0    = (t0 != null) ? t0.getDampingRatio()     : 0.9;
        double reel0    = (t0 != null) ? t0.getReelRate()         : 240;
        double fmax0    = (t0 != null) ? t0.getMaxForce()         : 900;
        double slack0   = (t0 != null) ? t0.getSlackEps()         : 0.02;
        double zPerp0   = (t0 != null) ? t0.getPerpDampingRatio() : 0.15; // perpendicular damping

        // Compact spinners
        Spinner<Double> spK     = dblSpinner(20,   2000,  k0,    10);
        Spinner<Double> spZ     = dblSpinner(0.0,  2.0,   zeta0, 0.05);
        Spinner<Double> spReel  = dblSpinner(0,    2000,  reel0, 10);
        Spinner<Double> spFmax  = dblSpinner(0,    10000, fmax0, 50);
        Spinner<Double> spSlack = dblSpinner(0.0,  0.2,   slack0,0.005);
        Spinner<Double> spZPerp = dblSpinner(0.0,  1.0,   zPerp0,0.01);

        box.getChildren().addAll(
            new Label("Tethers — Physics"),
            labeled("Stiffness k (N/m)", spK),
            labeled("Damping ratio ζ", spZ),
            labeled("Reel rate (units/s)", spReel),
            labeled("Max force (N)", spFmax),
            labeled("Slack epsilon", spSlack),
            labeled("Perp damping ζ⊥", spZPerp)
        );

        // Apply changes to ALL tethers
        spK.valueProperty().addListener((o, ov, nv) -> forAll(controller, t -> t.setStiffness(          safe(nv, 160))));
        spZ.valueProperty().addListener((o, ov, nv) -> forAll(controller, t -> t.setDampingRatio( clamp(safe(nv, 0.9), 0, 2))));
        spReel.valueProperty().addListener((o, ov, nv) -> forAll(controller, t -> t.setReelRate(          safe(nv, 240))));
        spFmax.valueProperty().addListener((o, ov, nv) -> forAll(controller, t -> t.setMaxForce(          safe(nv, 900))));
        spSlack.valueProperty().addListener((o, ov, nv) -> forAll(controller, t -> t.setSlackEps(   clamp(safe(nv, 0.02), 0, 1))));
        spZPerp.valueProperty().addListener((o, ov, nv) -> forAll(controller, t -> t.setPerpDampingRatio( clamp(safe(nv, 0.15), 0, 1))));

        return box;
    }

    // ---- helpers ----

    private static Spinner<Double> dblSpinner(double min, double max, double val, double step) {
        Spinner<Double> sp = new Spinner<>();
        sp.setEditable(true);
        sp.setPrefWidth(140);
        sp.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(min, max, val, step));
        return sp;
    }

    private static VBox labeled(String label, Spinner<?> sp) {
        VBox v = new VBox(2);
        v.getChildren().addAll(new Label(label), sp);
        return v;
    }

    private static void forAll(TetherController sys, java.util.function.Consumer<Tether> f) {
        for (int i = 0; ; i++) {
            Tether t = sys.getTether(i);
            if (t == null) break;
            f.accept(t);
        }
    }

    private static double safe(Double v, double def) { return v != null ? v : def; }
    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
}
