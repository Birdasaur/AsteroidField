package AsteroidField;

import AsteroidField.audio.SfxPlayer;
import AsteroidField.events.AudioEvent;
import AsteroidField.events.SfxEvent;
import AsteroidField.runtime.DockingController;
import AsteroidField.runtime.DockingModeController;
import AsteroidField.spacecraft.FancyCraft;
import AsteroidField.ui.overlay.OverlayController;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
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

    @Override
    public void start(Stage stage) {
        // --- Core layout ---
        BorderPane root = new BorderPane();
        Scene scene = new Scene(root, 1600, 900, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.BLACK);

        // --- Center stack: 3D + overlays ---
        Game3DView gameView = new Game3DView();

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
