package AsteroidField;

import AsteroidField.asteroids.AsteroidLodManager;
import AsteroidField.asteroids.field.families.FamilyPool;
import AsteroidField.asteroids.field.families.WeightedFamilyEntry;
import AsteroidField.asteroids.field.placement.BeltPlacementStrategy;
import AsteroidField.asteroids.field.placement.PlacementStrategy;
import AsteroidField.asteroids.providers.AsteroidMeshProvider;
import AsteroidField.audio.SfxPlayer;
import AsteroidField.css.StyleResourceProvider;
import AsteroidField.events.ApplicationEvent;
import AsteroidField.events.AsteroidFieldEvent; // event wiring (keep)
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
import javafx.scene.input.KeyCode;
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

    private Scene scene;
    private BorderPane root;

    @Override
    public void start(Stage stage) {
        // --- Core layout ---
        root = new BorderPane();
        scene = new Scene(root, 1600, 900, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.BLACK);

        // --- Center stack: 3D + overlays ---
        Game3DView gameView = new Game3DView();

        // --- WorldBuilder: families + placement ---
        worldBuilder = new WorldBuilder(gameView);

        // Build weighted entries from all registered providers (defaults: enabled=true, weight=1.0)
        List<WeightedFamilyEntry> entries = new ArrayList<>();
        AsteroidMeshProvider.PROVIDERS.values().forEach(p -> entries.add(new WeightedFamilyEntry(p)));
        familyPool = new FamilyPool(entries);

        // Placement: belt with built-in defaults
        BeltPlacementStrategy belt = new BeltPlacementStrategy();
        placement = belt;

        // --- LOD Manager ---
        lodManager = new AsteroidLodManager(gameView.getCamera());
        lodManager.setDistances(1500, 3500, 6000, 400); // near, mid, far, hysteresis
        lodManager.setBudgetPerFrame(120);
        lodManager.setForwardConeDegrees(70);          // optional gating
        lodManager.start();
        
        //REgester all our services
        OverlayController.registerGlobalService(AsteroidLodManager.class, lodManager);
        OverlayController.registerGlobalService(WorldBuilder.class, worldBuilder);
        OverlayController.registerGlobalService(FamilyPool.class, familyPool);
        OverlayController.registerGlobalService(PlacementStrategy.class, placement);

        // REQUIRED: register LOD manager for asteroid-field lifecycle events
        scene.addEventHandler(AsteroidFieldEvent.ANY, lodManager);

        // TEMP craft proxy to visualize quickly
        fancyCraft = new FancyCraft();
        fancyCraft.setTranslateZ(-200);
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
        scene.addEventHandler(SfxEvent.ANY, sfx);
        sfx.preloadDirectory("sfx/");
        scene.getRoot().fireEvent(new SfxEvent(SfxEvent.REGISTER_SFX_ALIAS, "carl-tonight", "sfx/carl-tonight.wav"));
        scene.getRoot().fireEvent(new SfxEvent(SfxEvent.REGISTER_SFX_ALIAS, "dock_chime", "sfx/dock_chime.wav"));
        scene.getRoot().fireEvent(new SfxEvent(SfxEvent.REGISTER_SFX_ALIAS, "undock_whoosh", "sfx/undock_whoosh.wav"));

        // --- Docking mode hooks: pause/resume physics, hide/show craft, audio cues ---
        final double MUSIC_LEVEL_NORMAL = 0.35;
        final double MUSIC_LEVEL_DOCKED = 0.15;

        dockingModeController = new DockingModeController(
            scene,
            /* onEnterDocked */ () -> {
                gameView.pausePhysics();
                gameView.setCraftProxyVisible(false);
                scene.getRoot().fireEvent(new AudioEvent(AudioEvent.SET_MUSIC_VOLUME, MUSIC_LEVEL_DOCKED));
                scene.getRoot().fireEvent(new SfxEvent(SfxEvent.PLAY_SFX, "carl-tonight"));
            },
            /* onExitDocked */ () -> {
                gameView.resumePhysics();
                gameView.setCraftProxyVisible(true);
                scene.getRoot().fireEvent(new AudioEvent(AudioEvent.SET_MUSIC_VOLUME, MUSIC_LEVEL_NORMAL));
                scene.getRoot().fireEvent(new SfxEvent(SfxEvent.PLAY_SFX, "undock_whoosh"));
            }
        );

        // --- Key handling ---
        scene.setOnKeyPressed(e -> {
            KeyCode code = e.getCode();
            switch (code) {
                case F1 -> {
                    boolean on = !gameView.getFpsLook().isEnabled();
                    gameView.setFpsLookEnabled(on);
                }
                case F2 -> {
                    boolean allow = !gameView.getTethers().isTetherInputEnabled();
                    gameView.getTethers().setTetherInputEnabled(allow);
                }
                case F3 -> { // Toggle the Asteroid LOD Pane
                    scene.getRoot().fireEvent(new ApplicationEvent(ApplicationEvent.SHOW_ASTEROID_LOD));
                }
                case F4 -> { // Toggle the Field Debug Pane (WorldBuilder controls)
                    scene.getRoot().fireEvent(new ApplicationEvent(ApplicationEvent.SHOW_FIELD_MANAGER));
                }
                
                case F9 -> {
                    // Clear any prior field first
                    if (fieldHandle != null) {
                        fieldHandle.detach();
                        fieldHandle = null;
                    }

                    // High-count config using our helper
                    var cfg = WorldBuilder.defaultHighCountConfig();
                    cfg.count = 1000;
                    cfg.subdivisionsMax = 3;
                    cfg.usePrototypes = true;
                    cfg.prototypeCount = 20;

                    fieldHandle = worldBuilder.buildAndAttach(familyPool, placement, cfg);
                    System.out.println("Spawned: " + fieldHandle.getField().instances.size() + " asteroids");
                    // No direct LOD/event calls—WorldBuilder now fires ATTACHED.
                }
                case F10 -> {
                    if (fieldHandle != null) {
                        fieldHandle.detach();
                        fieldHandle = null;
                        System.out.println("Cleared field.");
                    }
                    // No direct LOD/event calls—Handle now fires DETACHED.
                }
                default -> { /* noop */ }
            }
        });

        // --- Stage ---
                //Make everything pretty
        String CSS = StyleResourceProvider.getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(CSS);
        CSS = StyleResourceProvider.getResource("covalent.css").toExternalForm();
        scene.getStylesheets().add(CSS);
        stage.setTitle("Asteroid Field — Playable Demo");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() throws Exception {
        // Optional cleanup
        if (lodManager != null) {
            // If you want to explicitly remove handler (not required on app exit):
            // scene.removeEventHandler(AsteroidFieldEvent.ANY, lodManager);
            lodManager.stop();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
