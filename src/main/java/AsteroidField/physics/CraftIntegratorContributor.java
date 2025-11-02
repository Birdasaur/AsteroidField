package AsteroidField.physics;

/** Runs the craft's integrator once per fixed step. */
public final class CraftIntegratorContributor implements PhysicsContributor {
    private final KinematicCraft craft;

    public CraftIntegratorContributor(KinematicCraft craft) {
        this.craft = craft;
    }

    @Override
    public void step(double dt) {
        if (craft != null) craft.tick(dt);
    }

    @Override
    public PhysicsPhase getPhase() { return PhysicsPhase.INTEGRATION; }

    @Override
    public int getPriority() { return 0; }
}
