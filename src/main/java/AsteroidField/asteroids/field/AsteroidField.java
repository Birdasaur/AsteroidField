package AsteroidField.asteroids.field;

import java.util.List;
import javafx.scene.Group;

/**
 * Container that owns the Group of MeshViews plus metadata for each asteroid instance.
 */
public final class AsteroidField {
    public final Group root = new Group();
    public final List<AsteroidInstance> instances;

    public AsteroidField(List<AsteroidInstance> instances) {
        this.instances = instances;
        for (AsteroidInstance ai : instances) {
            root.getChildren().add(ai.node());
        }
    }
}
