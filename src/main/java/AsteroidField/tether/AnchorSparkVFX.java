package AsteroidField.tether.vfx;

import javafx.animation.*;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.effect.Bloom;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.util.Duration;

import java.util.concurrent.ThreadLocalRandom;

public final class AnchorSparkVFX {
    private AnchorSparkVFX() {}

    /** Play a short radial “spark” burst at the impact point. */
    public static void play(Group worldRoot,
                            Point3D position,
                            Point3D outwardNormal,   // can be null → randomize
                            Color   color) {
        if (worldRoot == null || position == null) return;

        final int    count       = 10;          // number of sparks
        final double minLen      = 10;          // world units
        final double maxLen      = 32;
        final double radius      = 0.7;         // rod thickness
        final double spreadDeg   = 35;          // cone spread around normal
        final double driftFactor = 0.35;        // how far the tip “slides” outward
        final Duration life      = Duration.millis(220);

        Point3D baseDir = (outwardNormal == null || outwardNormal.magnitude() < 1e-6)
                ? new Point3D(0, 1, 0) : outwardNormal.normalize();

        for (int i = 0; i < count; i++) {
            Point3D dir = jitterInCone(baseDir, spreadDeg);
            double   len = rand(minLen, maxLen);

            // Build a small rod whose BASE is at local origin, aligned +Y and length 1.
            Cylinder cyl = new Cylinder(radius, 1);
            cyl.setCullFace(CullFace.NONE);
            cyl.setMouseTransparent(true);

            PhongMaterial mat = new PhongMaterial(color != null ? color : Color.LIGHTGOLDENRODYELLOW);
            mat.setSpecularColor(Color.WHITE);
            mat.setSpecularPower(128);
            cyl.setMaterial(mat);
            cyl.setEffect(new Bloom(0.2)); // subtle pop; safe in JavaFX 3D

            // Local “anchor at base”: move cylinder’s center up by 0.5 so y=0 is the base
            Translate localBase = new Translate(0, 0.5, 0);

            // Orient from +Y to direction
            Rotate orient = rotationFromY(dir);

            // Scale Y to the world length
            Scale scale = new Scale(1, len, 1);

            // Drift outward along its local +Y (after rotation) to sell motion
            Translate drift = new Translate(0, 0, 0);

            Group rod = new Group(cyl);
            rod.getTransforms().addAll(localBase);                 // base anchored at origin
            Group oriented = new Group(rod);
            oriented.getTransforms().addAll(orient, scale, drift); // then rotate/scale/drift
            Group atPos = new Group(oriented);
            atPos.setTranslateX(position.getX());
            atPos.setTranslateY(position.getY());
            atPos.setTranslateZ(position.getZ());
            atPos.setOpacity(0.95);

            worldRoot.getChildren().add(atPos);

            // Animate: grow length quickly, drift a bit, fade out, then remove
            Timeline tl = new Timeline(
                // Length grow: scaleY 0 → 1 happens via scale already at len; animate a factor using rod scaleY
                new KeyFrame(Duration.ZERO,
                    new KeyValue(oriented.scaleYProperty(), 0.0, Interpolator.EASE_OUT),
                    new KeyValue(atPos.opacityProperty(), 0.95, Interpolator.DISCRETE)
                ),
                new KeyFrame(life.multiply(0.35),
                    new KeyValue(oriented.scaleYProperty(), 1.0, Interpolator.EASE_OUT)
                ),
                // Drift outward a bit along its oriented local +Y
                new KeyFrame(life,
                    new KeyValue(drift.yProperty(), len * driftFactor, Interpolator.EASE_IN),
                    new KeyValue(atPos.opacityProperty(), 0.0, Interpolator.EASE_IN)
                )
            );
            final Group toRemove = atPos;
            tl.setOnFinished(ev -> worldRoot.getChildren().remove(toRemove));
            tl.play();
        }
    }

    // --- helpers ---

    private static double rand(double lo, double hi) {
        return ThreadLocalRandom.current().nextDouble(lo, hi);
    }

    private static Rotate rotationFromY(Point3D dir) {
        Point3D y = new Point3D(0, 1, 0);
        Point3D d = dir.normalize();
        double dot = clamp(y.dotProduct(d), -1, 1);
        double angle = Math.toDegrees(Math.acos(dot));
        Point3D axis = y.crossProduct(d);
        if (axis.magnitude() < 1e-6) axis = new Point3D(1, 0, 0); // parallel/anti-parallel
        return new Rotate(angle, axis);
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    /** Random unit vector within a cone around axis by 'spreadDeg'. */
    private static Point3D jitterInCone(Point3D axis, double spreadDeg) {
        Point3D a = axis.normalize();
        double spreadRad = Math.toRadians(spreadDeg);
        double u = rand(0, 1);
        double v = rand(0, 1);
        double theta = 2 * Math.PI * u;
        double cosAlpha = 1 - v * (1 - Math.cos(spreadRad)); // uniform over cone cap
        double sinAlpha = Math.sqrt(1 - cosAlpha * cosAlpha);

        // build an orthonormal basis around axis a
        Point3D ortho = Math.abs(a.getY()) < 0.99 ? new Point3D(0,1,0) : new Point3D(1,0,0);
        Point3D b1 = a.crossProduct(ortho).normalize();
        Point3D b2 = a.crossProduct(b1).normalize();

        return a.multiply(cosAlpha)
             .add(b1.multiply(sinAlpha * Math.cos(theta)))
             .add(b2.multiply(sinAlpha * Math.sin(theta)));
    }
}
