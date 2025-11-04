package AsteroidField.asteroids.field;

import AsteroidField.asteroids.parameters.AsteroidParameters;
import javafx.geometry.Point3D;
import javafx.scene.shape.MeshView;

/**
 * Immutable metadata wrapper for an asteroid MeshView in the field.
 */
public final class AsteroidInstance {
    private final MeshView node;
    private final String familyName;
    private final AsteroidParameters params;
    private final double approxRadius;
    private final Point3D position;

    public AsteroidInstance(MeshView node,
                            String familyName,
                            AsteroidParameters params,
                            double approxRadius,
                            Point3D position) {
        this.node = node;
        this.familyName = familyName;
        this.params = params;
        this.approxRadius = approxRadius;
        this.position = position;
    }

    public MeshView node() { return node; }
    public String familyName() { return familyName; }
    public AsteroidParameters params() { return params; }
    public double approxRadius() { return approxRadius; }
    public Point3D position() { return position; }
}
