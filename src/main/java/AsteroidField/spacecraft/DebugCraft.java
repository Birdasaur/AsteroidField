package AsteroidField.spacecraft;

import javafx.collections.ListChangeListener;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Transform;

/** Simple “ship” proxy to visualize the camera/craft in mini views. */
public class DebugCraft extends Group {
    private final Group hull = new Group();

    public DebugCraft() {
        // Body (small box) aligned to -Z “forward”
        Box body = new Box(18, 10, 36);
        body.setMaterial(new PhongMaterial(Color.web("#FFAF3F")));
        body.setCullFace(CullFace.NONE);

        // Nose (short cylinder) rotated so its axis is along Z
        Cylinder nose = new Cylinder(4, 8);
        nose.getTransforms().add(new Rotate(90, Rotate.X_AXIS)); // Y-axis -> Z-axis
        nose.setTranslateZ(-22); // in front of body (toward -Z)
        nose.setMaterial(new PhongMaterial(Color.web("#FFD27A")));

        // Tail fin
        Box fin = new Box(2, 16, 8);
        fin.setTranslateY(-12);
        fin.setTranslateZ(10);
        fin.setMaterial(new PhongMaterial(Color.web("#7EC8FF")));

        hull.getChildren().addAll(body, nose, fin);
        getChildren().add(hull);
        setMouseTransparent(true); // never intercept input
    }
    public void followNode(javafx.scene.Node src) {
        // copy transforms list
        getTransforms().setAll(src.getTransforms());
        src.getTransforms().addListener((javafx.collections.ListChangeListener<? super javafx.scene.transform.Transform>) c ->
            getTransforms().setAll(src.getTransforms())
        );
        // bind simple properties
        rotateProperty().bind(src.rotateProperty());
        rotationAxisProperty().bind(src.rotationAxisProperty());
        translateXProperty().bind(src.translateXProperty());
        translateYProperty().bind(src.translateYProperty());
        translateZProperty().bind(src.translateZProperty());
    }
    /**
     * Mirror a camera’s pose (transforms + rotate/axis + translates) onto this node.
     * Useful when the camera itself is not in the scene graph (SubScene.camera).
     */
    public void bindToCamera(PerspectiveCamera cam) {
        // 1) Keep our transforms list in sync with the camera’s transforms
        getTransforms().setAll(cam.getTransforms());
        cam.getTransforms().addListener((ListChangeListener<? super Transform>) c -> {
            getTransforms().setAll(cam.getTransforms());
        });

        // 2) Bind the simple properties
        rotateProperty().bind(cam.rotateProperty());
        rotationAxisProperty().bind(cam.rotationAxisProperty());
        translateXProperty().bind(cam.translateXProperty());
        translateYProperty().bind(cam.translateYProperty());
        translateZProperty().bind(cam.translateZProperty());
    }

    /** Size the craft proxy in world units. */
    public void setScale(double s) {
        hull.setScaleX(s);
        hull.setScaleY(s);
        hull.setScaleZ(s);
    }
}
