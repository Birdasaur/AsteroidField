package AsteroidField.util;

import javafx.animation.AnimationTimer;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

public class FlyCameraController {
    private final PerspectiveCamera cam;
    private final SubScene sub;

    // state
    private double yawDeg = 0.0;
    private double pitchDeg = 0.0;
    private double px = 0, py = 0, pz = -400;

    // tunables
    private double mouseSensitivity = 0.2;
    private double moveSpeed = 150;
    private double boostMultiplier = 3.0;
    private double scrollSpeed = 1.0;

    // key state
    private boolean kW, kA, kS, kD, kUp, kDown, kShift;

    // mouse
    private double lastMouseX, lastMouseY;
    private boolean dragging = false;

    // timer
    private AnimationTimer timer;
    private long lastNs = 0;
    private boolean enabled = true;

    public FlyCameraController(PerspectiveCamera cam, SubScene sub) {
        this.cam = cam;
        this.sub = sub;
        if (enabled) {
            installHandlers();
            startLoop();
        }
        applyTransform();
    }

    public void setEnabled(boolean on) {
        if (enabled == on) return;
        enabled = on;
        if (on) {
            installHandlers();
            startLoop();
        } else {
            removeHandlers();
            stopLoop();
        }
    }

    public void setSpeed(double unitsPerSec){ this.moveSpeed = unitsPerSec; }
    public void setBoostMultiplier(double m){ this.boostMultiplier = m; }
    public void setSensitivity(double degPerPixel){ this.mouseSensitivity = degPerPixel; }
    public void setScrollSpeed(double s){ this.scrollSpeed = s; }

    private void installHandlers() {
        sub.setOnMousePressed(this::onMousePressed);
        sub.setOnMouseReleased(this::onMouseReleased);
        sub.setOnMouseDragged(this::onMouseDragged);
        sub.setOnScroll(this::onScroll);
        sub.setOnKeyPressed(this::onKeyPressed);
        sub.setOnKeyReleased(this::onKeyReleased);
    }

    private void removeHandlers() {
        sub.setOnMousePressed(null);
        sub.setOnMouseReleased(null);
        sub.setOnMouseDragged(null);
        sub.setOnScroll(null);
        sub.setOnKeyPressed(null);
        sub.setOnKeyReleased(null);
    }

    private void startLoop() {
        if (timer == null) {
            timer = new AnimationTimer() {
                @Override public void handle(long now) {
                    if (lastNs == 0) { lastNs = now; return; }
                    double dt = (now - lastNs) / 1_000_000_000.0;
                    lastNs = now;
                    update(dt);
                }
            };
        }
        lastNs = 0;
        timer.start();
    }

    private void stopLoop() {
        if (timer != null) timer.stop();
        lastNs = 0;
    }

    private void update(double dt) {
        if (!enabled) return;

        double[] fwd = forward();
        double[] right = right();
        double[] up = new double[]{0,1,0};

        double speed = moveSpeed * (kShift ? boostMultiplier : 1.0);
        double vx = 0, vy = 0, vz = 0;

        if (kW) { vx += fwd[0]; vy += fwd[1]; vz += fwd[2]; }
        if (kS) { vx -= fwd[0]; vy -= fwd[1]; vz -= fwd[2]; }
        if (kD) { vx += right[0]; vy += right[1]; vz += right[2]; }
        if (kA) { vx -= right[0]; vy -= right[1]; vz -= right[2]; }
        if (kUp){ vx += up[0];   vy += up[1];   vz += up[2]; }
        if (kDown){vx -= up[0];  vy -= up[1];   vz -= up[2]; }

        double len = Math.sqrt(vx*vx + vy*vy + vz*vz);
        if (len > 1e-9) {
            vx = (vx/len) * speed * dt;
            vy = (vy/len) * speed * dt;
            vz = (vz/len) * speed * dt;
            px += vx; py += vy; pz += vz;
            applyTransform();
        }
    }

    // Event handler methods
    private void onMousePressed(javafx.scene.input.MouseEvent e) {
        if (e.isPrimaryButtonDown()) {
            dragging = true;
            lastMouseX = e.getSceneX();
            lastMouseY = e.getSceneY();
            sub.requestFocus();
        }
    }

    private void onMouseReleased(javafx.scene.input.MouseEvent e) {
        if (!e.isPrimaryButtonDown()) dragging = false;
    }

    private void onMouseDragged(javafx.scene.input.MouseEvent e) {
        if (!dragging) return;
        double dx = e.getSceneX() - lastMouseX;
        double dy = e.getSceneY() - lastMouseY;
        lastMouseX = e.getSceneX();
        lastMouseY = e.getSceneY();

        yawDeg   += dx * mouseSensitivity;
        pitchDeg -= dy * mouseSensitivity;
        pitchDeg = Math.max(-89.9, Math.min(89.9, pitchDeg));
        applyTransform();
    }

    private void onScroll(javafx.scene.input.ScrollEvent e) {
        double amount = e.getDeltaY() * scrollSpeed * 0.1;
        double[] fwd = forward();
        px += fwd[0] * amount;
        py += fwd[1] * amount;
        pz += fwd[2] * amount;
        applyTransform();
    }

    private void onKeyPressed(javafx.scene.input.KeyEvent e) {
        switch (e.getCode()) {
            case W -> kW = true;
            case A -> kA = true;
            case S -> kS = true;
            case D -> kD = true;
            case SPACE -> kUp = true;
            case CONTROL -> kDown = true;
            case SHIFT -> kShift = true;
            default -> {}
        }
    }

    private void onKeyReleased(javafx.scene.input.KeyEvent e) {
        switch (e.getCode()) {
            case W -> kW = false;
            case A -> kA = false;
            case S -> kS = false;
            case D -> kD = false;
            case SPACE -> kUp = false;
            case CONTROL -> kDown = false;
            case SHIFT -> kShift = false;
            default -> {}
        }
    }

    // Math helpers and transform
    private double[] forward() {
        double yaw = Math.toRadians(yawDeg);
        double pitch = Math.toRadians(pitchDeg);
        double cy = Math.cos(yaw),  sy = Math.sin(yaw);
        double cp = Math.cos(pitch), sp = Math.sin(pitch);
        double fx =  sy * cp;
        double fy = -sp;
        double fz =  cy * cp;
        return new double[]{fx, fy, fz};
    }

    private double[] right() {
        double yaw = Math.toRadians(yawDeg);
        double cy = Math.cos(yaw), sy = Math.sin(yaw);
        return new double[]{ cy, 0, -sy };
    }

    private void applyTransform() {
        cam.getTransforms().setAll(
            new Rotate(pitchDeg, Rotate.X_AXIS),
            new Rotate(yawDeg,   Rotate.Y_AXIS),
            new Translate(px, py, pz)
        );
    }
}