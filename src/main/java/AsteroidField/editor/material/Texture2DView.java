package AsteroidField.editor.material;

import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.TriangleMesh;

/**
 * Interactive 2D texture editor:
 * - Displays the WritableImage backing the diffuse map.
 * - Overlay Canvas for painting and highlighting faces under the mouse.
 * - Uses UvSpatialIndex to pick faces from (u,v).
 */
public final class Texture2DView extends StackPane {

    public enum PaintMode { BRUSH, FACE }

    private final ImageView imageView;
    private final Canvas overlay;
    private final DiffuseMapPainter painter;
    private final TriangleMesh mesh;
    private final UvMapper uv;
    private final UvSpatialIndex uvIndex;

    // UI state
    private Color brushColor = Color.WHITE;
    private double brushRadiusPx = 6.0;
    private boolean paintingEnabled = true;
    private PaintMode paintMode = PaintMode.BRUSH;

    private int hoveredFace = -1;
    private int externalHoverFace = -1;
    private FaceHoverListener hoverListener;

    @FunctionalInterface
    public interface FaceHoverListener {
        void onHoverFace(int faceIndex);
    }

    public Texture2DView(WritableImage image, TriangleMesh mesh) {
        this.painter = new DiffuseMapPainter(image);
        this.mesh = mesh;
        this.uv = new UvMapper(mesh);
        this.uvIndex = new UvSpatialIndex(mesh, 64, 64); // 128 x 64 domain (U in [0,2))

        int w = (int) image.getWidth();
        int h = (int) image.getHeight();

        this.imageView = new ImageView(image);
        imageView.setPreserveRatio(false);
        imageView.setFitWidth(w);
        imageView.setFitHeight(h);

        this.overlay = new Canvas(w, h);
        overlay.setMouseTransparent(false);

        getChildren().addAll(imageView, overlay);
        setPadding(new Insets(4));

        enableInteraction();
    }

    private void enableInteraction() {
        overlay.addEventHandler(MouseEvent.MOUSE_MOVED, this::onHover);
        overlay.addEventHandler(MouseEvent.MOUSE_EXITED, e -> { hoveredFace = -1; redrawOverlay(-1, -1); });
        overlay.addEventHandler(MouseEvent.MOUSE_PRESSED, this::onPaint);
        overlay.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::onPaint);
    }

    private void onHover(MouseEvent e) {
        double x = clamp(e.getX(), 0, overlay.getWidth());
        double y = clamp(e.getY(), 0, overlay.getHeight());
        double u = x / overlay.getWidth();
        double v = y / overlay.getHeight();

        int face = uvIndex.pickFace(u, v);
        hoveredFace = face;
        overlay.setCursor(face >= 0 ? Cursor.HAND : Cursor.CROSSHAIR);
        redrawOverlay(x, y);
    }

    private void onPaint(MouseEvent e) {
        if (!paintingEnabled) return;
        double x = clamp(e.getX(), 0, overlay.getWidth());
        double y = clamp(e.getY(), 0, overlay.getHeight());
        double u = x / overlay.getWidth();
        double v = y / overlay.getHeight();

        if (paintMode == PaintMode.FACE && hoveredFace >= 0) {
            painter.paintFaceUV(mesh, hoveredFace, brushColor);
        } else {
            painter.paintBrushUV(u, v, brushRadiusPx, brushColor);
        }
        redrawOverlay(x, y);
        e.consume();
    }

    private void redrawOverlay(double mouseX, double mouseY) {
        GraphicsContext g = overlay.getGraphicsContext2D();
        double w = overlay.getWidth();
        double h = overlay.getHeight();
        g.clearRect(0, 0, w, h);

        // Hovered face highlight
        if (hoveredFace >= 0) {
            UvMapper.FaceUV f = uv.get(hoveredFace);
            if (f != null) {
                // Draw with seam-aware wrap: draw up to two copies if needed
                drawFaceHighlight(g, f.u0(), f.v0(), f.u1(), f.v1(), f.u2(), f.v2(), w, h);
            }
        }

        // Brush preview ring
        if (mouseX >= 0 && mouseY >= 0) {
            g.setStroke(Color.BLACK);
            g.setLineWidth(1);
            g.strokeOval(mouseX - brushRadiusPx, mouseY - brushRadiusPx, 2 * brushRadiusPx, 2 * brushRadiusPx);
        }
    }

    private static void drawFaceHighlight(GraphicsContext g,
                                          double u0, double v0,
                                          double u1, double v1,
                                          double u2, double v2,
                                          double w, double h) {
        // unwrap into [0,2)
        double[] U = new double[]{u0, u1, u2};
        double[] V = new double[]{v0, v1, v2};
        unwrapU(U);

        // draw copy near [0,1) domain
        drawTri(g, projectNear(U[0], 0.5), V[0], projectNear(U[1], 0.5), V[1], projectNear(U[2], 0.5), V[2], w, h);
        // if triangle lives in [1,2), also draw shifted back into [0,1)
        drawTri(g, projectNear(U[0]-1.0, 0.5), V[0], projectNear(U[1]-1.0, 0.5), V[1], projectNear(U[2]-1.0, 0.5), V[2], w, h);
    }

    private static void drawTri(GraphicsContext g,
                                double u0, double v0,
                                double u1, double v1,
                                double u2, double v2,
                                double w, double h) {
        double x0 = u0 * w, y0 = v0 * h;
        double x1 = u1 * w, y1 = v1 * h;
        double x2 = u2 * w, y2 = v2 * h;
        g.setFill(Color.color(1.0, 1.0, 0.0, 0.25));
        g.fillPolygon(new double[]{x0,x1,x2}, new double[]{y0,y1,y2}, 3);
        g.setStroke(Color.color(0,0,0,0.85));
        g.setLineWidth(1.0);
        g.strokePolygon(new double[]{x0,x1,x2}, new double[]{y0,y1,y2}, 3);
    }

    private static void unwrapU(double[] U) {
        double min = Math.min(U[0], Math.min(U[1], U[2]));
        double max = Math.max(U[0], Math.max(U[1], U[2]));
        if (max - min > 0.5) {
            for (int i = 0; i < 3; i++) if (U[i] < 0.5) U[i] += 1.0;
        }
    }

    private static double projectNear(double Up, double center) {
        double d = Up - center;
        if (d > 0.5) return Up - 1.0;
        if (d < -0.5) return Up + 1.0;
        return Up;
    }

    /* ---------- Public API ---------- */

    public DiffuseMapPainter getPainter() { return painter; }
    public javafx.scene.Node getNode() { return this; }

    public void setBrushColor(Color color) { this.brushColor = color; }
    public void setBrushRadiusPx(double r) { this.brushRadiusPx = Math.max(0.5, r); }
    public void setPaintingEnabled(boolean enabled) { this.paintingEnabled = enabled; }
    public void setPaintMode(PaintMode mode) { this.paintMode = mode == null ? PaintMode.BRUSH : mode; }
    public void setExternalHoverFace(int faceIndex) { this.externalHoverFace = faceIndex; redrawOverlay(-1, -1); }
    public void setFaceHoverListener(FaceHoverListener l) { this.hoverListener = l; }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
}
