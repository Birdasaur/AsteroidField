package AsteroidField.physics;

import javafx.animation.AnimationTimer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Always-on fixed-step physics loop. Orchestrates contributors only. */
public final class PhysicsSystem {

    private final double fixedDt;              // e.g., 1/120
    private final double maxAccumulator = 0.25;
    private final List<PhysicsContributor> contributors = new ArrayList<>();
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
                for (PhysicsContributor c : contributors) {
                    c.step(fixedDt);
                }
                accumulator -= fixedDt;
            }
        }
    };

    public PhysicsSystem(double fixedHz) {
        this.fixedDt = (fixedHz <= 0) ? (1.0 / 120.0) : (1.0 / fixedHz);
        timer.start();
    }

    public void addContributor(PhysicsContributor c) {
        if (c == null) return;
        contributors.add(c);
        sortContributors();
    }

    public void removeContributor(PhysicsContributor c) {
        contributors.remove(c);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        accumulator = 0.0;
        lastNs = -1L;
    }

    public boolean isEnabled() { return enabled; }
    public double getFixedDt() { return fixedDt; }

    private void sortContributors() {
        contributors.sort(Comparator
                .comparing((PhysicsContributor x) -> x.getPhase())
                .thenComparingInt(PhysicsContributor::getPriority));
    }
}
