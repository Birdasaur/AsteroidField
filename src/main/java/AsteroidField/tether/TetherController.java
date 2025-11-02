package AsteroidField.tether;

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

/** Owns tethers + input. Stepped by PhysicsSystem via PhysicsContributor.step(dt). */
public final class TetherController implements PhysicsContributor {

    private final SubScene subScene;
    private final PerspectiveCamera camera;
    private final Group worldRoot3D;
    private final Supplier<List<Node>> collidablesSupplier;
    private final Tether.SpacecraftAdapter craft;

    private final Tether[] tethers;

    // input gating (what your toggle flips)
    private boolean tetherInputEnabled = false;

    // per-tether emitter offsets (camera-local axes)
    private final Point3D[] emitterOffsetLocal = new Point3D[] { Point3D.ZERO, Point3D.ZERO };

    public TetherController(SubScene subScene,
                            PerspectiveCamera camera,
                            Group worldRoot3D,
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
        for (Tether t : tethers) {
            t.setViewStartOffset(0.5);
            t.setRayFrontFaceOnly(true);
            t.setAllowAabbFallbackOnMeshMiss(false);
        }

        installInputHandlers();
        subScene.setFocusTraversable(true);
        subScene.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> subScene.requestFocus());
        setVisible(false); // start hidden until armed
    }

    // ---- PhysicsContributor ----
    @Override
    public void step(double dt) {
        // update tethers even if input is disabled (so attached ones keep acting), OR gate this if you prefer:
        for (Tether t : tethers) t.update(dt);
    }

    // ---- API you already use from the view ----
    public void setTetherInputEnabled(boolean enabled) {
        this.tetherInputEnabled = enabled;
        if (!enabled) releaseAll();
        setVisible(enabled);
    }
    public boolean isTetherInputEnabled() { return tetherInputEnabled; }

    public void setSymmetricWingOffsets(double lateralX, double localY, double localZ) {
        setEmitterOffsetsLocal(
                new Point3D(-Math.abs(lateralX), localY, localZ),
                new Point3D(+Math.abs(lateralX), localY, localZ));
    }
    public void setEmitterOffsetsLocal(Point3D... offsets) {
        if (offsets == null) return;
        if (offsets.length >= 1) emitterOffsetLocal[0] = (offsets[0] != null) ? offsets[0] : Point3D.ZERO;
        if (offsets.length >= 2) emitterOffsetLocal[1] = (offsets[1] != null) ? offsets[1] : Point3D.ZERO;
    }

    public void releaseAll() { for (Tether t : tethers) t.release(); }
    public Tether getTether(int i) { return (i>=0 && i<tethers.length) ? tethers[i] : null; }

    public void setVisible(boolean v) { for (Tether t : tethers) t.setVisible(v); }

    // ---- Input handlers (unchanged from your TetherSystem, but gated by tetherInputEnabled) ----
    private void installInputHandlers() {
        subScene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (!tetherInputEnabled) return;
            if (e.getButton() == MouseButton.PRIMARY) fireTether(0, e);
            else if (e.getButton() == MouseButton.SECONDARY) fireTether(1, e);
        });

        subScene.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (!tetherInputEnabled) return;
            if (e.getCode() == KeyCode.SHIFT)   { for (Tether t : tethers) t.setPulling(true); }
            if (e.getCode() == KeyCode.CONTROL) { releaseAll(); }
        });

        subScene.addEventFilter(KeyEvent.KEY_RELEASED, e -> {
            if (!tetherInputEnabled) return;
            if (e.getCode() == KeyCode.SHIFT)   { for (Tether t : tethers) t.setPulling(false); }
        });
    }

    private void fireTether(int idx, MouseEvent e) {
        if (idx < 0 || idx >= tethers.length) return;

        var rayScene = RayUtil.computePickRay(camera, subScene, e.getX(), e.getY());

        Point3D originParent = worldRoot3D.sceneToLocal(rayScene.origin);
        Point3D p1Parent     = worldRoot3D.sceneToLocal(rayScene.origin.add(rayScene.dir));
        Point3D dirParent    = p1Parent.subtract(originParent);
        if (dirParent.magnitude() < 1e-9) return;
        dirParent = dirParent.normalize();

        Point3D base  = craft.getWorldPosition();
        Point3D start = base.add(localOffsetToParent(emitterOffsetLocal[idx]));

        tethers[idx].fireFrom(start, dirParent);
    }

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
