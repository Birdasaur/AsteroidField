package AsteroidField.field.placement;

import java.util.List;
import java.util.Random;
import javafx.geometry.Point3D;
import javafx.scene.Node;

/** Strategy to place asteroids in space. */
public interface PlacementStrategy {
    String getName();
    Node getControls(); // JavaFX controls node for live tuning

    List<Placement> generate(int count, Random rng);

    /** Immutable placement tuple. */
    public static final class Placement {
        private final Point3D position;
        private final Point3D forward;
        private final Point3D up;
        private final double baseScale;

        public Placement(Point3D position, Point3D forward, Point3D up, double baseScale) {
            this.position = position;
            this.forward = forward;
            this.up = up;
            this.baseScale = baseScale;
        }
        public Point3D getPosition() { return position; }
        public Point3D getForward() { return forward; }
        public Point3D getUp() { return up; }
        public double getBaseScale() { return baseScale; }
    }
}
