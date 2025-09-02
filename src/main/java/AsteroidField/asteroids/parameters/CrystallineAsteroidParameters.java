package AsteroidField.asteroids.parameters;

/**
 * Parameters for procedural crystalline asteroids.
 * <p>
 * Uses a generic builder for extensibility and subclassing.
 * </p>
 */
public class CrystallineAsteroidParameters extends AsteroidParameters {

    // Crystal shape/count
    private final int crystalCount;
    private final int prismSides;
    private final boolean capBase;
    private final boolean capTip;
    private final int maxClusterSize;
    private final int clusterSpread;

    // Crystal sizing
    private final double minCrystalLength, maxCrystalLength;
    private final double minCrystalRadius, maxCrystalRadius;
    private final double tipRadiusScale;

    // Embedding, clustering, faceting
    private final double embedDepth;
    private final double facetJitter;
    private final double lengthJitter, radiusJitter;
    private final double maxTiltAngleRadians;

    // Tip/termination shaping
    private final double pointyTipChance;
    private final double bevelTipChance;
    private final double bevelDepth;

    // Crystal offshoots
    private final double offshootChance;
    private final double offshootScale;
    private final int offshootRecursion;

    // Advanced effects
    private final double twistAmount;
    private final double fractureChance;
    private final double fractureDepth;

    /**
     * Generic builder pattern for subclassing and fluent usage.
     * @param <T> The builder subclass type.
     */
    public static class Builder<T extends Builder<T>> extends AsteroidParameters.Builder<T> {
        private int crystalCount = 12;
        private int prismSides = 6;
        private boolean capBase = true;
        private boolean capTip = true;
        private int maxClusterSize = 2;
        private int clusterSpread = 1;
        private double minCrystalLength = 0.04;
        private double maxCrystalLength = 0.11;
        private double minCrystalRadius = 0.012;
        private double maxCrystalRadius = 0.022;
        private double tipRadiusScale = 0.25;
        private double embedDepth = 0.18;
        private double facetJitter = 0.07;
        private double lengthJitter = 0.08;
        private double radiusJitter = 0.06;
        private double maxTiltAngleRadians = Math.toRadians(12.0);

        private double pointyTipChance = 0.18;
        private double bevelTipChance = 0.11;
        private double bevelDepth = 0.08;
        private double offshootChance = 0.12;
        private double offshootScale = 0.46;
        private int offshootRecursion = 1;

        private double twistAmount = Math.toRadians(10.0);
        private double fractureChance = 0.18;
        private double fractureDepth = 0.13;

        // --- Fluent setters returning T ---
        public T crystalCount(int n) { this.crystalCount = n; return self(); }
        public T prismSides(int n) { this.prismSides = n; return self(); }
        public T capBase(boolean b) { this.capBase = b; return self(); }
        public T capTip(boolean b) { this.capTip = b; return self(); }
        public T maxClusterSize(int n) { this.maxClusterSize = n; return self(); }
        public T clusterSpread(int n) { this.clusterSpread = n; return self(); }
        public T minCrystalLength(double d) { this.minCrystalLength = d; return self(); }
        public T maxCrystalLength(double d) { this.maxCrystalLength = d; return self(); }
        public T minCrystalRadius(double d) { this.minCrystalRadius = d; return self(); }
        public T maxCrystalRadius(double d) { this.maxCrystalRadius = d; return self(); }
        public T tipRadiusScale(double d) { this.tipRadiusScale = d; return self(); }
        public T embedDepth(double d) { this.embedDepth = d; return self(); }
        public T facetJitter(double d) { this.facetJitter = d; return self(); }
        public T lengthJitter(double d) { this.lengthJitter = d; return self(); }
        public T radiusJitter(double d) { this.radiusJitter = d; return self(); }
        public T maxTiltAngleRadians(double d) { this.maxTiltAngleRadians = d; return self(); }
        public T pointyTipChance(double d) { this.pointyTipChance = d; return self(); }
        public T bevelTipChance(double d) { this.bevelTipChance = d; return self(); }
        public T bevelDepth(double d) { this.bevelDepth = d; return self(); }
        public T offshootChance(double d) { this.offshootChance = d; return self(); }
        public T offshootScale(double d) { this.offshootScale = d; return self(); }
        public T offshootRecursion(int n) { this.offshootRecursion = n; return self(); }
        public T twistAmount(double d) { this.twistAmount = d; return self(); }
        public T fractureChance(double d) { this.fractureChance = d; return self(); }
        public T fractureDepth(double d) { this.fractureDepth = d; return self(); }

        @Override
        public CrystallineAsteroidParameters build() { return new CrystallineAsteroidParameters(this); }
        @Override
        protected T self() { return (T) this; }
    }

    public CrystallineAsteroidParameters(Builder<?> b) {
        super(b);
        this.crystalCount = b.crystalCount;
        this.prismSides = b.prismSides;
        this.capBase = b.capBase;
        this.capTip = b.capTip;
        this.maxClusterSize = b.maxClusterSize;
        this.clusterSpread = b.clusterSpread;
        this.minCrystalLength = b.minCrystalLength;
        this.maxCrystalLength = b.maxCrystalLength;
        this.minCrystalRadius = b.minCrystalRadius;
        this.maxCrystalRadius = b.maxCrystalRadius;
        this.tipRadiusScale = b.tipRadiusScale;
        this.embedDepth = b.embedDepth;
        this.facetJitter = b.facetJitter;
        this.lengthJitter = b.lengthJitter;
        this.radiusJitter = b.radiusJitter;
        this.maxTiltAngleRadians = b.maxTiltAngleRadians;
        this.pointyTipChance = b.pointyTipChance;
        this.bevelTipChance = b.bevelTipChance;
        this.bevelDepth = b.bevelDepth;
        this.offshootChance = b.offshootChance;
        this.offshootScale = b.offshootScale;
        this.offshootRecursion = b.offshootRecursion;
        this.twistAmount = b.twistAmount;
        this.fractureChance = b.fractureChance;
        this.fractureDepth = b.fractureDepth;
    }

    public int getCrystalCount() { return crystalCount; }
    public int getPrismSides() { return prismSides; }
    public boolean isCapBase() { return capBase; }
    public boolean isCapTip() { return capTip; }
    public int getMaxClusterSize() { return maxClusterSize; }
    public int getClusterSpread() { return clusterSpread; }
    public double getMinCrystalLength() { return minCrystalLength; }
    public double getMaxCrystalLength() { return maxCrystalLength; }
    public double getMinCrystalRadius() { return minCrystalRadius; }
    public double getMaxCrystalRadius() { return maxCrystalRadius; }
    public double getTipRadiusScale() { return tipRadiusScale; }
    public double getEmbedDepth() { return embedDepth; }
    public double getFacetJitter() { return facetJitter; }
    public double getLengthJitter() { return lengthJitter; }
    public double getRadiusJitter() { return radiusJitter; }
    public double getMaxTiltAngleRadians() { return maxTiltAngleRadians; }
    public double getPointyTipChance() { return pointyTipChance; }
    public double getBevelTipChance() { return bevelTipChance; }
    public double getBevelDepth() { return bevelDepth; }
    public double getOffshootChance() { return offshootChance; }
    public double getOffshootScale() { return offshootScale; }
    public int getOffshootRecursion() { return offshootRecursion; }
    public double getTwistAmount() { return twistAmount; }
    public double getFractureChance() { return fractureChance; }
    public double getFractureDepth() { return fractureDepth; }

    @Override
    public Builder<?> toBuilder() {
        return new Builder<>()
            .radius(getRadius())
            .subdivisions(getSubdivisions())
            .deformation(getDeformation())
            .seed(getSeed())
            .familyName(getFamilyName())
            .crystalCount(getCrystalCount())
            .prismSides(getPrismSides())
            .capBase(isCapBase())
            .capTip(isCapTip())
            .maxClusterSize(getMaxClusterSize())
            .clusterSpread(getClusterSpread())
            .minCrystalLength(getMinCrystalLength())
            .maxCrystalLength(getMaxCrystalLength())
            .minCrystalRadius(getMinCrystalRadius())
            .maxCrystalRadius(getMaxCrystalRadius())
            .tipRadiusScale(getTipRadiusScale())
            .embedDepth(getEmbedDepth())
            .facetJitter(getFacetJitter())
            .lengthJitter(getLengthJitter())
            .radiusJitter(getRadiusJitter())
            .maxTiltAngleRadians(getMaxTiltAngleRadians())
            .pointyTipChance(getPointyTipChance())
            .bevelTipChance(getBevelTipChance())
            .bevelDepth(getBevelDepth())
            .offshootChance(getOffshootChance())
            .offshootScale(getOffshootScale())
            .offshootRecursion(getOffshootRecursion())
            .twistAmount(getTwistAmount())
            .fractureChance(getFractureChance())
            .fractureDepth(getFractureDepth());
    }
}