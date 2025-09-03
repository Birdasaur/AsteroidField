package AsteroidField.tether;

import javafx.geometry.Point3D;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.transform.Affine;

public final class RayUtil {
    private RayUtil(){}

    public static class Ray {
        public final Point3D origin;
        public final Point3D dir; // normalized
        public Ray(Point3D o, Point3D d){ origin=o; dir=d.normalize(); }
    }

    /**
     * Compute a world-space ray from subscene coordinates (mouseX, mouseY).
     *
     * JavaFX typical usage: camera at negative Z looking toward origin => camera "forward" is +Z.
     * IMPORTANT: Use yNdc = (2*y/h) - 1 so that a click at the TOP of the viewport points UP.
     */
    public static Ray computePickRay(PerspectiveCamera cam, SubScene subScene, double mouseX, double mouseY) {
        double w = subScene.getWidth();
        double h = subScene.getHeight();
        double aspect = (w <= 0 || h <= 0) ? 1.0 : (w / h);

        // Normalized device coordinates [-1, 1]
        double xNdc = (2.0 * mouseX / w) - 1.0;   // -1 left, +1 right
        double yNdc = (2.0 * mouseY / h) - 1.0;   // -1 top, +1 bottom  (<< fixed)

        double fovYRad = Math.toRadians(cam.getFieldOfView());
        double tan = Math.tan(fovYRad / 2.0);

        // Camera-local forward is +Z.
        Point3D dirCam = new Point3D(xNdc * aspect * tan, yNdc * tan, +1).normalize();

        // Transform to SCENE space
        Affine toScene = new Affine(cam.getLocalToSceneTransform());
        Point3D camScene = toScene.transform(Point3D.ZERO);
        Point3D dirScene = toScene.deltaTransform(dirCam).normalize();

        return new Ray(camScene, dirScene);
    }
}
