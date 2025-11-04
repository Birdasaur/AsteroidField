package AsteroidField.ui.overlay;

import AsteroidField.events.ApplicationEvent; // use your existing app event type
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;

/**
 * Minimal overlay controller that shows/hides a single text console overlay
 * in the provided overlay root. Uses a placeholder console for now to keep
 * the step compile-safe. Later we’ll swap in your TextPane/LitPathPane.
 */
public final class OverlayController {
    private final Scene scene;
    private final Pane overlayRoot;

    // Temporary stand-in for your LitPathPane-based TextPane
    private PlaceholderTextConsole textConsolePane;

    public OverlayController(Scene scene, Pane overlayRoot) {
        this.scene = scene;
        this.overlayRoot = overlayRoot;
        installHandlers();
    }

    private void installHandlers() {
        scene.addEventHandler(ApplicationEvent.SHOW_TEXT_CONSOLE, e -> {
            boolean streaming = e.object2 != null && (boolean) e.object2; // true = append
            ensureConsole();
            if (!overlayRoot.getChildren().contains(textConsolePane)) {
                overlayRoot.getChildren().add(textConsolePane);
                textConsolePane.slideIn(); // simple visual cue
            } else {
                if (!streaming) {
                    textConsolePane.setVisible(true);
                }
            }
            if (e.object instanceof String s) {
                if (streaming) {
                    Platform.runLater(() -> textConsolePane.appendText(s));
                } else {
                    Platform.runLater(() -> textConsolePane.setText(s));
                }
            }
            e.consume();
        });

        scene.addEventHandler(ApplicationEvent.CLOSE_TEXT_CONSOLE, e -> {
            if (textConsolePane != null) {
                overlayRoot.getChildren().remove(textConsolePane);
            }
            e.consume();
        });
    }

    private void ensureConsole() {
        if (textConsolePane == null) {
            textConsolePane = new PlaceholderTextConsole();
            textConsolePane.setPickOnBounds(true);
        }
    }
}
