package AsteroidField.asteroids.parameters;

/**
 * Icosphere-line hollow-base parameters:
 * - Outer shell (from icosphere)
 * - Inner shell (scaled/roughened; inward-facing)
 * - One or more mouth openings (faces culled on both shells)
 *
 * NOTE: This version creates clean openings but does not yet stitch tunnel walls.
 *       You'll see straight "ports" through the shell thickness (visually fine).
 *       We can add ring-stitched tunnel sleeves in a follow-up.
 */
public class HollowBaseAsteroidParameters extends AsteroidParameters {

    // Mouths
    private final int mouthCount;            // 1..N entrances
    private final double mouthApertureDeg;   // half-angle in degrees for both shells
    private final double mouthJitter;        // 0..0.5

    // Shells
    private final double innerRadiusRatio;   // inner shell radius / outer radius (0.2..0.95)
    private final double innerNoiseAmp;      // inner shell roughness (0..0.3)
    private final double innerNoiseFreq;     // inner shell roughness frequency (ignored in this basic RNG version)

    public static class Builder extends AsteroidParameters.Builder<Builder> {
        private int mouthCount = 1;
        private double mouthApertureDeg = 55.0;
        private double mouthJitter = 0.15;

        private double innerRadiusRatio = 0.6;
        private double innerNoiseAmp = 0.05;
        private double innerNoiseFreq = 1.0;

        public Builder mouthCount(int n) { this.mouthCount = n; return this; }
        public Builder mouthApertureDeg(double deg) { this.mouthApertureDeg = deg; return this; }
        public Builder mouthJitter(double j) { this.mouthJitter = j; return this; }

        public Builder innerRadiusRatio(double r) { this.innerRadiusRatio = r; return this; }
        public Builder innerNoiseAmp(double a) { this.innerNoiseAmp = a; return this; }
        public Builder innerNoiseFreq(double f) { this.innerNoiseFreq = f; return this; }

        @Override public HollowBaseAsteroidParameters build() { return new HollowBaseAsteroidParameters(this); }
        @Override protected Builder self() { return this; }
    }

    private HollowBaseAsteroidParameters(Builder b) {
        super(b);
        this.mouthCount = Math.max(1, b.mouthCount);
        this.mouthApertureDeg = clamp(b.mouthApertureDeg, 10.0, 140.0);
        this.mouthJitter = clamp(b.mouthJitter, 0.0, 0.5);

        this.innerRadiusRatio = clamp(b.innerRadiusRatio, 0.2, 0.95);
        this.innerNoiseAmp = clamp(b.innerNoiseAmp, 0.0, 0.3);
        this.innerNoiseFreq = Math.max(0.0, b.innerNoiseFreq);
    }

    private static double clamp(double v, double a, double b) { return Math.max(a, Math.min(b, v)); }

    public int getMouthCount() { return mouthCount; }
    public double getMouthApertureDeg() { return mouthApertureDeg; }
    public double getMouthJitter() { return mouthJitter; }
    public double getInnerRadiusRatio() { return innerRadiusRatio; }
    public double getInnerNoiseAmp() { return innerNoiseAmp; }
    public double getInnerNoiseFreq() { return innerNoiseFreq; }

    @Override
    public HollowBaseAsteroidParameters.Builder toBuilder() {
        return new HollowBaseAsteroidParameters.Builder()
                .radius(getRadius())
                .subdivisions(getSubdivisions())
                .deformation(getDeformation())
                .seed(getSeed())
                .familyName(getFamilyName())
                .mouthCount(getMouthCount())
                .mouthApertureDeg(getMouthApertureDeg())
                .mouthJitter(getMouthJitter())
                .innerRadiusRatio(getInnerRadiusRatio())
                .innerNoiseAmp(getInnerNoiseAmp())
                .innerNoiseFreq(getInnerNoiseFreq());
    }
}
