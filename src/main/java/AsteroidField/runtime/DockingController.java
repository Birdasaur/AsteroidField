package AsteroidField.runtime;

import AsteroidField.events.ApplicationEvent;
import AsteroidField.util.TinyTags;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

/**
 * Minimal docking controller for MVP wiring:
 *  - Press 'E' to "dock": opens/streams text to the terminal overlay
 *  - Press 'U' to "undock": closes the terminal overlay
 *
 * Note: uses scene.getRoot().fireEvent(...) per JavaFX rules.
 */
public final class DockingController {

    private final Scene scene;
    private final SubScene subScene; // reserved for later (focus capture, etc.)

    private final EventHandler<KeyEvent> keyHandler = this::onKey;

    public DockingController(Scene scene, SubScene subScene) {
        this.scene = scene;
        this.subScene = subScene;
        scene.addEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
    }

    private void onKey(KeyEvent e) {
        if (e.getCode() == KeyCode.E) {
            // Simulate “Docked” → open terminal and stream some lines.
            scene.getRoot().fireEvent(new ApplicationEvent(
                ApplicationEvent.SHOW_TEXT_CONSOLE,
                "=== COLONYNET TERMINAL ===\nDock link established.\n\n",
                Boolean.FALSE // replace text
            ));
            scene.getRoot().fireEvent(new ApplicationEvent(
                ApplicationEvent.SHOW_TEXT_CONSOLE,
                "CONNECTING...\nhandshake OK\nsession: " + TinyTags.sessionTag() + "\n\n", 
                Boolean.TRUE // append
            ));
            scene.getRoot().fireEvent(new ApplicationEvent(
                ApplicationEvent.SHOW_TEXT_CONSOLE,
                "[1] Refinery / Storage\n[2] Communications / Missions\n[3] Upgrades / Recipes\n[0] Undock\n> _\n",
                Boolean.TRUE // append
            ));
            e.consume();
        } else if (e.getCode() == KeyCode.U) {
            scene.getRoot().fireEvent(new ApplicationEvent(ApplicationEvent.CLOSE_TEXT_CONSOLE));
            e.consume();
        }
    }

    public void dispose() {
        scene.removeEventFilter(KeyEvent.KEY_PRESSED, keyHandler);
    }
}
