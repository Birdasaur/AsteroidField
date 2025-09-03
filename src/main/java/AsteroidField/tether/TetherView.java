package AsteroidField.tether;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Affine;

/**
 * Tether view that uses a single Affine to orient the cylinder.
 * - Cylinder is centered at its local origin, height runs along local +Y from -h/2..+h/2
 * - We set height = |end-start|
 * - Build an orthonormal basis where local Y maps to (end-start).normalize()
 * - Place the cylinder center at the midpoint
 *
 * Now supports independent visibility toggles for start/end markers.
 */
public class TetherView extends Group {

    private final Cylinder beam;
    private final Affine   xform;

    // Debug markers
    private final Sphere startMarker;
    private final Sphere endMarker;

    // Independent toggles. Default to off since these are mostly for debug
    private boolean showStartMarker = false;
    private boolean showEndMarker   = false;

    public TetherView(int radialDivisionsIgnored, float radius, Color color) {
        beam = new Cylinder(radius, 1.0);
        beam.setMaterial(new PhongMaterial(color));
        beam.setCullFace(CullFace.NONE);
        beam.setDrawMode(DrawMode.FILL);      // Use FILL for max visibility (change to LINE if you prefer)
        beam.setMouseTransparent(true);       // never block picking

        xform = new Affine();
        beam.getTransforms().add(xform);

        // Debug markers (mouseTransparent so they don't block ray casts)
        startMarker = new Sphere(radius * 1.8);
        startMarker.setMaterial(new PhongMaterial(Color.LIMEGREEN));
        startMarker.setCullFace(CullFace.NONE);
        startMarker.setDrawMode(DrawMode.FILL);
        startMarker.setMouseTransparent(true);

        endMarker = new Sphere(radius * 2.2);
        endMarker.setMaterial(new PhongMaterial(Color.RED));
        endMarker.setCullFace(CullFace.NONE);
        endMarker.setDrawMode(DrawMode.FILL);
        endMarker.setMouseTransparent(true);

        getChildren().addAll(beam, startMarker, endMarker);
        setVisible(false);
        updateMarkerVisibility();
    }

    // --- Independent marker controls ---
    public void setShowStartMarker(boolean show) {
        this.showStartMarker = show;
        updateMarkerVisibility();
    }
    public void setShowEndMarker(boolean show) {
        this.showEndMarker = show;
        updateMarkerVisibility();
    }
    public void setMarkerVisibility(boolean showStart, boolean showEnd) {
        this.showStartMarker = showStart;
        this.showEndMarker   = showEnd;
        updateMarkerVisibility();
    }
    public boolean isStartMarkerVisible() { return showStartMarker; }
    public boolean isEndMarkerVisible()   { return showEndMarker; }

    private void updateMarkerVisibility() {
        startMarker.setVisible(showStartMarker);
        endMarker.setVisible(showEndMarker);
    }

    public void setVisibleAndPickOnBounds(boolean visible) {
        setVisible(visible);
        setPickOnBounds(visible);
    }

    public void setStartAndEnd(Point3D start, Point3D end) {
        // Markers
        startMarker.setTranslateX(start.getX());
        startMarker.setTranslateY(start.getY());
        startMarker.setTranslateZ(start.getZ());

        endMarker.setTranslateX(end.getX());
        endMarker.setTranslateY(end.getY());
        endMarker.setTranslateZ(end.getZ());

        // Segment math
        Point3D dir = end.subtract(start);
        double len = dir.magnitude();
        if (len < 1e-6) len = 1e-6;
        Point3D v   = dir.normalize();         // target for local +Y
        Point3D mid = start.midpoint(end);     // cylinder center in world

        // Build an orthonormal frame: right (X), up (Z), v (Y)
        Point3D refUp = (Math.abs(v.getY()) < 0.99) ? new Point3D(0, 1, 0) : new Point3D(1, 0, 0);
        Point3D right = v.crossProduct(refUp);
        double rmag = right.magnitude();
        if (rmag < 1e-6) {
            refUp = new Point3D(0, 0, 1);
            right = v.crossProduct(refUp);
            rmag = right.magnitude();
            if (rmag < 1e-6) right = new Point3D(1, 0, 0);
        }
        right = right.normalize();
        Point3D up = right.crossProduct(v).normalize();

        // Set cylinder length (centered at origin in its local space)
        beam.setHeight(len);

        // Affine from basis vectors (columns) and translation (midpoint)
        xform.setMxx(right.getX()); xform.setMxy(v.getX()); xform.setMxz(up.getX()); xform.setTx(mid.getX());
        xform.setMyx(right.getY()); xform.setMyy(v.getY()); xform.setMyz(up.getY()); xform.setTy(mid.getY());
        xform.setMzx(right.getZ()); xform.setMzy(v.getZ()); xform.setMzz(up.getZ()); xform.setTz(mid.getZ());
    }
}
