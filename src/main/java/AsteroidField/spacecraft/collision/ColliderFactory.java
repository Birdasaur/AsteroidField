package AsteroidField.spacecraft.collision;

import javafx.scene.shape.MeshView;

/**
 * Strategy for producing (or retrieving) a ColliderBundle for a given MeshView.
 * Lets us plug in real simplification/BVH building later without touching callers.
 */
public interface ColliderFactory {
    ColliderKey keyFor(MeshView mv);

    /** Should return a shared (prototype) ColliderBundle for this key. */
    ColliderBundle getOrBuild(ColliderKey key, MeshView exemplar);
}
