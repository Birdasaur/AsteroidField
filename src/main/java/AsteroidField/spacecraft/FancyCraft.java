package AsteroidField.spacecraft;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;

/** A more distinct spacecraft proxy for gameplay + debugging. */
public class FancyCraft extends Group {
    private final Group hull = new Group();

    public FancyCraft() {
        // Nose (cone-like: use a short cylinder + sphere tip)
        Cylinder nose = new Cylinder(5, 20);
        nose.setMaterial(new PhongMaterial(Color.SILVER));
        nose.setTranslateZ(-30); // extend forward along -Z

        Sphere tip = new Sphere(5);
        tip.setMaterial(new PhongMaterial(Color.LIGHTGRAY));
        tip.setTranslateZ(-40);

        // Fuselage
        Box body = new Box(15, 10, 60);
        body.setMaterial(new PhongMaterial(Color.DARKSLATEGRAY));

        // Wings
        Box leftWing  = new Box(2, 30, 10);
        Box rightWing = new Box(2, 30, 10);
        leftWing.setMaterial(new PhongMaterial(Color.GRAY));
        rightWing.setMaterial(new PhongMaterial(Color.GRAY));
        leftWing.setTranslateX(-12);
        rightWing.setTranslateX(+12);

        // Tail engines
        Cylinder engineL = new Cylinder(4, 12);
        engineL.setMaterial(new PhongMaterial(Color.DODGERBLUE));
        engineL.getTransforms().add(new Rotate(90, Rotate.X_AXIS));
        engineL.setTranslateX(-6);
        engineL.setTranslateZ(+35);

        Cylinder engineR = new Cylinder(4, 12);
        engineR.setMaterial(new PhongMaterial(Color.DODGERBLUE));
        engineR.getTransforms().add(new Rotate(90, Rotate.X_AXIS));
        engineR.setTranslateX(+6);
        engineR.setTranslateZ(+35);

        hull.getChildren().addAll(body, nose, tip, leftWing, rightWing, engineL, engineR);

        getChildren().add(hull);
        setMouseTransparent(true);
    }

    public void setScale(double s) {
        setScaleX(s);
        setScaleY(s);
        setScaleZ(s);
    }

    // Optional: add axis markers for debugging
    public void addDebugAxes(double len) {
        Box xAxis = new Box(len, 1, 1);
        xAxis.setMaterial(new PhongMaterial(Color.RED));
        xAxis.setTranslateX(len / 2.0);

        Box yAxis = new Box(1, len, 1);
        yAxis.setMaterial(new PhongMaterial(Color.GREEN));
        yAxis.setTranslateY(len / 2.0);

        Box zAxis = new Box(1, 1, len);
        zAxis.setMaterial(new PhongMaterial(Color.BLUE));
        zAxis.setTranslateZ(-len / 2.0); // -Z is forward

        getChildren().addAll(xAxis, yAxis, zAxis);
    }
}
