// AsteroidField.runtime.world.CollidableRegistry
package AsteroidField.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.application.Platform;
import javafx.scene.Node;

/** Runtime-owned list of collidable scene nodes (FX thread usage). */
public final class CollidableRegistry implements CollidableSource {
    private final List<Node> nodes = new ArrayList<>();

    @Override
    public List<Node> getCollidables() {
        return Collections.unmodifiableList(nodes);
    }

    public void add(Node n) {
        if (n == null) return;
        assertFx();
        nodes.add(n);
    }

    public void addAll(List<? extends Node> ns) {
        if (ns == null) return;
        assertFx();
        nodes.addAll(ns);
    }

    public void remove(Node n) {
        assertFx();
        nodes.remove(n);
    }

    public void clear() {
        assertFx();
        nodes.clear();
    }

    private static void assertFx() {
        // Optional safety; remove if you don’t want the check.
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("CollidableRegistry must be mutated on the FX thread.");
        }
    }
}
