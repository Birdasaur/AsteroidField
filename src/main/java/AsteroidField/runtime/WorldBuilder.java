package AsteroidField.runtime;

import AsteroidField.Game3DView;
import AsteroidField.asteroids.AsteroidLodManager;
import AsteroidField.asteroids.field.AsteroidField;
import AsteroidField.asteroids.field.AsteroidFieldGenerator;
import AsteroidField.asteroids.field.AsteroidInstance;
import AsteroidField.asteroids.field.families.FamilyPool;
import AsteroidField.asteroids.field.placement.PlacementStrategy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.application.Platform;
import javafx.scene.shape.MeshView;
import lit.litfx.core.utils.Utils;

/**
 * Thin adapter around AsteroidFieldGenerator that:
 *  1) builds a field (synchronously),
 *  2) attaches its root to the Game3DView world,
 *  3) registers each MeshView as a collidable with the runtime (tethers & ship collisions),
 *  4) wires and starts the AsteroidLodManager for this field.
 *
 * Usage (FX thread):
 *   WorldBuilder wb = new WorldBuilder(gameView);
 *   WorldBuilder.Handle h = wb.buildAndAttach(families, placement, defaultHighCountConfig());
 *   // later: h.detach();
 */
public final class WorldBuilder {

    /** Handle to later detach/deregister the field (and stop LOD) as a unit. */
    public static final class Handle {
        private final Game3DView view;
        private final AsteroidField field;
        private final List<MeshView> registered;
        private final AsteroidLodManager lod;

        private Handle(Game3DView view, AsteroidField field, List<MeshView> registered, AsteroidLodManager lod) {
            this.view = view;
            this.field = field;
            this.registered = registered;
            this.lod = lod;
        }

        /** Remove field root from world, deregister collidables, and stop/clear LOD. Must be called on FX thread. */
        public void detach() {
            Runnable r = () -> {
                // stop & clear LOD first to avoid swapping meshes on removed nodes
                if (lod != null) {
                    lod.stop();
                    lod.clear();
                }
                if (field != null && field.root != null) {
                    view.getWorldRoot().getChildren().remove(field.root);
                }
                if (registered != null) {
                    for (MeshView mv : registered) {
                        view.removeCollidable(mv); // no-op if already removed
                    }
                    registered.clear();
                }
            };
            if (Platform.isFxApplicationThread()) r.run(); else Platform.runLater(r);
        }

        public AsteroidField getField() { return field; }
        public AsteroidLodManager getLod() { return lod; }
    }

    private final Game3DView view;

    public WorldBuilder(Game3DView view) {
        this.view = Objects.requireNonNull(view, "view");
    }

    /**
     * Build a field synchronously, attach it to the Game3DView world root, register all meshes as collidables,
     * and start a per-field LOD manager.
     *
     * Call on FX thread for now (generator is synchronous; we attach immediately).
     */
    public Handle buildAndAttach(FamilyPool families,
                                 PlacementStrategy placement,
                                 AsteroidFieldGenerator.Config cfg) {
        Objects.requireNonNull(families, "families");
        Objects.requireNonNull(placement, "placement");
        Objects.requireNonNull(cfg, "cfg");

        // 1) Generate (synchronously)
        System.out.println("Generating " + cfg.count + " asteroids using " + cfg.prototypeCount + " prototypes.");
        long startTime = System.nanoTime();
        AsteroidFieldGenerator gen = new AsteroidFieldGenerator(families, placement);
        AsteroidField field = gen.build(cfg);
        Utils.printTotalTime(startTime);

        // 2) Attach to world
        view.getWorldRoot().getChildren().add(field.root);

        // 3) Register collidables
        List<MeshView> registered = new ArrayList<>(field.instances.size());
        for (AsteroidInstance inst : field.instances) {
            if (inst.node() instanceof MeshView mv) {
                view.addCollidable(mv);
                registered.add(mv);
            }
        }

        // 4) LOD: create, tune, register this field, and start
        AsteroidLodManager lod = new AsteroidLodManager(view.getCamera());
        // Good starting points; tweak or expose via a debug pane later:
        lod.setDistances(/* near */ 1_000, /* mid */ 3_000, /* far */ 6_000, /* hysteresis */ 1_500);
        lod.setBudgetPerFrame(160);           // evaluate ≤160 asteroids/frame
        lod.setForwardConeDegrees(50);        // only swap when roughly in-view
        lod.registerField(field);
        lod.start();

        return new Handle(view, field, registered, lod);
    }

    /**
     * A convenient high-count default for demos and stress tests.
     * Uses prototype reuse for performance while still showing variety.
     */
    public static AsteroidFieldGenerator.Config defaultHighCountConfig() {
        AsteroidFieldGenerator.Config cfg = new AsteroidFieldGenerator.Config();
        cfg.count = 300;                       // push higher as perf allows
        cfg.seed = System.nanoTime();

        // Size/shape variety (tuned for readability + performance)
        cfg.radiusMin = 60.0;
        cfg.radiusMax = 220.0;
        cfg.subdivisionsMin = 1;
        cfg.subdivisionsMax = 3;
        cfg.deformationMin = 0.15;
        cfg.deformationMax = 0.35;

        // Prototype reuse = fewer mesh allocations for 100+ bodies
        cfg.usePrototypes = true;
        cfg.prototypeCount = 48;               // 36–60 is a nice sweet spot

        // Neutral material for now; families can override later
        cfg.baseColor = javafx.scene.paint.Color.DARKGRAY;
        return cfg;
        }
}
