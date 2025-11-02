package AsteroidField.physics;

import AsteroidField.tether.KinematicCraft;
import AsteroidField.tether.PhysicsContributor;
import javafx.animation.AnimationTimer;

import java.util.ArrayList;
import java.util.List;

/** Always-on fixed-step physics loop. No input or game feature logic lives here. */
public final class PhysicsSystem {

    private final double fixedDt;            // seconds (e.g., 1/120)
    private final double maxAccumulator = 0.25;
    private final List<PhysicsContributor> contributors = new ArrayList<>();

    private KinematicCraft craft;            // optional single-body integrator (your camera rig)
    private boolean enabled = true;

    private double accumulator = 0.0;
    private long lastNs = -1L;

    private final AnimationTimer timer = new AnimationTimer() {
        @Override public void handle(long now) {
            if (!enabled) return;
            if (lastNs < 0) { lastNs = now; return; }

            double dt = (now - lastNs) * 1e-9;
            lastNs = now;

            accumulator = Math.min(accumulator + dt, maxAccumulator);
            while (accumulator >= fixedDt) {
                // 1) Contributors apply forces / do per-step logic
                for (PhysicsContributor c : contributors) c.step(fixedDt);

                // 2) Integrate the craft (if present)
                if (craft != null) craft.tick(fixedDt);

                accumulator -= fixedDt;
            }
        }
    };

    public PhysicsSystem(double fixedHz) {
        this.fixedDt = (fixedHz <= 0) ? (1.0 / 120.0) : (1.0 / fixedHz);
        timer.start();
    }

    public void addContributor(PhysicsContributor c) { if (c != null) contributors.add(c); }
    public void removeContributor(PhysicsContributor c) { contributors.remove(c); }

    public void setCraft(KinematicCraft craft) { this.craft = craft; }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        accumulator = 0.0;
        lastNs = -1L;
    }

    public boolean isEnabled() { return enabled; }
    public double getFixedDt() { return fixedDt; }
}
