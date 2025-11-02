package AsteroidField.physics;

/** Minimal interface for anything we integrate per physics step. */
public interface KinematicCraft {
    /** Advance simulation by a fixed time step (seconds). */
    void tick(double dt);
}
