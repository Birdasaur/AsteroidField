package AsteroidField.tether;

import javafx.animation.AnimationTimer;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;

import java.util.List;
import java.util.function.Supplier;

/**
 * Manages multiple tethers, input mapping, and per-frame updates.
 * Default controls:
 *   - LMB: fire tether #0
 *   - RMB: fire tether #1
 *   - SHIFT (hold): pull/reel-in
 *   - CTRL: release all
 */
public class TetherSystem {

    private final SubScene subScene;
    private final PerspectiveCamera camera;
    private final Group worldRoot3D;
    private final Supplier<List<Node>> collidablesSupplier;
    private final Tether.SpacecraftAdapter craft;

    private final Tether[] tethers;
    private boolean enabled = false;
    private final boolean hasKinematic;

    private final AnimationTimer timer = new AnimationTimer() {
        long last = -1;
        @Override public void handle(long now) {
            if (!enabled) { last = now; return; }
            if (last < 0) { last = now; return; }
            double dt = (now - last) / 1_000_000_000.0;
            last = now;
            for (Tether t : tethers) t.update(dt);
            if (hasKinematic) ((KinematicCraft) craft).tick(dt);
        }
    };

    public TetherSystem(SubScene subScene, PerspectiveCamera camera, Group worldRoot3D,
                        Supplier<List<Node>> collidablesSupplier,
                        Tether.SpacecraftAdapter craft) {
        this.subScene = subScene;
        this.camera = camera;
        this.worldRoot3D = worldRoot3D;
        this.collidablesSupplier = collidablesSupplier;
        this.craft = craft;

        this.tethers = new Tether[]{
                new Tether(0, worldRoot3D, collidablesSupplier, craft, Color.CYAN),
                new Tether(1, worldRoot3D, collidablesSupplier, craft, Color.ORANGE)
        };

        this.hasKinematic = (craft instanceof KinematicCraft);

        installInputHandlers();
        subScene.setFocusTraversable(true);
        subScene.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> subScene.requestFocus());
        timer.start();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        for (Tether t : tethers) t.setVisible(enabled);
    }

    public boolean isEnabled() { return enabled; }

    public void releaseAll() {
        for (Tether t : tethers) t.release();
    }

    private void installInputHandlers() {
        subScene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (!enabled) return;
            if (e.getButton() == MouseButton.PRIMARY) fireTether(0, e);
            else if (e.getButton() == MouseButton.SECONDARY) fireTether(1, e);
        });

        subScene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (!enabled) return;
            if (e.getCode() == KeyCode.SHIFT) {
                for (Tether t : tethers) t.setPulling(true);
            }
            if (e.getCode() == KeyCode.CONTROL) {
                releaseAll();
            }
        });

        subScene.addEventFilter(KeyEvent.KEY_RELEASED, e -> {
            if (!enabled) return;
            if (e.getCode() == KeyCode.SHIFT) {
                for (Tether t : tethers) t.setPulling(false);
            }
        });
    }

// Replace your existing fireTether with this version
private void fireTether(int idx, MouseEvent e) {
    if (idx < 0 || idx >= tethers.length) return;

    // 1) Pick ray in SCENE space
    var rayScene = RayUtil.computePickRay(camera, subScene, e.getX(), e.getY());

    // 2) Convert the ray to the world root's PARENT space (the space Tether uses)
    javafx.geometry.Point3D originParent = worldRoot3D.sceneToLocal(rayScene.origin);
    javafx.geometry.Point3D p1Parent     = worldRoot3D.sceneToLocal(rayScene.origin.add(rayScene.dir));
    javafx.geometry.Point3D dirParent    = p1Parent.subtract(originParent);
    if (dirParent.magnitude() < 1e-9) return;
    dirParent = dirParent.normalize();

    // 3) Start point in parent space
    javafx.geometry.Point3D start = craft.getWorldPosition();

    // 4) Fire
    tethers[idx].fireFrom(start, dirParent);
}

    // Convenience for debugging
    public Tether getTether(int index) { return tethers[index]; }
}