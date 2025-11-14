package AsteroidField.ui.overlay;

import AsteroidField.asteroids.AsteroidLodManager;
import AsteroidField.events.ApplicationEvent;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central overlay hub:
 *  - Manages overlay panes in a single overlay root (lazy-create & cache).
 *  - Holds a tiny service registry so panes can look up runtime services.
 *  - Listens for app-level overlay events (console, LOD pane, etc).
 *
 * Usage:
 *   OverlayController oc = new OverlayController(scene, overlayDesktop);
 *   // Register services (anytime — global stash works before/after construction):
 *   OverlayController.registerGlobalService(AsteroidLodManager.class, lodManager);
 *   // Fire events elsewhere:
 *   scene.getRoot().fireEvent(new ApplicationEvent(ApplicationEvent.SHOW_ASTEROID_LOD));
 *   scene.getRoot().fireEvent(new ApplicationEvent(ApplicationEvent.SHOW_TEXT_CONSOLE, "Hello", false));
 */
public final class OverlayController {

    // --------- Global service stash (available before instance exists) ---------
    private static final Map<Class<?>, Object> GLOBAL_SERVICES = new ConcurrentHashMap<>();

    public static <T> void registerGlobalService(Class<T> type, T instance) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(instance, "instance");
        GLOBAL_SERVICES.put(type, instance);
    }

    public static <T> T getGlobalService(Class<T> type) {
        Objects.requireNonNull(type, "type");
        return type.cast(GLOBAL_SERVICES.get(type));
    }

    // --------- Instance fields ---------
    private final Scene scene;
    private final Pane overlayRoot;

    // Per-instance services (imported from global on construction; you can add more later)
    private final Map<Class<?>, Object> services = new ConcurrentHashMap<>();

    // Pane cache (one instance per key)
    private final Map<String, Node> panes = new HashMap<>();

    public OverlayController(Scene scene, Pane overlayRoot) {
        this.scene = Objects.requireNonNull(scene, "scene");
        this.overlayRoot = Objects.requireNonNull(overlayRoot, "overlayRoot");

        // Import whatever was registered globally so far.
        services.putAll(GLOBAL_SERVICES);

        installHandlers();
    }

    // --------- Public service registry (instance-level) ---------
    public <T> void registerService(Class<T> type, T instance) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(instance, "instance");
        services.put(type, instance);
    }

    public <T> T getService(Class<T> type) {
        Objects.requireNonNull(type, "type");
        Object v = services.get(type);
        if (v == null) v = GLOBAL_SERVICES.get(type); // fallback to global if needed
        return type.cast(v);
    }

    // --------- Pane API ---------
    /** Show (or create+show) a pane keyed by 'key'. The factory is only invoked if missing. */
    public Node showPane(String key, java.util.function.Supplier<? extends Node> factory) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(factory, "factory");

        Node pane = panes.get(key);
        if (pane == null) {
            pane = factory.get();
            panes.put(key, pane);
            Node finalPane = pane;
            Platform.runLater(() -> {
                if (!overlayRoot.getChildren().contains(finalPane)) {
                    overlayRoot.getChildren().add(finalPane);
                }
                bringToFront(finalPane);
                trySlideIn(finalPane);
            });
        } else {
            Node finalPane = pane;
            Platform.runLater(() -> {
                if (!overlayRoot.getChildren().contains(finalPane)) {
                    overlayRoot.getChildren().add(finalPane);
                }
                finalPane.setVisible(true);
                bringToFront(finalPane);
            });
        }
        return pane;
    }

    /** Close (remove and forget) a pane by key. Calls LitPathPane.close() if available. */
    public void closePane(String key) {
        Node pane = panes.remove(key);
        if (pane == null) return;
        Node finalPane = pane;
        Platform.runLater(() -> {
            tryCloseIfLitPathPane(finalPane);
            overlayRoot.getChildren().remove(finalPane);
        });
    }

    private static void bringToFront(Node n) {
        if (n != null) n.toFront();
    }

    // Try to call LitPathPane.slideInPane() if present; fallback to PlaceholderTextConsole.slideIn()
    private static void trySlideIn(Node n) {
        if (n == null) return;
        // Try slideInPane()
        try {
            Method m = n.getClass().getMethod("slideInPane");
            m.setAccessible(true);
            m.invoke(n);
            return;
        } catch (ReflectiveOperationException ignored) { }
        // Try slideIn()
        try {
            Method m = n.getClass().getMethod("slideIn");
            m.setAccessible(true);
            m.invoke(n);
        } catch (ReflectiveOperationException ignoredToo) { }
    }

    // Try to call LitPathPane.close() if available
    private static void tryCloseIfLitPathPane(Node n) {
        if (n == null) return;
        try {
            Method m = n.getClass().getMethod("close");
            m.setAccessible(true);
            m.invoke(n);
        } catch (ReflectiveOperationException ignored) { }
    }

    // --------- Event wiring ---------
    private void installHandlers() {
        // Text console: SHOW / CLOSE
        scene.addEventHandler(ApplicationEvent.SHOW_TEXT_CONSOLE, e -> {
            boolean streaming = e.object2 != null && (boolean) e.object2;
            Node pane = showPane("console", PlaceholderTextConsole::new);

            if (pane instanceof PlaceholderTextConsole console && e.object instanceof String s) {
                if (streaming) {
                    console.appendText(s);
                } else {
                    console.setText(s);
                }
            }
            e.consume();
        });

        scene.addEventHandler(ApplicationEvent.CLOSE_TEXT_CONSOLE, e -> {
            closePane("console");
            e.consume();
        });

        // LOD pane: SHOW / CLOSE
        // Requires ApplicationEvent.SHOW_ASTEROID_LOD and CLOSE_ASTEROID_LOD
        scene.addEventHandler(ApplicationEvent.SHOW_ASTEROID_LOD, e -> {
            AsteroidLodManager lod = getService(AsteroidLodManager.class);
            Node pane = showPane("lod", () -> new AsteroidLodPane(scene, overlayRoot, lod));
            bringToFront(pane);
            e.consume();
        });

        scene.addEventHandler(ApplicationEvent.CLOSE_ASTEROID_LOD, e -> {
            closePane("lod");
            e.consume();
        });
    }
}
