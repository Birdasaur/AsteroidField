package AsteroidField;

import AsteroidField.asteroids.AsteroidLodManager;
import AsteroidField.asteroids.field.families.FamilyPool;
import AsteroidField.asteroids.field.families.WeightedFamilyEntry;
import AsteroidField.asteroids.field.placement.BeltPlacementStrategy;
import AsteroidField.asteroids.field.placement.PlacementStrategy;
import AsteroidField.asteroids.providers.AsteroidMeshProvider;
import AsteroidField.audio.SfxPlayer;
import AsteroidField.events.AudioEvent;
import AsteroidField.events.SfxEvent;
import AsteroidField.runtime.DockingController;
import AsteroidField.runtime.DockingModeController;
import AsteroidField.runtime.WorldBuilder;
import AsteroidField.spacecraft.FancyCraft;
import AsteroidField.ui.overlay.OverlayController;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import static javafx.scene.input.KeyCode.F10;
import static javafx.scene.input.KeyCode.F9;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Playable-demo shell. Uses Game3DView for the 3D world and an overlay
 * layer managed by OverlayController. DockingController (E/U) drives
 * the terminal overlay events; DockingModeController reacts with
 * physics pause/resume, craft visibility, and audio cues.
 */
public class GameApp extends Application {

    OverlayController overlayController;
    DockingController dockingController;
    DockingModeController dockingModeController;
    SfxPlayer sfx;
    FancyCraft fancyCraft;
    WorldBuilder worldBuilder;
    WorldBuilder.Handle fieldHandle;
    FamilyPool familyPool;
    PlacementStrategy placement;
    AsteroidLodManager lodManager;

    @Override
    public void start(Stage stage) {
        // --- Core layout ---
        BorderPane root = new BorderPane();
        Scene scene = new Scene(root, 1600, 900, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.BLACK);

        // --- Center stack: 3D + overlays ---
        Game3DView gameView = new Game3DView();
        // --- WorldBuilder demo: families + placement ---
        worldBuilder = new WorldBuilder(gameView);

        // Build weighted entries from all registered providers (defaults: enabled=true, weight=1.0)
        List<WeightedFamilyEntry> entries = new ArrayList<>();
        AsteroidMeshProvider.PROVIDERS.values().forEach(p -> {
            WeightedFamilyEntry e = new WeightedFamilyEntry(p);
            // Optional explicitness:
            // e.enabledProperty().set(true);
            // e.weightProperty().set(1.0);
            entries.add(e);
        });
        familyPool = new FamilyPool(entries);

        // Placement: belt with its built-in defaults (no public setters)
        BeltPlacementStrategy belt = new BeltPlacementStrategy();
        //belt.setThicknessSigma(0);
        placement = belt;

        // --- LOD Manager ---
        lodManager = new AsteroidLodManager(gameView.getCamera());
        // Tune distances to your ~8k belt (adjust as you like)
        lodManager.setDistances(1500, 3500, 6000, 400); // near, mid, far, hysteresis
        lodManager.setBudgetPerFrame(120);
        // Optional: gate swaps to a 70° half-cone in front of the camera
        lodManager.setForwardConeDegrees(70);
        // Start its internal timer now; it won’t do anything until you register a field
        lodManager.start();
        
        // TEMP craft proxy so you can see hide/show immediately.
        // Remove once your real craft rig is wired up.
        fancyCraft = new FancyCraft();
        fancyCraft.setTranslateZ(-200); // in front of camera a bit
        gameView.setCraftProxy(fancyCraft);

        Pane overlayDesktop = new Pane();
        overlayDesktop.setPickOnBounds(false);  // clicks pass through empty areas

        StackPane centerStack = new StackPane(gameView, overlayDesktop);
        StackPane.setAlignment(overlayDesktop, Pos.TOP_LEFT);
        StackPane.setMargin(overlayDesktop, new Insets(0));

        root.setCenter(centerStack);

        // --- Overlay controller (listens for ApplicationEvent.* on the Scene) ---
        overlayController = new OverlayController(scene, overlayDesktop);

        // --- Input: Docking controller (E/U to open/close terminal overlay) ---
        dockingController = new DockingController(scene, gameView.getSubScene());

        // --- Audio: SFX (one-shots) ---
        sfx = new SfxPlayer(scene);
        sfx.setMasterVolume(0.8);
        // listen for all SfxEvent.* fired anywhere in the scene graph
        scene.addEventHandler(SfxEvent.ANY, sfx);
        // preload a directory (filesystem) so first-play is instant
        sfx.preloadDirectory("sfx/");

        // register a few aliases so the rest of your code isn’t tied to filenames
        scene.getRoot().fireEvent(new SfxEvent(
            SfxEvent.REGISTER_SFX_ALIAS, "carl-tonight", "sfx/carl-tonight.wav"
        ));
        scene.getRoot().fireEvent(new SfxEvent(
            SfxEvent.REGISTER_SFX_ALIAS, "dock_chime", "sfx/dock_chime.wav"
        ));
        scene.getRoot().fireEvent(new SfxEvent(
            SfxEvent.REGISTER_SFX_ALIAS, "undock_whoosh", "sfx/undock_whoosh.wav"
        ));

        // --- Docking mode hooks: pause/resume physics, hide/show craft, audio cues ---
        final double MUSIC_LEVEL_NORMAL = 0.35;
        final double MUSIC_LEVEL_DOCKED = 0.15;

        dockingModeController = new DockingModeController(
            scene,
            /* onEnterDocked */ () -> {
                // Pause simulation and hide craft proxy
                gameView.pausePhysics();
                gameView.setCraftProxyVisible(false);

                // Duck music (safe if JukeBox not installed—no listener means no-op)
                scene.getRoot().fireEvent(new AudioEvent(
                    AudioEvent.SET_MUSIC_VOLUME, MUSIC_LEVEL_DOCKED
                ));

                // docking chime (using alias)
                scene.getRoot().fireEvent(new SfxEvent(
                    SfxEvent.PLAY_SFX, "carl-tonight" // or "dock_chime"
                ));
            },
            /* onExitDocked */ () -> {
                // Resume simulation and show craft proxy
                gameView.resumePhysics();
                gameView.setCraftProxyVisible(true);

                // Restore music
                scene.getRoot().fireEvent(new AudioEvent(
                    AudioEvent.SET_MUSIC_VOLUME, MUSIC_LEVEL_NORMAL
                ));

                // Optional: undock sound
                scene.getRoot().fireEvent(new SfxEvent(
                    SfxEvent.PLAY_SFX, "undock_whoosh"
                ));
            }
        );
// Toggle FPS look capture with F1; toggle tether input with F2'
scene.setOnKeyPressed(e -> {
    switch (e.getCode()) {
        case F1 -> {
            boolean on = !gameView.getFpsLook().isEnabled();
            gameView.setFpsLookEnabled(on);
        }
        case F2 -> {
            boolean allow = !gameView.getTethers().isTetherInputEnabled();
            gameView.getTethers().setTetherInputEnabled(allow);
        }
        case F9 -> {
            // Clear any prior field first
            if (fieldHandle != null) { fieldHandle.detach(); fieldHandle = null; }

            // High-count config using our helper
            var cfg = WorldBuilder.defaultHighCountConfig();
            cfg.count = 1000; // higher count requires higher vram; 
            cfg.subdivisionsMax = 3;
            cfg.usePrototypes = true; //prototypes on for performance improvements
            cfg.prototypeCount = 20; //higher prototype count improves variability at cost of performance
            //cfg.baseColor = Color.GRAY;

            fieldHandle = worldBuilder.buildAndAttach(familyPool, placement, cfg);
            System.out.println("Spawned: " + fieldHandle.getField().instances.size() + " asteroids");
            lodManager.clear(); // drop any prior references
            lodManager.registerField(fieldHandle.getField());            
        }
        case F10 -> {
            if (fieldHandle != null) {
                fieldHandle.detach();
                fieldHandle = null;
                System.out.println("Cleared field.");
            }
            // Also clear LOD entries so it stops touching removed meshes
            lodManager.clear();
        }      
        default -> { /* noop */ }
    }
});
        // --- Stage ---
        stage.setTitle("Asteroid Field — Playable Demo");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
