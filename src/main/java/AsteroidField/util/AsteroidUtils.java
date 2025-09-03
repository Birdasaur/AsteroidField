package AsteroidField.util;

import AsteroidField.asteroids.AsteroidFamilyUI;
import AsteroidField.asteroids.AsteroidGenerator;
import AsteroidField.asteroids.parameters.AsteroidParameters;
import AsteroidField.asteroids.providers.AsteroidMeshProvider;
import javafx.beans.binding.Bindings;
import javafx.geometry.Point3D;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

/** Grab-bag helper: pose binding, wireframe overlays, and asteroid creation. */
public final class AsteroidUtils {
    private AsteroidUtils() {}

    public static AsteroidParameters buildParamsForFamily(String family, AsteroidParameters oldParams) {
        AsteroidMeshProvider provider = AsteroidMeshProvider.PROVIDERS.get(family);
        if (provider instanceof AsteroidFamilyUI uiProvider) {
            return uiProvider.buildDefaultParamsFrom(oldParams);
        }
        return new AsteroidParameters.Builder<>()
                .radius(oldParams.getRadius())
                .subdivisions(oldParams.getSubdivisions())
                .deformation(oldParams.getDeformation())
                .seed(oldParams.getSeed())
                .familyName(family)
                .build();
    }    
    // ---------- Pose binding ----------

    /** Bind all pose properties so target fully mirrors source (translate/rotate/scale + transforms list). */
    public static void bindPose(Node target, Node source) {
        // Convenience properties
        target.translateXProperty().bind(source.translateXProperty());
        target.translateYProperty().bind(source.translateYProperty());
        target.translateZProperty().bind(source.translateZProperty());

        target.rotateProperty().bind(source.rotateProperty());
        target.rotationAxisProperty().bind(source.rotationAxisProperty());

        target.scaleXProperty().bind(source.scaleXProperty());
        target.scaleYProperty().bind(source.scaleYProperty());
        target.scaleZProperty().bind(source.scaleZProperty());

        // Transform list (Affine/Rotate/Translate in getTransforms())
        Bindings.bindContent(target.getTransforms(), source.getTransforms());
    }

    /** Unbind everything bound by {@link #bindPose(Node, Node)}. */
    public static void unbindPose(Node target, Node source) {
        target.translateXProperty().unbind();
        target.translateYProperty().unbind();
        target.translateZProperty().unbind();

        target.rotateProperty().unbind();
        target.rotationAxisProperty().unbind();

        target.scaleXProperty().unbind();
        target.scaleYProperty().unbind();
        target.scaleZProperty().unbind();

        Bindings.unbindContent(target.getTransforms(), source.getTransforms());
    }

    // ---------- Wireframe overlays ----------

    /** Create a LINE-mode wireframe that mirrors a source MeshView. */
    public static MeshView createWireframeFor(MeshView src, Color lineColor, boolean visible) {
        MeshView wire = new MeshView(src.getMesh());
        wire.setMaterial(new PhongMaterial(lineColor != null ? lineColor : Color.ALICEBLUE));
        wire.setDrawMode(DrawMode.LINE);
        wire.setMouseTransparent(true);
        wire.setCullFace(CullFace.NONE);
        bindPose(wire, src);          // keep co-located with the source
        wire.setVisible(visible);
        return wire;
    }

    // ---------- Asteroid builders ----------

    /** Build a filled MeshView for the given parameters. */
    public static MeshView createAsteroid(AsteroidParameters p) {
        AsteroidMeshProvider provider = AsteroidMeshProvider.PROVIDERS.get(p.getFamilyName());
        if (provider == null) provider = new AsteroidMeshProvider.Default();
        TriangleMesh mesh = new AsteroidGenerator(provider, p).generateAsteroid();

        MeshView mv = new MeshView(mesh);
        mv.setMaterial(new PhongMaterial(Color.GAINSBORO));
        mv.setDrawMode(DrawMode.FILL);
        mv.setCullFace(CullFace.NONE);
        return mv;
    }

    /** Build a filled MeshView and place it at the given world position. */
    public static MeshView createAsteroidAt(AsteroidParameters p, Point3D pos) {
        MeshView mv = createAsteroid(p);
        if (pos != null) {
            mv.setTranslateX(pos.getX());
            mv.setTranslateY(pos.getY());
            mv.setTranslateZ(pos.getZ());
        }
        return mv;
    }
}
