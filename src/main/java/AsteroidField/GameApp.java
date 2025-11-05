package AsteroidField;

import AsteroidField.audio.SfxPlayer;
import AsteroidField.events.AudioEvent;
import AsteroidField.events.SfxEvent;
import AsteroidField.runtime.DockingController;
import AsteroidField.runtime.DockingModeController;
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
import javafx.scene.shape.Box;
import javafx.stage.Stage;

/**
 * Playable-demo shell. Uses Game3DView for the 3D world and an overlay
 * layer managed by OverlayController. DockingController (E/U) drives
 * the terminal overlay events for now.
 */
public class GameApp extends Application {

    OverlayController overlayController;
    DockingController dockingController;
    DockingModeController dockingModeController;
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
        Box tempCraft = new Box(40, 20, 60);
        tempCraft.setTranslateZ(-200); // in front of camera a bit
        gameView.setCraftProxy(tempCraft);

        Pane overlayDesktop = new Pane();
        overlayDesktop.setPickOnBounds(false);  // clicks pass through empty areas

        StackPane centerStack = new StackPane(gameView, overlayDesktop);
        StackPane.setAlignment(overlayDesktop, Pos.TOP_LEFT);
        StackPane.setMargin(overlayDesktop, new Insets(0));

        root.setCenter(centerStack);

        // --- Overlay controller (listens for ApplicationEvent.* on the Scene) ---
        overlayController = new OverlayController(scene, overlayDesktop);
        // --- Docking controller (E/U to open/close terminal overlay) ---
        dockingController = new DockingController(scene, gameView.getSubScene());

        // --- Audio: SFX (one-shots) ---
        SfxPlayer sfx = new SfxPlayer(scene);
        sfx.setMasterVolume(0.8);
        // listen for all SfxEvent.* fired anywhere in the scene graph
        scene.addEventHandler(SfxEvent.ANY, sfx);
        // preload a directory (filesystem) so first-play is instant
        sfx.preloadDirectory("sfx/"); 
        
        // --- Docking mode hooks: hide/show craft, duck/restore music, optional SFX ---
        final double MUSIC_LEVEL_NORMAL = 0.35;
        final double MUSIC_LEVEL_DOCKED = 0.15;

        
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
        
        dockingModeController = new DockingModeController(
            scene,
            /* onEnterDocked */ () -> {
                // Hide craft proxy
                gameView.setCraftProxyVisible(false);

                // Duck music (safe if JukeBox not installed—no listener means no-op)
                scene.getRoot().fireEvent(new AudioEvent(
                    AudioEvent.SET_MUSIC_VOLUME, MUSIC_LEVEL_DOCKED
                ));

                // docking chime 
                scene.getRoot().fireEvent(new SfxEvent(
                    SfxEvent.PLAY_SFX, "carl-tonight" //"dock_chime"
                ));
            },
            /* onExitDocked */ () -> {
                // Show craft proxy
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

        // --- Stage ---
        stage.setTitle("Asteroid Field — Playable Demo");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
