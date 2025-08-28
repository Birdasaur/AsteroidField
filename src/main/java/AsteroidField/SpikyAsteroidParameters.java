package AsteroidField;

public class SpikyAsteroidParameters extends AsteroidParameters {
    private final int spikeCount;
    private final double spikeLength, spikeWidth, randomness;

    public static class Builder extends AsteroidParameters.Builder<Builder> {
        private int spikeCount = 20;
        private double spikeLength = 1.5; // multiplier on radius
        private double spikeWidth = 0.25; // fraction of vertices that are spiked
        private double randomness = 0.4;  // 0 = all spikes same length, 1 = highly varied

        public Builder spikeCount(int n) { this.spikeCount = n; return this; }
        public Builder spikeLength(double d) { this.spikeLength = d; return this; }
        public Builder spikeWidth(double d) { this.spikeWidth = d; return this; }
        public Builder randomness(double d) { this.randomness = d; return this; }

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
    }

    public int getSpikeCount() { return spikeCount; }
    public double getSpikeLength() { return spikeLength; }
    public double getSpikeWidth() { return spikeWidth; }
    public double getRandomness() { return randomness; }

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
            .randomness(getRandomness());
    }
}
