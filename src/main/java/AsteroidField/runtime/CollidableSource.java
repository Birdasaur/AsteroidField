package AsteroidField.runtime;

import java.util.List;
import javafx.scene.Node;

public interface CollidableSource {
    List<Node> getCollidables();
}