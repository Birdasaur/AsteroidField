package AsteroidField.ui.scene3d;

import javafx.animation.AnimationTimer;
import javafx.scene.AmbientLight;
import javafx.scene.DepthTest;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Transform;

/**
 * Minimal spherical skybox using a single 2:1 equirectangular texture.
 * - Follows camera position (no rotation) to avoid parallax.
 * - Uses an inverted sphere (CullFace.FRONT) so we render the inside.
 * - Lit by a local AmbientLight scoped only to the sphere (so it’s always visible).
 */
public class SphericalSkybox extends Group {

    private final Sphere sphere;
    private final PhongMaterial material = new PhongMaterial();
    private final Affine follow = new Affine();
    private final PerspectiveCamera camera;
    private AnimationTimer timer;

    /**
     * @param texture  Equirectangular 2:1 image (e.g., 4096x2048, 8192x4096).
     * @param radius   Sphere radius; should be well inside camera farClip (e.g., farClip * 0.45).
     * @param camera   The scene camera to follow (position only).
     * @param divisions Sphere tesselation (32–128). 64 is a good default.
     */
    public SphericalSkybox(Image texture, double radius, PerspectiveCamera camera, int divisions) {
        this.camera = camera;

        // Sphere with inward-facing surface: Cull FRONT so we see the backfaces from inside.
        this.sphere = new Sphere(radius, divisions);
        sphere.setCullFace(CullFace.FRONT);
        sphere.setDrawMode(DrawMode.FILL);
        sphere.setMouseTransparent(true);
        sphere.setDepthTest(DepthTest.ENABLE); // background should pass depth correctly

        // Material: always visible via local ambient light (scoped to just this sphere)
        material.setDiffuseMap(texture);
        material.setSpecularColor(Color.BLACK); // no spec highlights
        sphere.setMaterial(material);

        // Local ambient light so the texture is visible regardless of the scene lights
        AmbientLight skyLight = new AmbientLight(Color.WHITE);
        skyLight.getScope().add(sphere); // only lights the sky sphere

        // Follow the camera position (no rotation)
        getTransforms().add(follow);
        getChildren().addAll(sphere, skyLight);
        startFollowing();
    }

    public SphericalSkybox(Image texture, double radius, PerspectiveCamera camera) {
        this(texture, radius, camera, 64);
    }

    /** Change the sky texture at runtime. */
    public void setTexture(Image texture) {
        material.setDiffuseMap(texture);
    }

    /** Change radius (must remain < camera farClip). */
    public void setRadius(double r) {
        sphere.setRadius(r);
    }

    public double getRadius() {
        return sphere.getRadius();
    }

    /** Stop the internal follow timer (call when tearing down). */
    public void stop() {
        if (timer != null) timer.stop();
    }

    // --- follow camera position only ---
    private void startFollowing() {
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                Transform ct = (camera != null) ? camera.getLocalToSceneTransform() : null;
                if (ct != null) {
                    follow.setTx(ct.getTx());
                    follow.setTy(ct.getTy());
                    follow.setTz(ct.getTz());
                }
            }
        };
        timer.start();
    }
}
