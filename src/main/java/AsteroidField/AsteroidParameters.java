package AsteroidField;

public class AsteroidParameters {
    protected double radius;
    protected int subdivisions;
    protected double deformation;
    protected long seed;
    protected String familyName;

    public static class Builder<T extends Builder<T>> {
        protected double radius = 100.0;
        protected int subdivisions = 2;
        protected double deformation = 0.3;
        protected long seed = 42L;
        protected String familyName = "Default";

        public T radius(double r) { this.radius = r; return self(); }
        public T subdivisions(int s) { this.subdivisions = s; return self(); }
        public T deformation(double d) { this.deformation = d; return self(); }
        public T seed(long s) { this.seed = s; return self(); }
        public T familyName(String name) { this.familyName = name; return self(); }

        protected T self() { return (T) this; }

        public AsteroidParameters build() { return new AsteroidParameters(this); }
    }

    protected AsteroidParameters(Builder<?> builder) {
        this.radius = builder.radius;
        this.subdivisions = builder.subdivisions;
        this.deformation = builder.deformation;
        this.seed = builder.seed;
        this.familyName = builder.familyName;
    }

    // Getters
    public double getRadius() { return radius; }
    public int getSubdivisions() { return subdivisions; }
    public double getDeformation() { return deformation; }
    public long getSeed() { return seed; }
    public String getFamilyName() { return familyName; }
}
