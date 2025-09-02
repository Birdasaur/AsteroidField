package AsteroidField.asteroids.parameters;

public class CubicAsteroidParameters extends AsteroidParameters {
    private final int subdivisions;
    private final double deformation;

    public static class Builder extends AsteroidParameters.Builder<Builder> {
        private int subdivisions = 0;
        private double deformation = 0.12;
        public Builder subdivisions(int n) { this.subdivisions = n; return self(); }
        public Builder deformation(double d) { this.deformation = d; return self(); }
        @Override public CubicAsteroidParameters build() { return new CubicAsteroidParameters(this); }
        @Override protected Builder self() { return this; }
    }
    private CubicAsteroidParameters(Builder b) {
        super(b);
        this.subdivisions = b.subdivisions;
        this.deformation = b.deformation;
    }
    public int getSubdivisions() { return subdivisions; }
    public double getDeformation() { return deformation; }
    @Override
    public Builder toBuilder() {
        return new Builder()
            .radius(getRadius())
            .subdivisions(getSubdivisions())
            .deformation(getDeformation())
            .seed(getSeed())
            .familyName(getFamilyName());
    }
}
