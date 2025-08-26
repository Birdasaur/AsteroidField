package AsteroidField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CapsuleAsteroidParameters extends AsteroidParameters {
    private final double length, width;
    private final int craterCount, bumpCount;
    private final double craterDepth, craterRadius;
    private final double bumpHeight, bumpRadius;
    private final List<double[]> craterCenters;
    private final List<double[]> bumpCenters;

    public static class Builder extends AsteroidParameters.Builder<Builder> {
        private double length = 200;
        private double width = 60;
        private int craterCount = 5;
        private double craterDepth = 0.25, craterRadius = 0.2;
        private int bumpCount = 4;
        private double bumpHeight = 0.22, bumpRadius = 0.18;
        private List<double[]> craterCenters = null;
        private List<double[]> bumpCenters = null;

        public Builder length(double v) { this.length = v; return this; }
        public Builder width(double v) { this.width = v; return this; }
        public Builder craterCount(int v) { this.craterCount = v; return this; }
        public Builder craterDepth(double v) { this.craterDepth = v; return this; }
        public Builder craterRadius(double v) { this.craterRadius = v; return this; }
        public Builder bumpCount(int v) { this.bumpCount = v; return this; }
        public Builder bumpHeight(double v) { this.bumpHeight = v; return this; }
        public Builder bumpRadius(double v) { this.bumpRadius = v; return this; }
        public Builder craterCenters(List<double[]> c) { this.craterCenters = c; return this; }
        public Builder bumpCenters(List<double[]> b) { this.bumpCenters = b; return this; }

        @Override
        public CapsuleAsteroidParameters build() { return new CapsuleAsteroidParameters(this); }
        @Override protected Builder self() { return this; }
    }

    private CapsuleAsteroidParameters(Builder b) {
        super(b);
        this.length = b.length;
        this.width = b.width;
        this.craterCount = b.craterCount;
        this.craterDepth = b.craterDepth;
        this.craterRadius = b.craterRadius;
        this.bumpCount = b.bumpCount;
        this.bumpHeight = b.bumpHeight;
        this.bumpRadius = b.bumpRadius;

        // Defensive copy, allow null for new/randomize
        this.craterCenters = (b.craterCenters != null)
            ? Collections.unmodifiableList(new ArrayList<>(b.craterCenters))
            : null;
        this.bumpCenters = (b.bumpCenters != null)
            ? Collections.unmodifiableList(new ArrayList<>(b.bumpCenters))
            : null;
    }

    public double getLength() { return length; }
    public double getWidth() { return width; }
    public int getCraterCount() { return craterCount; }
    public double getCraterDepth() { return craterDepth; }
    public double getCraterRadius() { return craterRadius; }
    public int getBumpCount() { return bumpCount; }
    public double getBumpHeight() { return bumpHeight; }
    public double getBumpRadius() { return bumpRadius; }
    public List<double[]> getCraterCenters() { return craterCenters; }
    public List<double[]> getBumpCenters() { return bumpCenters; }
}
