package AsteroidField.spacecraft.collision;

import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Phase-1 factory: uses the original mesh as the collider (no simplification),
 * no BVH yet. Thread-safe map so we can preload on background thread later.
 */
public final class DefaultColliderFactory implements ColliderFactory {
    private final Map<ColliderKey, ColliderBundle> protoMap = new ConcurrentHashMap<>();
    private final String lodParams; // placeholder for future decimation settings

    public DefaultColliderFactory() { this(""); }
    public DefaultColliderFactory(String lodParams) { this.lodParams = lodParams == null ? "" : lodParams; }

    @Override
    public ColliderKey keyFor(MeshView mv) {
        // If you have app-level prototype IDs, use ColliderKey.fromPrototypeId(..)
        return ColliderKey.fromMeshView(mv, lodParams);
    }

    @Override
    public ColliderBundle getOrBuild(ColliderKey key, MeshView exemplar) {
        return protoMap.computeIfAbsent(key, k -> {
            // Phase-1: wrap the existing mesh as the collider LOD
            MeshView colliderView = new MeshView(exemplar.getMesh());
            colliderView.getTransforms().setAll(exemplar.getTransforms()); // keep local same if needed
            int tris = 0;
            if (exemplar.getMesh() instanceof TriangleMesh tm && tm.getFaces() != null) {
                tris = tm.getFaces().size() / 6; // JavaFX stores 6 ints per tri (p0/t0, p1/t1, p2/t2)
            }
            return new ColliderBundle(new ColliderLOD(colliderView), null, tris);
        });
    }
}
