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
import javafx.scene.transform.Affine;

import java.util.List;
import java.util.function.Supplier;

/**
 * Manages multiple tethers, input mapping, and per-frame updates.
 * Default controls:
 *   - LMB: fire tether #0
 *   - RMB: fire tether #1
 *   - SHIFT (hold): pull/reel-in
 *   - CTRL: release all
 *
 * NEW: per-tether local start offsets (in craft/camera local axes: +X=right, +Y=up, +Z=forward).
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

    // Per-tether local offsets (craft/camera local space). Defaults to zero (no offset).
    // You can set these via setEmitterOffsetLocal()/setEmitterOffsetsLocal().
    private final Point3D[] emitterOffsetLocal = new Point3D[] {
            Point3D.ZERO, Point3D.ZERO
    };

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

        this.tethers = new Tether[] {
                new Tether(0, worldRoot3D, collidablesSupplier, craft, Color.CYAN),
                new Tether(1, worldRoot3D, collidablesSupplier, craft, Color.ORANGE)
        };

        // Optional: tiny forward clearance so the rope base doesn't clip the near plane
//        for (Tether t : tethers) t.setViewStartOffset(0.5);

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

    /** Set a local start offset for a given tether (idx: 0 or 1). Units are world units, axes are camera/craft local. */
    public void setEmitterOffsetLocal(int idx, Point3D localOffset) {
        if (idx < 0 || idx >= emitterOffsetLocal.length) return;
        emitterOffsetLocal[idx] = (localOffset != null) ? localOffset : Point3D.ZERO;
    }

    /** Convenience: set both offsets at once (array length 1 or 2 accepted). */
    public void setEmitterOffsetsLocal(Point3D... offsets) {
        if (offsets == null) return;
        if (offsets.length >= 1) setEmitterOffsetLocal(0, offsets[0]);
        if (offsets.length >= 2) setEmitterOffsetLocal(1, offsets[1]);
    }

    /** Convenience: symmetric "wings": left/right about X, with common Y,Z. */
    public void setSymmetricWingOffsets(double lateralX, double downY, double forwardZ) {
        setEmitterOffsetsLocal(
                new Point3D(-Math.abs(lateralX), downY, forwardZ),
                new Point3D(+Math.abs(lateralX), downY, forwardZ)
        );
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

    // ---- Core: fire with scene->parent ray conversion AND per-tether local emitter offset ----
    private void fireTether(int idx, MouseEvent e) {
        if (idx < 0 || idx >= tethers.length) return;

        // 1) Pick ray in SCENE space
        var rayScene = RayUtil.computePickRay(camera, subScene, e.getX(), e.getY());

        // 2) Convert the ray to the world root's PARENT space (the space Tether uses)
        Point3D originParent = worldRoot3D.sceneToLocal(rayScene.origin);
        Point3D p1Parent     = worldRoot3D.sceneToLocal(rayScene.origin.add(rayScene.dir));
        Point3D dirParent    = p1Parent.subtract(originParent);
        if (dirParent.magnitude() < 1e-9) return;
        dirParent = dirParent.normalize();

        // 3) Compute the start point in PARENT space with a per-tether *local* offset
        //    Local offset is expressed in camera/craft local axes: +X=right, +Y=up, +Z=forward
        Point3D base = craft.getWorldPosition();                // parent space
        Point3D start = base.add(localOffsetToParent(emitterOffsetLocal[idx])); // parent space

        // 4) Fire
        tethers[idx].fireFrom(start, dirParent);
    }

    /**
     * Convert a vector given in camera/craft *local* coordinates to PARENT (world root) space.
     * We derive basis vectors by transforming camera's local unit axes into scene, then into parent, then subtracting origin.
     */
    private Point3D localOffsetToParent(Point3D local) {
        if (local == null || local.magnitude() == 0) return Point3D.ZERO;

        // Camera local -> scene
        Affine camToScene = new Affine(camera.getLocalToSceneTransform());
        Point3D originScene = camToScene.transform(Point3D.ZERO);
        Point3D rightScene  = camToScene.transform(new Point3D(1, 0, 0));
        Point3D upScene     = camToScene.transform(new Point3D(0, 1, 0));
        Point3D fwdScene    = camToScene.transform(new Point3D(0, 0, 1));

        // Scene -> parent
        Point3D originParent = worldRoot3D.sceneToLocal(originScene);
        Point3D rightParent  = worldRoot3D.sceneToLocal(rightScene).subtract(originParent).normalize();
        Point3D upParent     = worldRoot3D.sceneToLocal(upScene).subtract(originParent).normalize();
        Point3D fwdParent    = worldRoot3D.sceneToLocal(fwdScene).subtract(originParent).normalize();

        // Combine
        return rightParent.multiply(local.getX())
                .add(upParent.multiply(local.getY()))
                .add(fwdParent.multiply(local.getZ()));
    }

    public void setShowStartMarker(int idx, boolean show) {
        if (idx >= 0 && idx < tethers.length) tethers[idx].setShowStartMarker(show);
    }
    public void setShowEndMarker(int idx, boolean show) {
        if (idx >= 0 && idx < tethers.length) tethers[idx].setShowEndMarker(show);
    }
    public void setMarkerVisibility(int idx, boolean showStart, boolean showEnd) {
        if (idx >= 0 && idx < tethers.length) tethers[idx].setMarkerVisibility(showStart, showEnd);
    }
    // Convenience for all
    public void setMarkerVisibilityAll(boolean showStart, boolean showEnd) {
        for (Tether t : tethers) t.setMarkerVisibility(showStart, showEnd);
    }    
    // Convenience for debugging
    public Tether getTether(int index) { return tethers[index]; }
}
