package AsteroidField.physics;

/** Deterministic phases for the fixed-step pipeline. */
public enum PhysicsPhase {
    INPUT,        // read inputs / AI decisions
    PRE_FORCE,    // sensing, precompute
    FORCE,        // apply forces (thrusters, tethers, gravity)
    INTEGRATION,  // integrate velocities/positions
    COLLISION,    // continuous/static collision resolution
    POST,         // cleanup, events, telemetry
    DEFAULT       // fallback
}
