package AsteroidField.tether;

import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

/**
 * Visual tether using a Cylinder. The Cylinder is normally centered at local Y=0,
 * so we add a PRE-TRANSLATE of +0.5 in local Y before scaling to make its BASE at y=0.
 * Order: [localBase] -> [scaleY=len] -> [rotate] -> [worldTranslate=start].
 */
public class TetherView extends Group {

    // Main beam
    private final Cylinder beam;
    private final Translate localBase;   // shifts center up by +0.5 so base is at 0
    private final Scale scale;           // scales height 1 -> len
    private final Rotate rotate;         // orients +Y -> direction
    private final Translate worldTranslate; // moves base to start

    // Debug markers
    private final Sphere startMarker;
    private final Sphere endMarker;

    private boolean showMarkers = true;   // turn on for debugging

    public TetherView(int radialDivisionsIgnored, float radius, Color color) {
        // Cylinder of height=1 oriented along +Y
        beam = new Cylinder(radius, 100.0);
        beam.setCullFace(CullFace.NONE);
        beam.setDrawMode(DrawMode.LINE);       // FILL for maximum visibility
        beam.setMaterial(new PhongMaterial(color));
        beam.setMouseTransparent(true);        // never block picking

        // Transforms: note the order they are ADDED is the order applied
        localBase = new Translate(0, +0.5, 0); // base at y=0 after this
        scale = new Scale(1, 0.0001, 1);       // set Y to 'len' in setStartAndEnd
        rotate = new Rotate(0, new Point3D(0, 0, 1));
        worldTranslate = new Translate(0, 0, 0);

        beam.getTransforms().addAll(localBase, scale, rotate, worldTranslate);

        // Debug markers
        startMarker = new Sphere(radius * 1.8);
        startMarker.setMaterial(new PhongMaterial(Color.LIMEGREEN));
        startMarker.setCullFace(CullFace.NONE);
        startMarker.setDrawMode(DrawMode.LINE);
        startMarker.setMouseTransparent(true);

        endMarker = new Sphere(radius * 2.2);
        endMarker.setMaterial(new PhongMaterial(Color.RED));
        endMarker.setCullFace(CullFace.NONE);
        endMarker.setDrawMode(DrawMode.LINE);
        endMarker.setMouseTransparent(true);

        getChildren().addAll(beam, startMarker, endMarker);
        setVisible(false);
        updateMarkerVisibility();
    }

    public void setShowMarkers(boolean show) {
        this.showMarkers = show;
        updateMarkerVisibility();
    }

    private void updateMarkerVisibility() {
        startMarker.setVisible(showMarkers);
        endMarker.setVisible(showMarkers);
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

        // Beam
        Point3D dir = end.subtract(start);
        double len = dir.magnitude();
        if (len < 1e-6) len = 1e-6;
        Point3D ndir = dir.normalize();

        // Scale Y to length (base stays at y=0 because of localBase)
        scale.setX(1);
        scale.setY(len);
        scale.setZ(1);

        // Rotate from +Y to target direction
        Point3D yAxis = new Point3D(0, 1, 0);
        double dot = clamp(yAxis.dotProduct(ndir), -1, 1);
        double angleDeg = Math.toDegrees(Math.acos(dot));
        Point3D rotAxis = yAxis.crossProduct(ndir);
        if (rotAxis.magnitude() < 1e-6) {
            rotAxis = new Point3D(1, 0, 0);
            angleDeg = (dot > 0) ? 0 : 180;
        }
        rotate.setAxis(rotAxis);
        rotate.setAngle(angleDeg);

        // Move base to start
        worldTranslate.setX(start.getX());
        worldTranslate.setY(start.getY());
        worldTranslate.setZ(start.getZ());
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
