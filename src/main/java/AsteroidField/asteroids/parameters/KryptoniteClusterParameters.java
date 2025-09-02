package AsteroidField.asteroids.parameters;

/**
 * Parameters for Kryptonite Cluster asteroids, extending CrystallineAsteroidParameters.
 * Uses a generic builder for robust fluent subclassing.
 */
public class KryptoniteClusterParameters extends CrystallineAsteroidParameters {

    private final int numClusters;
    private final int crystalsPerCluster;
    private final double diskAngleDegrees;

    /**
     * Generic builder for KryptoniteClusterParameters.
     * Allows fluent chaining and robust subclassing.
     */
    public static class Builder extends CrystallineAsteroidParameters.Builder<Builder> {
        private int numClusters = 1;
        private int crystalsPerCluster = 20;
        private double diskAngleDegrees = 24.0;

        public Builder numClusters(int n) { this.numClusters = n; return this; }
        public Builder crystalsPerCluster(int n) { this.crystalsPerCluster = n; return this; }
        public Builder diskAngleDegrees(double d) { this.diskAngleDegrees = d; return this; }

        @Override
        public KryptoniteClusterParameters build() { return new KryptoniteClusterParameters(this); }
        @Override
        protected Builder self() { return this; }
    }

    public KryptoniteClusterParameters(Builder b) {
        super(b);
        this.numClusters = b.numClusters;
        this.crystalsPerCluster = b.crystalsPerCluster;
        this.diskAngleDegrees = b.diskAngleDegrees;
    }

    public int getNumClusters() { return numClusters; }
    public int getCrystalsPerCluster() { return crystalsPerCluster; }
    public double getDiskAngleDegrees() { return diskAngleDegrees; }

    /**
     * Returns a builder initialized with all current values.
     * This is professional Java: return type is parent builder for API compatibility.
     * If you need the full subclass builder, simply cast.
     */
    @Override
    public Builder toBuilder() {
        return new Builder()
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
            .fractureDepth(getFractureDepth())
            .numClusters(getNumClusters())
            .crystalsPerCluster(getCrystalsPerCluster())
            .diskAngleDegrees(getDiskAngleDegrees());
    }
}
