package AsteroidField.runtime;

import AsteroidField.events.ApplicationEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;

/**
 * Listens for overlay-driven docking signals and toggles "docked mode".
 * For this step it only calls the provided hooks; you can later pass
 * lambdas to pause physics, hide craft, mute SFX, etc.
 *
 * Enter:  ApplicationEvent.SHOW_TEXT_CONSOLE
 * Exit:   ApplicationEvent.CLOSE_TEXT_CONSOLE
 */
public final class DockingModeController {

    private final Scene scene;
    private final Runnable onEnterDocked;
    private final Runnable onExitDocked;

    private boolean docked = false;

    private final EventHandler<ApplicationEvent> appHandler = this::onAppEvent;

    public DockingModeController(Scene scene,
                                 Runnable onEnterDocked,
                                 Runnable onExitDocked) {
        this.scene = scene;
        this.onEnterDocked = (onEnterDocked != null) ? onEnterDocked : () -> {};
        this.onExitDocked  = (onExitDocked  != null) ? onExitDocked  : () -> {};

        // Listen to all application events; we’ll filter inside.
        scene.addEventHandler(ApplicationEvent.ANY, appHandler);
    }

    private void onAppEvent(ApplicationEvent e) {
        if (e.getEventType() == ApplicationEvent.SHOW_TEXT_CONSOLE) {
            if (!docked) {
                docked = true;
                onEnterDocked.run();
            }
        } else if (e.getEventType() == ApplicationEvent.CLOSE_TEXT_CONSOLE) {
            if (docked) {
                docked = false;
                onExitDocked.run();
            }
        }
    }

    public boolean isDocked() { return docked; }

    public void dispose() {
        scene.removeEventHandler(ApplicationEvent.ANY, appHandler);
    }
}
