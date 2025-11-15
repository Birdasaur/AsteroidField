package AsteroidField.spacecraft.collision;

import javafx.scene.shape.MeshView;

/** Per-instance binding to a shared ColliderBundle, plus instance flags if needed. */
public final class InstanceEntry {
    private final MeshView sourceMeshView;     // the render mesh instance
    private final ColliderKey key;             // points into the prototype map
    private final ColliderBundle bundle;       // shared collider data

    public InstanceEntry(MeshView sourceMeshView, ColliderKey key, ColliderBundle bundle) {
        this.sourceMeshView = sourceMeshView;
        this.key = key;
        this.bundle = bundle;
    }

    public MeshView sourceMeshView() { return sourceMeshView; }
    public ColliderKey key() { return key; }
    public ColliderBundle bundle() { return bundle; }
}
