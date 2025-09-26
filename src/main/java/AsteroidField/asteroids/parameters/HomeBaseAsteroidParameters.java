package AsteroidField.asteroids.parameters;

/**
 * Icosphere-line, deform-only "Home Base" parameters:
 * - 1..N mouth directions (amphitheater-style craters)
 * - elliptical mouth aperture (major/minor)
 * - interior cavity ratio (fraction of outer radius)
 * - optional rim lift for a visible lip at the crater boundary
 */
public class HomeBaseAsteroidParameters extends AsteroidParameters {

    // Mouth (entrance) controls
    private final int mouthCount;             // number of amphitheater craters
    private final double mouthMajorDeg;       // major half-angle in degrees (ellipse axis U)
    private final double mouthMinorDeg;       // minor half-angle in degrees (ellipse axis V)
    private final double mouthJitter;         // 0..0.5 angular jitter for mouth placement

    // Cavity controls
    private final double cavityRadiusRatio;   // target inner radius near crater axis (0.1..0.9)
    private final double cavityBlendScale;    // scales the mouth half-angles for cavity blending (1..4)
    private final double rimLift;             // outward lip as fraction of radius (0..0.2)
    private final double rimSharpness;        // 0.5..3.0 higher = tighter lip ring

    public static class Builder extends AsteroidParameters.Builder<Builder> {
        private int mouthCount = 1;
        private double mouthMajorDeg = 90.0;
        private double mouthMinorDeg = 60.0;
        private double mouthJitter = 0.15;

        private double cavityRadiusRatio = 0.45;
        private double cavityBlendScale = 2.2;
        private double rimLift = 0.06;
        private double rimSharpness = 1.1;

        public Builder mouthCount(int n) { this.mouthCount = n; return this; }
        public Builder mouthMajorDeg(double d) { this.mouthMajorDeg = d; return this; }
        public Builder mouthMinorDeg(double d) { this.mouthMinorDeg = d; return this; }
        public Builder mouthJitter(double j) { this.mouthJitter = j; return this; }

        public Builder cavityRadiusRatio(double r) { this.cavityRadiusRatio = r; return this; }
        public Builder cavityBlendScale(double s) { this.cavityBlendScale = s; return this; }
        public Builder rimLift(double r) { this.rimLift = r; return this; }
        public Builder rimSharpness(double k) { this.rimSharpness = k; return this; }

        @Override public HomeBaseAsteroidParameters build() { return new HomeBaseAsteroidParameters(this); }
        @Override protected Builder self() { return this; }
    }

    private HomeBaseAsteroidParameters(Builder b) {
        super(b);
        this.mouthCount = Math.max(1, b.mouthCount);
        this.mouthMajorDeg = clamp(b.mouthMajorDeg, 10.0, 160.0);
        this.mouthMinorDeg = clamp(b.mouthMinorDeg, 10.0, 160.0);
        this.mouthJitter   = clamp(b.mouthJitter, 0.0, 0.5);
        this.cavityRadiusRatio = clamp(b.cavityRadiusRatio, 0.1, 0.9);
        this.cavityBlendScale  = clamp(b.cavityBlendScale, 1.0, 4.0);
        this.rimLift      = clamp(b.rimLift, 0.0, 0.2);
        this.rimSharpness = clamp(b.rimSharpness, 0.5, 3.0);
    }

    private static double clamp(double v, double a, double b) { return Math.max(a, Math.min(b, v)); }

    public int getMouthCount() { return mouthCount; }
    public double getMouthMajorDeg() { return mouthMajorDeg; }
    public double getMouthMinorDeg() { return mouthMinorDeg; }
    public double getMouthJitter() { return mouthJitter; }
    public double getCavityRadiusRatio() { return cavityRadiusRatio; }
    public double getCavityBlendScale() { return cavityBlendScale; }
    public double getRimLift() { return rimLift; }
    public double getRimSharpness() { return rimSharpness; }

    @Override
    public HomeBaseAsteroidParameters.Builder toBuilder() {
        return new HomeBaseAsteroidParameters.Builder()
            .radius(getRadius())
            .subdivisions(getSubdivisions())
            .deformation(getDeformation())
            .seed(getSeed())
            .familyName(getFamilyName())
            .mouthCount(getMouthCount())
            .mouthMajorDeg(getMouthMajorDeg())
            .mouthMinorDeg(getMouthMinorDeg())
            .mouthJitter(getMouthJitter())
            .cavityRadiusRatio(getCavityRadiusRatio())
            .cavityBlendScale(getCavityBlendScale())
            .rimLift(getRimLift())
            .rimSharpness(getRimSharpness());
    }
}
