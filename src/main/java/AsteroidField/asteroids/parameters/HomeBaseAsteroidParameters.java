package AsteroidField.asteroids.parameters;

/**
 * Icosphere-line, deform-only "Home Base" parameters with rounded crater profile
 * and local Taubin smoothing controls.
 */
public class HomeBaseAsteroidParameters extends AsteroidParameters {

    // Mouth (entrance) controls
    private final int mouthCount;             // 1..N amphitheater craters
    private final double mouthMajorDeg;       // major half-angle (deg)
    private final double mouthMinorDeg;       // minor half-angle (deg)
    private final double mouthJitter;         // 0..0.5

    // Cavity shape controls
    private final double cavityRadiusRatio;   // 0.1..0.9 of outer radius
    private final double cavityBlendScale;    // 1..4 (widens cavity influence)
    private final double rimLift;             // 0..0.2 (outward lip)
    private final double rimSharpness;        // 0.5..3.0

    // Rounded crater profile + smoothing knobs
    private final double plateau;             // 0..0.8 (flat bottom width near axis)
    private final double softnessExp;         // 0.5..3.0 (raised-cosine softness)
    private final int smoothIterations;       // 0..10
    private final double smoothLambda;        // 0..1 (Taubin λ)
    private final double smoothMu;            // -1..0 (Taubin μ)

    public static class Builder extends AsteroidParameters.Builder<Builder> {
        private int mouthCount = 1;
        private double mouthMajorDeg = 90.0;
        private double mouthMinorDeg = 60.0;
        private double mouthJitter   = 0.15;

        private double cavityRadiusRatio = 0.45;
        private double cavityBlendScale  = 2.2;
        private double rimLift      = 0.06;
        private double rimSharpness = 1.1;

        private double plateau = 0.35;
        private double softnessExp = 1.0;
        private int smoothIterations = 2;
        private double smoothLambda = 0.50;
        private double smoothMu     = -0.53;

        public Builder mouthCount(int n) { this.mouthCount = n; return this; }
        public Builder mouthMajorDeg(double d) { this.mouthMajorDeg = d; return this; }
        public Builder mouthMinorDeg(double d) { this.mouthMinorDeg = d; return this; }
        public Builder mouthJitter(double j) { this.mouthJitter = j; return this; }

        public Builder cavityRadiusRatio(double r) { this.cavityRadiusRatio = r; return this; }
        public Builder cavityBlendScale(double s) { this.cavityBlendScale = s; return this; }
        public Builder rimLift(double r) { this.rimLift = r; return this; }
        public Builder rimSharpness(double k) { this.rimSharpness = k; return this; }

        public Builder plateau(double p) { this.plateau = p; return this; }
        public Builder softnessExp(double e) { this.softnessExp = e; return this; }
        public Builder smoothIterations(int iters) { this.smoothIterations = iters; return this; }
        public Builder smoothLambda(double l) { this.smoothLambda = l; return this; }
        public Builder smoothMu(double m) { this.smoothMu = m; return this; }

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
        this.plateau      = clamp(b.plateau, 0.0, 0.8);
        this.softnessExp  = clamp(b.softnessExp, 0.5, 3.0);
        this.smoothIterations = Math.max(0, Math.min(10, b.smoothIterations));
        this.smoothLambda = clamp(b.smoothLambda, 0.0, 1.0);
        this.smoothMu     = clamp(b.smoothMu, -1.0, 0.0);
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
    public double getPlateau() { return plateau; }
    public double getSoftnessExp() { return softnessExp; }
    public int getSmoothIterations() { return smoothIterations; }
    public double getSmoothLambda() { return smoothLambda; }
    public double getSmoothMu() { return smoothMu; }

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
            .rimSharpness(getRimSharpness())
            .plateau(getPlateau())
            .softnessExp(getSoftnessExp())
            .smoothIterations(getSmoothIterations())
            .smoothLambda(getSmoothLambda())
            .smoothMu(getSmoothMu());
    }
}
