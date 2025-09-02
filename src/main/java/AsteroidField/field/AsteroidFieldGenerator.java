package AsteroidField.field;

import AsteroidField.asteroids.parameters.AsteroidParameters;
import AsteroidField.asteroids.providers.AsteroidMeshProvider;
import AsteroidField.field.families.FamilyPool;
import AsteroidField.field.families.FamilySupport;
import AsteroidField.field.placement.PlacementStrategy;
import AsteroidField.field.placement.PlacementStrategy.Placement;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;

/**
 * Synchronous field generator. Call build(...) on a background thread for heavy scenes,
 * then attach the returned field.root to your scene on the FX thread.
 */
public final class AsteroidFieldGenerator {

    /** Configuration for a single field generation run. */
    public static final class Config {
        public int count = 300;
        public long seed = 12345L;

        public double radiusMin = 60, radiusMax = 160;
        public int subdivisionsMin = 1, subdivisionsMax = 3;
        public double deformationMin = 0.15, deformationMax = 0.45;

        public boolean usePrototypes = true;
        public int prototypeCount = 60;

        public Color baseColor = Color.DARKGRAY;
    }

    private final FamilyPool families;
    private final PlacementStrategy placement;

    public AsteroidFieldGenerator(FamilyPool families, PlacementStrategy placement) {
        this.families = families;
        this.placement = placement;
    }

    /** Build the field synchronously. Prefer running on a worker thread. */
    public AsteroidField build(Config cfg) {
        Random rng = new Random(cfg.seed);
        List<Placement> places = placement.generate(cfg.count, rng);
        List<AsteroidInstance> instances = new ArrayList<>(cfg.count);

        // Optional prototype meshes for speed
        List<TriangleMesh> prototypes = new ArrayList<>();
        List<AsteroidParameters> protoParams = new ArrayList<>();
        List<String> protoFamilies = new ArrayList<>();
        if (cfg.usePrototypes) {
            int pc = Math.min(cfg.prototypeCount, cfg.count);
            for (int i = 0; i < pc; i++) {
                AsteroidMeshProvider picked = families.pick(rng);
                AsteroidParameters p = FamilySupport.createParams(
                        picked, rng,
                        lerp(cfg.radiusMin, cfg.radiusMax, rng.nextDouble()),
                        randInt(rng, cfg.subdivisionsMin, cfg.subdivisionsMax),
                        lerp(cfg.deformationMin, cfg.deformationMax, rng.nextDouble())
                );
                TriangleMesh m = picked.generateMesh(p);
                prototypes.add(m);
                protoParams.add(p);
                protoFamilies.add(picked.getDisplayName());
            }
        }

        PhongMaterial sharedMat = new PhongMaterial(cfg.baseColor);

        for (int i = 0; i < cfg.count; i++) {
            Placement pl = places.get(i);
            TriangleMesh mesh;
            AsteroidParameters params;
            String family;

            if (cfg.usePrototypes && !prototypes.isEmpty()) {
                int idx = i % prototypes.size();
                mesh = prototypes.get(idx);
                params = protoParams.get(idx);
                family = protoFamilies.get(idx);
            } else {
                AsteroidMeshProvider picked = families.pick(rng);
                family = picked.getDisplayName();
                params = FamilySupport.createParams(
                        picked, rng,
                        lerp(cfg.radiusMin, cfg.radiusMax, rng.nextDouble()),
                        randInt(rng, cfg.subdivisionsMin, cfg.subdivisionsMax),
                        lerp(cfg.deformationMin, cfg.deformationMax, rng.nextDouble())
                );
                mesh = picked.generateMesh(params);
            }

            MeshView mv = new MeshView(mesh);
            mv.setCullFace(CullFace.BACK);
            mv.setMaterial(sharedMat); // tweak per-instance if desired

            // Placement
            Point3D pos = pl.getPosition();
            mv.setTranslateX(pos.getX());
            mv.setTranslateY(pos.getY());
            mv.setTranslateZ(pos.getZ());
            applyOrientation(mv, pl.getForward(), pl.getUp());
            mv.setScaleX(pl.getBaseScale());
            mv.setScaleY(pl.getBaseScale());
            mv.setScaleZ(pl.getBaseScale());

            double approxR = estimateBoundingRadius(mesh) * pl.getBaseScale();
            instances.add(new AsteroidInstance(mv, family, params, approxR, pos));
        }

        return new AsteroidField(instances);
    }

    // --- utilities ---

    private static int randInt(Random rng, int a, int bInclusive) {
        if (a >= bInclusive) return a;
        return a + rng.nextInt(bInclusive - a + 1);
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    private static double estimateBoundingRadius(TriangleMesh tm) {
        javafx.collections.ObservableFloatArray pts = tm.getPoints();
        double maxR2 = 0.0;
        for (int i = 0; i < pts.size(); i += 3) {
            float x = pts.get(i), y = pts.get(i + 1), z = pts.get(i + 2);
            double r2 = x * x + y * y + z * z;
            if (r2 > maxR2) maxR2 = r2;
        }
        return Math.sqrt(maxR2);
    }

    /** Simple yaw/pitch from forward vector, assuming up ≈ +Z. */
    private static void applyOrientation(MeshView n, Point3D forward, Point3D up) {
        Point3D f = forward.normalize();
        double yaw = Math.toDegrees(Math.atan2(f.getX(), f.getZ()));   // rotate around Y
        double pitch = Math.toDegrees(Math.asin(-f.getY()));           // rotate around X
        n.getTransforms().addAll(new Rotate(yaw, Rotate.Y_AXIS), new Rotate(pitch, Rotate.X_AXIS));
    }

    /** Utility to add the field Group on the FX thread. */
    public static void attachToSceneOnFx(AsteroidField field, javafx.scene.Group parent) {
        Platform.runLater(() -> parent.getChildren().add(field.root));
    }
}
