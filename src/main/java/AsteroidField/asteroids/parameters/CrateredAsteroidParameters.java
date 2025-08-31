package AsteroidField.asteroids.parameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CrateredAsteroidParameters extends AsteroidParameters {
    private final int craterCount;
    private final double craterDepth;
    private final double craterWidth;
    private final List<double[]> craterCenters;

    public static class Builder extends AsteroidParameters.Builder<Builder> {
        private int craterCount = 5;
        private double craterDepth = 0.2;
        private double craterWidth = 0.2;
        private List<double[]> craterCenters = null;

        public Builder craterCount(int count) { this.craterCount = count; return this; }
        public Builder craterDepth(double depth) { this.craterDepth = depth; return this; }
        public Builder craterWidth(double width) { this.craterWidth = width; return this; }
        public Builder craterCenters(List<double[]> centers) { this.craterCenters = centers; return this; }

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
        this.craterCenters = (builder.craterCenters != null)
                ? Collections.unmodifiableList(new ArrayList<>(builder.craterCenters))
                : null;
    }
    public int getCraterCount() { return craterCount; }
    public double getCraterDepth() { return craterDepth; }
    public double getCraterWidth() { return craterWidth; }
    public List<double[]> getCraterCenters() { return craterCenters; }

    @Override
    public CrateredAsteroidParameters.Builder toBuilder() {
        return new CrateredAsteroidParameters.Builder()
                .radius(getRadius())
                .subdivisions(getSubdivisions())
                .deformation(getDeformation())
                .seed(getSeed())
                .familyName(getFamilyName())
                .craterCount(getCraterCount())
                .craterDepth(getCraterDepth())
                .craterWidth(getCraterWidth())
                .craterCenters(getCraterCenters());
    }
}