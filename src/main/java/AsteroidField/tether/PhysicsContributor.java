package AsteroidField.tether;

/** A small hook for modules that add forces each fixed physics step. */
public interface PhysicsContributor {
    /** Advance by a fixed time step (seconds) and apply forces to the craft. */
    void step(double dt);
}
