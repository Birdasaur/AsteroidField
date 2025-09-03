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

public class TetherSystem {

    private final SubScene subScene;
    private final PerspectiveCamera camera;
    private final Group worldRoot3D;
    private final Supplier<List<Node>> collidablesSupplier;
    private final Tether.SpacecraftAdapter craft;

    private final Tether[] tethers;
    private boolean enabled = false;
    private final boolean hasKinematic;

    // Per-tether local emitter offsets (camera local axes)
    private final Point3D[] emitterOffsetLocal = new Point3D[] {
            Point3D.ZERO, Point3D.ZERO
    };

    // Fixed-step physics (substep in timer)
    private final double fixedDt = 1.0 / 120.0; // 120 Hz
    private final double maxAccumulator = 0.25;
    private double accumulator = 0;
    private long lastNs = -1;

    private final AnimationTimer timer = new AnimationTimer() {
        @Override public void handle(long now) {
            if (!enabled) { lastNs = now; return; }
            if (lastNs < 0) { lastNs = now; return; }
            double dt = (now - lastNs) / 1_000_000_000.0;
            lastNs = now;

            accumulator = Math.min(accumulator + dt, maxAccumulator);
            while (accumulator >= fixedDt) {
                for (Tether t : tethers) t.update(fixedDt);
                if (hasKinematic) ((KinematicCraft) craft).tick(fixedDt);
                accumulator -= fixedDt;
            }
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

        // Small forward clearance from camera to avoid near-plane clipping
        for (Tether t : tethers) t.setViewStartOffset(0.5);

        this.hasKinematic = (craft instanceof KinematicCraft);

        installInputHandlers();
        subScene.setFocusTraversable(true);
        subScene.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> subScene.requestFocus());
        timer.start();
    }

public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    if (!enabled) {
        releaseAll();                    // <— detach when turning off
    }
    for (Tether t : tethers) t.setVisible(enabled);
    if (enabled) { accumulator = 0; lastNs = -1; }
}

    public boolean isEnabled() { return enabled; }
    public void releaseAll() { for (Tether t : tethers) t.release(); }

    // Debug marker helper
    public void setMarkerVisibilityAll(boolean showStart, boolean showEnd) {
        for (Tether t : tethers) { t.setShowStartMarker(showStart); t.setShowEndMarker(showEnd); }
    }

    public Tether getTether(int i) { return (i>=0 && i<tethers.length) ? tethers[i] : null; }

    /** Local offset for one tether (camera/craft local axes: +X=right, +Y=up, +Z=forward). */
    public void setEmitterOffsetLocal(int idx, Point3D localOffset) {
        if (idx < 0 || idx >= emitterOffsetLocal.length) return;
        emitterOffsetLocal[idx] = (localOffset != null) ? localOffset : Point3D.ZERO;
    }

    /** Convenience: set both offsets. */
    public void setEmitterOffsetsLocal(Point3D... offsets) {
        if (offsets == null) return;
        if (offsets.length >= 1) setEmitterOffsetLocal(0, offsets[0]);
        if (offsets.length >= 2) setEmitterOffsetLocal(1, offsets[1]);
    }

    /** Symmetric wing emitters: +/-X with common Y,Z. */
    public void setSymmetricWingOffsets(double lateralX, double localY, double localZ) {
        setEmitterOffsetsLocal(
                new Point3D(-Math.abs(lateralX), localY, localZ),
                new Point3D(+Math.abs(lateralX), localY, localZ)
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
            if (e.getCode() == KeyCode.CONTROL) { releaseAll(); }
        });

        subScene.addEventFilter(KeyEvent.KEY_RELEASED, e -> {
            if (!enabled) return;
            if (e.getCode() == KeyCode.SHIFT) {
                for (Tether t : tethers) t.setPulling(false);
            }
        });
    }

    // Fire with scene->parent conversion and per-tether emitter offset
    private void fireTether(int idx, MouseEvent e) {
        if (idx < 0 || idx >= tethers.length) return;

        // 1) Ray in SCENE space
        var rayScene = RayUtil.computePickRay(camera, subScene, e.getX(), e.getY());

        // 2) Convert ray to parent/world space
        Point3D originParent = worldRoot3D.sceneToLocal(rayScene.origin);
        Point3D p1Parent     = worldRoot3D.sceneToLocal(rayScene.origin.add(rayScene.dir));
        Point3D dirParent    = p1Parent.subtract(originParent).normalize();

        // 3) Start = craft position + emitter local offset (converted)
        Point3D base = craft.getWorldPosition();
        Point3D start = base.add(localOffsetToParent(emitterOffsetLocal[idx]));

        // 4) Fire
        tethers[idx].fireFrom(start, dirParent);
    }

    // Convert a camera-local vector to parent/world space
    private Point3D localOffsetToParent(Point3D local) {
        if (local == null || local.magnitude() == 0) return Point3D.ZERO;

        Affine camToScene = new Affine(camera.getLocalToSceneTransform());
        Point3D originScene = camToScene.transform(Point3D.ZERO);
        Point3D rightScene  = camToScene.transform(new Point3D(1, 0, 0));
        Point3D upScene     = camToScene.transform(new Point3D(0, 1, 0));
        Point3D fwdScene    = camToScene.transform(new Point3D(0, 0, 1));

        Point3D originParent = worldRoot3D.sceneToLocal(originScene);
        Point3D rightParent  = worldRoot3D.sceneToLocal(rightScene).subtract(originParent).normalize();
        Point3D upParent     = worldRoot3D.sceneToLocal(upScene).subtract(originParent).normalize();
        Point3D fwdParent    = worldRoot3D.sceneToLocal(fwdScene).subtract(originParent).normalize();

        return rightParent.multiply(local.getX())
                .add(upParent.multiply(local.getY()))
                .add(fwdParent.multiply(local.getZ()));
    }
}
