package AsteroidField;

public class CrateredAsteroidParameters extends AsteroidParameters {
    private final int craterCount;
    private final double craterDepth;
    private final double craterWidth;

    public static class Builder extends AsteroidParameters.Builder<Builder> {
        private int craterCount = 5;
        private double craterDepth = 0.2;
        private double craterWidth = 0.2;

        public Builder craterCount(int count) { this.craterCount = count; return this; }
        public Builder craterDepth(double depth) { this.craterDepth = depth; return this; }
        public Builder craterWidth(double width) { this.craterWidth = width; return this; }

        @Override
        public CrateredAsteroidParameters build() {
            return new CrateredAsteroidParameters(this);
        }
        @Override
        protected Builder self() { return this; }
    }

    private CrateredAsteroidParameters(Builder builder) {
        super(builder);
        this.craterCount = builder.craterCount;
        this.craterDepth = builder.craterDepth;
        this.craterWidth = builder.craterWidth;
    }
    public int getCraterCount() { return craterCount; }
    public double getCraterDepth() { return craterDepth; }
    public double getCraterWidth() { return craterWidth; }
}
