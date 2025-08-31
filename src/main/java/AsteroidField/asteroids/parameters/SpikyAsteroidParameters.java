package AsteroidField.asteroids.parameters;

public class SpikyAsteroidParameters extends AsteroidParameters {
    private final int spikeCount;
    private final double spikeLength, spikeWidth, randomness, spikeSpacingJitter;

    public static class Builder extends AsteroidParameters.Builder<Builder> {
        private int spikeCount = 4;
        private double spikeLength = 1.5; // multiplier on radius
        private double spikeWidth = 0.25; // angular width (0.01..0.5) of each spike
        private double randomness = 0.4;  // 0 = all spikes same, 1 = highly variable (length+width)
        private double spikeSpacingJitter = 0.1; // 0 = evenly spaced, up to ~0.3 for jitter

        public Builder spikeCount(int n) { this.spikeCount = n; return this; }
        public Builder spikeLength(double d) { this.spikeLength = d; return this; }
        public Builder spikeWidth(double d) { this.spikeWidth = d; return this; }
        public Builder randomness(double d) { this.randomness = d; return this; }
        public Builder spikeSpacingJitter(double d) { this.spikeSpacingJitter = d; return this; }

        @Override
        public SpikyAsteroidParameters build() { return new SpikyAsteroidParameters(this); }
        @Override protected Builder self() { return this; }
    }

    private SpikyAsteroidParameters(Builder b) {
        super(b);
        this.spikeCount = b.spikeCount;
        this.spikeLength = b.spikeLength;
        this.spikeWidth = b.spikeWidth;
        this.randomness = b.randomness;
        this.spikeSpacingJitter = b.spikeSpacingJitter;
    }

    public int getSpikeCount() { return spikeCount; }
    public double getSpikeLength() { return spikeLength; }
    public double getSpikeWidth() { return spikeWidth; }
    public double getRandomness() { return randomness; }
    public double getSpikeSpacingJitter() { return spikeSpacingJitter; }

    @Override
    public SpikyAsteroidParameters.Builder toBuilder() {
        return new SpikyAsteroidParameters.Builder()
            .radius(getRadius())
            .subdivisions(getSubdivisions())
            .deformation(getDeformation())
            .seed(getSeed())
            .familyName(getFamilyName())
            .spikeCount(getSpikeCount())
            .spikeLength(getSpikeLength())
            .spikeWidth(getSpikeWidth())
            .randomness(getRandomness())
            .spikeSpacingJitter(getSpikeSpacingJitter());
    }
}
