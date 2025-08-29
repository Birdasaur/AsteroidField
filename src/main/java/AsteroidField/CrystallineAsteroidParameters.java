package AsteroidField;

public class CrystallineAsteroidParameters extends AsteroidParameters {
    private final int crystalCount;
    private final int prismSides;
    private final boolean capBase;
    private final boolean capTip;
    private final int maxClusterSize;
    private final int clusterSpread;
    private final double minCrystalLength, maxCrystalLength;
    private final double minCrystalRadius, maxCrystalRadius;
    private final double tipRadiusScale;
    private final double maxTiltAngleRadians;
    private final double lengthJitter, radiusJitter;
    private final double embedDepth; // NEW: fraction of base radius

    public static class Builder extends AsteroidParameters.Builder<Builder> {
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
        private double maxTiltAngleRadians = Math.toRadians(12.0);
        private double lengthJitter = 0.08;
        private double radiusJitter = 0.06;
        private double embedDepth = 0.18; // 18% by default

        public Builder crystalCount(int n) { this.crystalCount = n; return this; }
        public Builder prismSides(int n) { this.prismSides = n; return this; }
        public Builder capBase(boolean b) { this.capBase = b; return this; }
        public Builder capTip(boolean b) { this.capTip = b; return this; }
        public Builder maxClusterSize(int n) { this.maxClusterSize = n; return this; }
        public Builder clusterSpread(int n) { this.clusterSpread = n; return this; }
        public Builder minCrystalLength(double d) { this.minCrystalLength = d; return this; }
        public Builder maxCrystalLength(double d) { this.maxCrystalLength = d; return this; }
        public Builder minCrystalRadius(double d) { this.minCrystalRadius = d; return this; }
        public Builder maxCrystalRadius(double d) { this.maxCrystalRadius = d; return this; }
        public Builder tipRadiusScale(double d) { this.tipRadiusScale = d; return this; }
        public Builder maxTiltAngleRadians(double d) { this.maxTiltAngleRadians = d; return this; }
        public Builder lengthJitter(double d) { this.lengthJitter = d; return this; }
        public Builder radiusJitter(double d) { this.radiusJitter = d; return this; }
        public Builder embedDepth(double d) { this.embedDepth = d; return this; } // NEW

        @Override
        public CrystallineAsteroidParameters build() { return new CrystallineAsteroidParameters(this); }
        @Override
        protected Builder self() { return this; }
    }

    private CrystallineAsteroidParameters(Builder b) {
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
        this.maxTiltAngleRadians = b.maxTiltAngleRadians;
        this.lengthJitter = b.lengthJitter;
        this.radiusJitter = b.radiusJitter;
        this.embedDepth = b.embedDepth;
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
    public double getMaxTiltAngleRadians() { return maxTiltAngleRadians; }
    public double getLengthJitter() { return lengthJitter; }
    public double getRadiusJitter() { return radiusJitter; }
    public double getEmbedDepth() { return embedDepth; } // NEW

    @Override
    public CrystallineAsteroidParameters.Builder toBuilder() {
        return new CrystallineAsteroidParameters.Builder()
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
            .maxTiltAngleRadians(getMaxTiltAngleRadians())
            .lengthJitter(getLengthJitter())
            .radiusJitter(getRadiusJitter())
            .embedDepth(getEmbedDepth()); // NEW
    }
}
