package AsteroidField.field.families;

import AsteroidField.asteroids.AsteroidFamilyUI;
import AsteroidField.asteroids.parameters.AsteroidParameters;
import AsteroidField.asteroids.providers.AsteroidMeshProvider;
import java.util.Random;

/** Utilities to build family-correct parameters for field generation. */
public final class FamilySupport {
    private FamilySupport() {}

    /**
     * Create parameters for the given provider using global knobs for radius/subdivisions/deformation.
     * If provider implements AsteroidFamilyUI, we delegate to buildDefaultParamsFrom(...) so the family
     * fills its extra fields consistently (e.g., crater centers).
     */
    public static AsteroidParameters createParams(AsteroidMeshProvider provider,
                                                  Random rng,
                                                  double radius,
                                                  int subdivisions,
                                                  double deformation) {
        long seed = rng.nextLong();
        if (provider instanceof AsteroidFamilyUI) {
            AsteroidFamilyUI ui = (AsteroidFamilyUI) provider;
            AsteroidParameters previous = new AsteroidParameters.Builder<>()
                    .radius(radius)
                    .subdivisions(subdivisions)
                    .deformation(deformation)
                    .seed(seed)
                    .familyName(provider.getDisplayName())
                    .build();
            return ui.buildDefaultParamsFrom(previous);
        } else {
            return new AsteroidParameters.Builder<>()
                    .radius(radius)
                    .subdivisions(subdivisions)
                    .deformation(deformation)
                    .seed(seed)
                    .familyName(provider.getDisplayName())
                    .build();
        }
    }
}
