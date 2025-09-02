package AsteroidField.asteroids.parameters;

/**
 * Parameters for cubic-based asteroids. Uses generic builder for extensible subfamilies.
 */
public class CubicAsteroidParameters extends AsteroidParameters {
    private final int subdivisions;
    private final double deformation;

    /** Covariant Builder pattern for subclass compatibility.
     * @param <T> */
    public static class Builder<T extends Builder<T>> extends AsteroidParameters.Builder<T> {
        protected int subdivisions = 1;
        protected double deformation = 0.12;
        @Override
        public T subdivisions(int n) { this.subdivisions = n; return self(); }
        @Override
        public T deformation(double d) { this.deformation = d; return self(); }
        @Override
        public CubicAsteroidParameters build() { return new CubicAsteroidParameters(this); }
        @Override
        protected T self() { return (T) this; }
    }

    /** Subclass-friendly constructor
     * @param b */
    protected CubicAsteroidParameters(Builder<?> b) {
        super(b);
        this.subdivisions = b.subdivisions;
        this.deformation = b.deformation;
    }

    @Override
    public int getSubdivisions() { return subdivisions; }
    @Override
    public double getDeformation() { return deformation; }

    @Override
    public Builder<?> toBuilder() {
        return new Builder<>()
            .radius(getRadius())
            .subdivisions(getSubdivisions())
            .deformation(getDeformation())
            .seed(getSeed())
            .familyName(getFamilyName());
    }
}
