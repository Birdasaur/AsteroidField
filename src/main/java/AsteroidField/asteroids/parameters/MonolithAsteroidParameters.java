package AsteroidField.asteroids.parameters;

/**
 * Parameters for Monolith asteroids (blocky core + procedural tail(s)), 
 * fully compatible with CubicMesh and CubicAsteroidParameters patterns.
 */
public class MonolithAsteroidParameters extends CubicAsteroidParameters {
    // Tail parameters
    private final boolean[] tailFaces; // order: +Z, -Z, +X, -X, +Y, -Y
    private final int tailSegments;
    private final double tailBlockScale;
    private final double tailSpread;
    private final double tailJitter;

    // --- Covariant Builder Pattern ---
    public static class Builder extends CubicAsteroidParameters.Builder<Builder> {
        private boolean[] tailFaces = new boolean[] {true, false, false, false, false, false};
        private int tailSegments = 4;
        private double tailBlockScale = 0.7;
        private double tailSpread = 0.28;
        private double tailJitter = 0.22;

        public Builder tailFaces(boolean[] arr) { 
            this.tailFaces = (arr != null && arr.length == 6) ? arr.clone() : new boolean[6]; 
            return self();
        }
        /** Set a tail on a specific face: 0=+Z, 1=-Z, 2=+X, 3=-X, 4=+Y, 5=-Y */
        public Builder tailFace(int faceIdx, boolean on) {
            if (tailFaces == null || tailFaces.length != 6) tailFaces = new boolean[6];
            if (faceIdx >= 0 && faceIdx < 6) tailFaces[faceIdx] = on;
            return self();
        }
        public Builder tailSegments(int n) { this.tailSegments = n; return self(); }
        public Builder tailBlockScale(double d) { this.tailBlockScale = d; return self(); }
        public Builder tailSpread(double d) { this.tailSpread = d; return self(); }
        public Builder tailJitter(double d) { this.tailJitter = d; return self(); }

        @Override
        public MonolithAsteroidParameters build() { return new MonolithAsteroidParameters(this); }
        @Override
        protected Builder self() { return this; }
    }

    private MonolithAsteroidParameters(Builder b) {
        super(b);
        this.tailFaces = (b.tailFaces != null && b.tailFaces.length == 6) ? b.tailFaces.clone() : new boolean[6];
        this.tailSegments = b.tailSegments;
        this.tailBlockScale = b.tailBlockScale;
        this.tailSpread = b.tailSpread;
        this.tailJitter = b.tailJitter;
    }

    /** Returns a length-6 array (true if a tail is present on face) in order: +Z, -Z, +X, -X, +Y, -Y */
    public boolean[] getTailFaces() { return tailFaces.clone(); }
    public boolean hasTail(int faceIdx) { return tailFaces != null && faceIdx >= 0 && faceIdx < 6 && tailFaces[faceIdx]; }
    public int getTailSegments() { return tailSegments; }
    public double getTailBlockScale() { return tailBlockScale; }
    public double getTailSpread() { return tailSpread; }
    public double getTailJitter() { return tailJitter; }

    @Override
    public Builder toBuilder() {
        return new Builder()
            .radius(getRadius())
            .subdivisions(getSubdivisions())
            .deformation(getDeformation())
            .seed(getSeed())
            .familyName(getFamilyName())
            .tailFaces(getTailFaces())
            .tailSegments(getTailSegments())
            .tailBlockScale(getTailBlockScale())
            .tailSpread(getTailSpread())
            .tailJitter(getTailJitter());
    }
}
