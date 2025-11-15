package AsteroidField.physics;

import javafx.animation.AnimationTimer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Always-on fixed-step physics loop. Orchestrates contributors only. */
public final class PhysicsSystem {

// --- PERF LOGGING (PhysicsSystem) ---
//private static final boolean PERF = Boolean.getBoolean("perf.logs");
private static final boolean PERF = true;
private static final long PERF_WINDOW_NS = 1_000_000_000L; // 1s

private long perfWindowStartNs = 0L;
private long perfPhysNsAcc = 0L;
private int  perfSubstepsAcc = 0;
    
    private final double fixedDt;              // e.g., 1/120
    private final double maxAccumulator = 0.25;
    private final List<PhysicsContributor> contributors = new ArrayList<>();
    private boolean enabled = true;

    private double accumulator = 0.0;
    private long lastNs = -1L;

    private final AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (!enabled) return;
            if (lastNs < 0) { lastNs = now; return; }

            final long perfFrameStart = PERF ? System.nanoTime() : 0L;

            double dt = (now - lastNs) * 1e-9;
            lastNs = now;

            accumulator = Math.min(accumulator + dt, maxAccumulator);
            int substeps = 0;

            while (accumulator >= fixedDt) {
                for (PhysicsContributor c : contributors) {
                    c.step(fixedDt);
                }
                accumulator -= fixedDt;
                substeps++;
            }

            if (PERF) {
                long frameNs = System.nanoTime() - perfFrameStart;
                perfPhysNsAcc += frameNs;
                perfSubstepsAcc += substeps;

                if (perfWindowStartNs == 0L)
                    perfWindowStartNs = System.nanoTime();

                long elapsed = System.nanoTime() - perfWindowStartNs;
                if (elapsed >= PERF_WINDOW_NS) {
                    double ms = perfPhysNsAcc / 1_000_000.0;
                    System.out.printf(
                        "[PERF] t=%d, Physics, phys_total_ms=%.3f, substeps=%d, fixed_dt_ms=%.3f%n",
                        System.currentTimeMillis(),
                        ms,
                        perfSubstepsAcc,
                        fixedDt * 1000.0
                    );

                    // reset
                    perfWindowStartNs = System.nanoTime();
                    perfPhysNsAcc = 0L;
                    perfSubstepsAcc = 0;
                }
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
