package AsteroidField.spacecraft.collision;

import javafx.scene.shape.MeshView;

/**
 * Holder for a simplified collider representation.
 * For Phase 1 it simply wraps the original MeshView; later we’ll swap
 * to a decimated mesh/convex hull without changing call sites.
 */
public final class ColliderLOD {
    private final MeshView colliderMeshView; // scene graph-free MeshView preferred

    public ColliderLOD(MeshView colliderMeshView) {
        this.colliderMeshView = colliderMeshView;
    }

    public MeshView meshView() { return colliderMeshView; }
}
