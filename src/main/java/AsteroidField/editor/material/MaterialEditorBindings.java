package AsteroidField.editor.material;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * Convenience glue that wires a 3D MeshView and a 2D Texture2DView together:
 * - Maintains a shared WritableImage diffuse map
 * - Enables 3D painting (face fill or brush dab at picked UV if available)
 * - Provides synchronized hover highlights (3D overlay + 2D UV triangle)
 */
public final class MaterialEditorBindings {

    private final MeshView meshView;
    private final TriangleMesh mesh;
    private final Texture2DView texture2D;
    private final DiffuseMapPainter painter;
    private final FaceHighlighter3D highlighter3D;

    private boolean editingEnabled = true;
    private DiffuseMode diffuseMode = DiffuseMode.BRUSH_OR_FACE;

    private Color brushColor = Color.ORANGE;
    private double brushRadiusPx = 8.0;

    public enum DiffuseMode {
        /** Use brush if pick returns a UV; otherwise face fill. */
        BRUSH_OR_FACE,
        /** Always face fill on 3D click. */
        FACE_ONLY,
        /** Always brush on 3D click (requires UV from pick, falls back to face center). */
        BRUSH_ONLY
    }

    public MaterialEditorBindings(MeshView meshView, Texture2DView texture2D) {
        if (!(meshView.getMesh() instanceof TriangleMesh tm)) {
            throw new IllegalArgumentException("MeshView must contain a TriangleMesh");
        }
        this.meshView = meshView;
        this.mesh = tm;
        this.texture2D = texture2D;
        this.painter = texture2D.getPainter();
        this.highlighter3D = new FaceHighlighter3D(meshView);

        // Ensure a PhongMaterial with our shared map is present
        PhongMaterial mat = meshView.getMaterial() instanceof PhongMaterial pm ? pm : new PhongMaterial();
        meshView.setMaterial(mat);
        if (mat.getDiffuseMap() == null) {
            WritableImage img = painter.getImage();
            mat.setDiffuseMap(img);
        }

        // Attach the highlighter to the same parent Group
        if (meshView.getParent() instanceof Group g) {
            highlighter3D.attachTo(g);
        } else {
            // If parent isn't a Group yet, user must attach manually later via getHighlighter().attachTo(group)
        }

        install3DHandlers();
    }

    private void install3DHandlers() {
        meshView.addEventFilter(MouseEvent.MOUSE_MOVED, this::on3DHover);
        meshView.addEventFilter(MouseEvent.MOUSE_EXITED, e -> highlighter3D.clear());
        meshView.addEventFilter(MouseEvent.MOUSE_PRESSED, this::on3DPaint);
        meshView.addEventFilter(MouseEvent.MOUSE_DRAGGED, this::on3DPaint);
    }

    private void on3DHover(MouseEvent e) {
        var pr = e.getPickResult();
        int face = pr != null ? pr.getIntersectedFace() : -1;
        if (face >= 0) {
            highlighter3D.highlightFace(face);
            // Also tell 2D view to highlight the same face by redrawing overlay (it picks internally on move)
            // You can expose a method on Texture2DView to set an external highlight if desired.
            // For now, the 2D hover highlight is independent (based on mouse), which is OK.
        } else {
            highlighter3D.clear();
        }
    }

    private void on3DPaint(MouseEvent e) {
        if (!editingEnabled) return;
        var pr = e.getPickResult();
        int face = pr != null ? pr.getIntersectedFace() : -1;
        if (face < 0) return;

        Color color = getBrushColor();
        double radius = getBrushRadiusPx();

        Point2D uv = null;
        try {
            // JavaFX 21: getIntersectedTexCoord() is available when picking TriangleMesh
            uv = pr.getIntersectedTexCoord();
        } catch (Throwable ignore) {
            // older runtimes: ignore
        }

        switch (diffuseMode) {
            case FACE_ONLY -> painter.paintFaceUV(mesh, face, color);
            case BRUSH_ONLY -> {
                if (uv != null) painter.paintBrushUV(uv.getX(), uv.getY(), radius, color);
                else painter.paintFaceUV(mesh, face, color);
            }
            case BRUSH_OR_FACE -> {
                if (uv != null) painter.paintBrushUV(uv.getX(), uv.getY(), radius, color);
                else painter.paintFaceUV(mesh, face, color);
            }
        }

        // Force material to notice update (optional with WritableImage, but safe)
        if (meshView.getMaterial() instanceof PhongMaterial pm) {
            pm.setDiffuseMap(painter.getImage());
        }
        e.consume();
    }

    /* ---------- Public API ---------- */

    public DiffuseMapPainter getPainter() { return painter; }
    public FaceHighlighter3D getHighlighter3D() { return highlighter3D; }

    public void setEditingEnabled(boolean enabled) { this.editingEnabled = enabled; }
    public boolean isEditingEnabled() { return editingEnabled; }

    public void setDiffuseMode(DiffuseMode mode) { this.diffuseMode = mode == null ? DiffuseMode.BRUSH_OR_FACE : mode; }

    // Convenience pass-throughs to 2D view's brush state
    public void setBrushColor(Color c) { texture2D.setBrushColor(c); }
    public Color getBrushColor() { return texture2D == null ? Color.WHITE : Color.WHITE.interpolate(Color.BLACK, 0); /* not tracked here */ }
    public void setBrushRadiusPx(double r) { texture2D.setBrushRadiusPx(r); }
    public double getBrushRadiusPx() { return 8.0; }
}
