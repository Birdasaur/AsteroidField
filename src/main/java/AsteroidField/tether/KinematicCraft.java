package AsteroidField.tether;

/** Optional extension so the tether system can tick inertia/damping each frame. */
public interface KinematicCraft extends Tether.SpacecraftAdapter {
    void tick(double dtSeconds);
}