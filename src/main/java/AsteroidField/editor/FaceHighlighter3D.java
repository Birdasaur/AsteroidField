package AsteroidField.editor;

import javafx.beans.binding.Bindings;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/**
 * Maintains an overlay MeshView (fill + wireframe) that highlights a single face of a base MeshView.
 * Uses a tiny normal offset to avoid z-fighting.
 */
public final class FaceHighlighter3D {

    private final MeshView base;
    private final TriangleMesh baseMesh;

    private final TriangleMesh triFill = new TriangleMesh();
    private final MeshView viewFill = new MeshView(triFill);

    private final TriangleMesh triWire = new TriangleMesh();
    private final MeshView viewWire = new MeshView(triWire);

    private int currentFace = -1;

    public FaceHighlighter3D(MeshView base) {
        this.base = base;
        if (!(base.getMesh() instanceof TriangleMesh tm)) {
            throw new IllegalArgumentException("Base MeshView must contain a TriangleMesh");
        }
        this.baseMesh = tm;

        // init single-triangle meshes
        setupTriangleMesh(triFill);
        setupTriangleMesh(triWire);

        // style
        viewFill.setDrawMode(DrawMode.FILL);
        viewFill.setCullFace(CullFace.NONE);
        viewFill.setMaterial(new PhongMaterial(Color.color(1.0, 0.93, 0.2, 0.45)));

        viewWire.setDrawMode(DrawMode.LINE);
        viewWire.setCullFace(CullFace.NONE);
        viewWire.setMaterial(new PhongMaterial(Color.color(0.0, 0.0, 0.0, 0.85)));

        // mirror all base transforms so overlay tracks base
        Bindings.bindContent(viewFill.getTransforms(), base.getTransforms());
        Bindings.bindContent(viewWire.getTransforms(), base.getTransforms());
        viewFill.translateXProperty().bind(base.translateXProperty());
        viewFill.translateYProperty().bind(base.translateYProperty());
        viewFill.translateZProperty().bind(base.translateZProperty());
        viewFill.rotateProperty().bind(base.rotateProperty());
        viewFill.rotationAxisProperty().bind(base.rotationAxisProperty());
        viewFill.scaleXProperty().bind(base.scaleXProperty());
        viewFill.scaleYProperty().bind(base.scaleYProperty());
        viewFill.scaleZProperty().bind(base.scaleZProperty());
        viewWire.translateXProperty().bind(base.translateXProperty());
        viewWire.translateYProperty().bind(base.translateYProperty());
        viewWire.translateZProperty().bind(base.translateZProperty());
        viewWire.rotateProperty().bind(base.rotateProperty());
        viewWire.rotationAxisProperty().bind(base.rotationAxisProperty());
        viewWire.scaleXProperty().bind(base.scaleXProperty());
        viewWire.scaleYProperty().bind(base.scaleYProperty());
        viewWire.scaleZProperty().bind(base.scaleZProperty());

        setVisible(false);
    }

    /** Adds both overlay nodes to a parent container (e.g., the same Group as the base). */
    public void attachTo(javafx.scene.Group parent) {
        if (!parent.getChildren().contains(viewFill)) parent.getChildren().add(viewFill);
        if (!parent.getChildren().contains(viewWire)) parent.getChildren().add(viewWire);
    }

    public MeshView getFillNode() { return viewFill; }
    public MeshView getWireNode() { return viewWire; }

    public void setVisible(boolean visible) {
        viewFill.setVisible(visible);
        viewWire.setVisible(visible);
    }

    public void clear() {
        currentFace = -1;
        setVisible(false);
    }

    /**
     * Update overlay to highlight the given face index.
     * @param faceIndex index in baseMesh.getFaces()/6
     */
    public void highlightFace(int faceIndex) {
        if (faceIndex < 0) { clear(); return; }
        if (faceIndex == currentFace) return;
        currentFace = faceIndex;

        var faces = baseMesh.getFaces();
        var points = baseMesh.getPoints();

        int baseIdx = faceIndex * 6;
        if (baseIdx + 4 >= faces.size()) { clear(); return; }

        int v0 = faces.get(baseIdx);
        int v1 = faces.get(baseIdx + 2);
        int v2 = faces.get(baseIdx + 4);

        int p0 = v0 * 3;
        int p1 = v1 * 3;
        int p2 = v2 * 3;

        // fetch triangle points
        float x0 = points.get(p0),     y0 = points.get(p0 + 1),     z0 = points.get(p0 + 2);
        float x1 = points.get(p1),     y1 = points.get(p1 + 1),     z1 = points.get(p1 + 2);
        float x2 = points.get(p2),     y2 = points.get(p2 + 1),     z2 = points.get(p2 + 2);

        // compute face normal
        double ux = x1 - x0, uy = y1 - y0, uz = z1 - z0;
        double vx = x2 - x0, vy = y2 - y0, vz = z2 - z0;
        double nx = uy * vz - uz * vy;
        double ny = uz * vx - ux * vz;
        double nz = ux * vy - uy * vx;
        double len = Math.sqrt(nx*nx + ny*ny + nz*nz);
        if (len < 1e-12) len = 1.0;
        nx /= len; ny /= len; nz /= len;

        // compute epsilon relative to triangle size
        double e0 = Math.sqrt(ux*ux + uy*uy + uz*uz);
        double e1 = Math.sqrt(vx*vx + vy*vy + vz*vz);
        double e2 = Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1) + (z2-z1)*(z2-z1));
        double avg = (e0 + e1 + e2) / 3.0;
        float eps = (float) (avg * 1e-3); // small outward offset

        // offset vertices along normal to prevent z-fighting
        float X0 = (float)(x0 + nx * eps), Y0 = (float)(y0 + ny * eps), Z0 = (float)(z0 + nz * eps);
        float X1 = (float)(x1 + nx * eps), Y1 = (float)(y1 + ny * eps), Z1 = (float)(z1 + nz * eps);
        float X2 = (float)(x2 + nx * eps), Y2 = (float)(y2 + ny * eps), Z2 = (float)(z2 + nz * eps);

        // update meshes
        updateSingleTriangle(triFill, X0, Y0, Z0, X1, Y1, Z1, X2, Y2, Z2);
        updateSingleTriangle(triWire, X0, Y0, Z0, X1, Y1, Z1, X2, Y2, Z2);

        setVisible(true);
    }

    private static void setupTriangleMesh(TriangleMesh tm) {
        tm.getPoints().setAll(
                0,0,0,
                0,0,0,
                0,0,0
        );
        tm.getTexCoords().setAll(0,0); // single dummy tex coord
        tm.getFaces().setAll(0,0, 1,0, 2,0);
    }

    private static void updateSingleTriangle(TriangleMesh tm,
                                             float x0,float y0,float z0,
                                             float x1,float y1,float z1,
                                             float x2,float y2,float z2) {
        tm.getPoints().setAll(
                x0,y0,z0,
                x1,y1,z1,
                x2,y2,z2
        );
        // faces + texcoords remain the same
    }
}
