import AsteroidField.util.Math3D;
import AsteroidField.util.Math3D.Basis;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Affine;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Transform;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.PointLight;
import javafx.geometry.Point3D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InertialHudOverlay extends Canvas {

    public enum Mode { Minimal, Debug }

    private boolean enabled = true;
    private Mode mode = Mode.Minimal;

    // Cached, smoothed inputs
    private Point3D v_velocity = Point3D.ZERO;
    private Point3D v_thrust   = Point3D.ZERO;
    private List<Point3D> v_tethers = new ArrayList<>();
    private Point3D v_cameraFwd = new Point3D(0,0,-1);

    // Rendering config
    private double radarRadius = 80;           // px
    private double uiScale = 1.0;              // multiplier for overall size
    private double smoothing = 0.25;           // 0..1 lowpass for noisy inputs
    private double speedScale = 1.0;           // world-units -> px scaling
    private boolean snapToBottomLeft = true;   // anchor placement

    // Colors
    private final Color colFrame    = Color.gray(0.95, 0.8);
    private final Color colCross    = Color.gray(0.85, 0.8);
    private final Color colVel      = Color.CYAN;
    private final Color colThrust   = Color.GOLD;
    private final Color colTether   = Color.LIMEGREEN;

    public InertialHudOverlay() {
        setMouseTransparent(true);
        widthProperty().addListener((obs, o, n) -> requestRender());
        heightProperty().addListener((obs, o, n) -> requestRender());
    }

    public void setMode(Mode m) {
        this.mode = (m == null ? Mode.Minimal : m);
        requestRender();
    }

    public void setEnabled(boolean on) {
        this.enabled = on;
        setVisible(on);
        requestRender();
    }

    public void setUiScale(double s) {
        this.uiScale = Math.max(0.5, s);
        requestRender();
    }

    /** How aggressively to smooth incoming vectors (0 = no smoothing, 1 = heavy). */
    public void setSmoothing(double factor) {
        this.smoothing = Math3D.clamp01(factor);
    }

    /** World-units to pixels for arrow lengths. */
    public void setSpeedScale(double scale) {
        this.speedScale = Math.max(0.0001, scale);
    }

    /** Call once per frame with fresh values (world space). */
    public void render(Point3D velocity,
                       Point3D cameraForward,
                       Point3D thrustDirOrScaled,   // pass (dir * magnitude) or actual world-space thrust vector
                       List<Point3D> tetherDirsWorld,
                       double scale /* additional scale multiplier for arrows */) {
        if (!enabled) return;

        // Smooth
        v_velocity = Math3D.lerp(v_velocity, Math3D.nullToZero(velocity), smoothing);
        v_thrust   = Math3D.lerp(v_thrust,   Math3D.nullToZero(thrustDirOrScaled), smoothing);
        v_cameraFwd = Math3D.lerp(v_cameraFwd, Math3D.nullToZero(cameraForward).normalize(), smoothing);

        v_tethers.clear();
        if (tetherDirsWorld != null) {
            for (Point3D t : tetherDirsWorld) v_tethers.add(Math3D.nullToZero(t));
        }

        // Adjust live scale
        this.speedScale = Math.max(0.0001, scale);

        requestRender();
    }

    private void requestRender() {
        if (!enabled) return;
        draw();
    }

    private void draw() {
        GraphicsContext g = getGraphicsContext2D();
        double w = getWidth(), h = getHeight();
        g.clearRect(0, 0, w, h);
        if (w <= 0 || h <= 0) return;

        g.setFontSmoothingType(FontSmoothingType.GRAY);
        g.setLineCap(StrokeLineCap.BUTT);
        g.setLineJoin(StrokeLineJoin.MITER);

        double baseR = radarRadius * uiScale;
        double cx = snapToBottomLeft ? (baseR + 16) : (w * 0.18);
        double cy = snapToBottomLeft ? (h - (baseR + 16)) : (h * 0.82);

        // Build camera basis (right/up) from forward
        Basis basis = Math3D.basisFromForward(v_cameraFwd);

        // Project vectors to 2D plane
        Point2D vel2 = Math3D.projectToRadar(v_velocity, basis).multiply(speedScale);
        Point2D thr2 = Math3D.projectToRadar(v_thrust,   basis).multiply(speedScale);
        List<Point2D> tethers2 = new ArrayList<>();
        for (Point3D t : v_tethers) tethers2.add(Math3D.projectToRadar(t, basis).multiply(speedScale));

        // Clamp lengths to radar
        vel2 = Math3D.clampToRadius(vel2, baseR);
        thr2 = Math3D.clampToRadius(thr2, baseR);
        for (int i=0;i<tethers2.size();i++) tethers2.set(i, Math3D.clampToRadius(tethers2.get(i), baseR));

        // Frame
        g.setStroke(colFrame);
        g.setLineWidth(1.5);
        g.strokeOval(cx - baseR, cy - baseR, baseR*2, baseR*2);
        g.setLineDashes(0);
        // rings
        g.setGlobalAlpha(0.7);
        g.strokeOval(cx - baseR*0.5, cy - baseR*0.5, baseR, baseR);
        g.setGlobalAlpha(1.0);

        // Crosshair = camera forward cue
        g.setStroke(colCross);
        g.setLineWidth(1.0);
        g.strokeLine(cx - baseR, cy, cx + baseR, cy);
        g.strokeLine(cx, cy - baseR, cx, cy + baseR);

        // Velocity arrow
        if (!Math3D.isZero(vel2)) {
            drawArrow(g, cx, cy, cx + vel2.getX(), cy - vel2.getY(), colVel, 2.5, "VEL", mode == Mode.Debug);
        }

        // Thrust arrow
        if (!Math3D.isZero(thr2)) {
            drawArrow(g, cx, cy, cx + thr2.getX(), cy - thr2.getY(), colThrust, 2.0, "THR", mode == Mode.Debug);
        }

        // Tether pulls
        if (!tethers2.isEmpty()) {
            for (int i = 0; i < tethers2.size(); i++) {
                Point2D p = tethers2.get(i);
                drawArrow(g, cx, cy, cx + p.getX(), cy - p.getY(), colTether, 1.7, (mode==Mode.Debug? "TE"+i: null), mode==Mode.Debug);
            }
        }

        if (mode == Mode.Debug) {
            g.setFill(Color.gray(0.95));
            g.setFont(Font.font(11));
            g.setTextAlign(TextAlignment.LEFT);
            g.setTextBaseline(VPos.TOP);
            String info = String.format(
                "HUD Debug\n|v|=%.2f  |thr|=%.2f\nforward=(%.2f,%.2f,%.2f)",
                v_velocity.magnitude(), v_thrust.magnitude(),
                v_cameraFwd.getX(), v_cameraFwd.getY(), v_cameraFwd.getZ()
            );
            g.fillText(info, cx + baseR + 10, cy - baseR);
        }
    }

    private void drawArrow(GraphicsContext g, double x0, double y0, double x1, double y1, Color c, double lw, String label, boolean drawLabel) {
        g.setStroke(c);
        g.setLineWidth(lw);
        g.setLineDashes(0);
        g.strokeLine(x0, y0, x1, y1);

        // Arrowhead
        double dx = x1 - x0, dy = y1 - y0;
        double len = Math.hypot(dx, dy);
        if (len < 1e-5) return;
        double ux = dx / len, uy = dy / len;
        double ah = Math.min(12 * uiScale, len * 0.25); // arrowhead length
        double aw = 0.6 * ah;

        double bx = x1 - ux * ah;
        double by = y1 - uy * ah;
        double nx = -uy, ny = ux;

        g.beginPath();
        g.moveTo(x1, y1);
        g.lineTo(bx + nx * aw, by + ny * aw);
        g.lineTo(bx - nx * aw, by - ny * aw);
        g.closePath();
        g.setFill(c.deriveColor(0,1,1,0.9));
        g.fill();

        if (drawLabel && label != null) {
            g.setFill(c);
            g.setFont(Font.font(10));
            g.setTextAlign(TextAlignment.CENTER);
            g.setTextBaseline(VPos.CENTER);
            g.fillText(label, x1 + nx * 10, y1 + ny * 10);
        }
    }
}
