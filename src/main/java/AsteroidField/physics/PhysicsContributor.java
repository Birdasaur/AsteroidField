package AsteroidField.physics;

/** Modules that advance once per fixed physics step. */
public interface PhysicsContributor {
    /** Advance by a fixed time step (seconds). */
    void step(double dt);

    /** The phase this contributor runs in. Defaults to DEFAULT. */
    default PhysicsPhase getPhase() { return PhysicsPhase.DEFAULT; }

    /** Optional intra-phase ordering; lower runs earlier. */
    default int getPriority() { return 0; }
}
