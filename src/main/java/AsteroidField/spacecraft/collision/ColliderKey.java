package AsteroidField.spacecraft.collision;

import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import java.util.Objects;

/** Stable key that identifies a collider prototype (shared across instances). */
public final class ColliderKey {
    private final String id;

    private ColliderKey(String id) { this.id = id; }

    /** Preferred: derive a key from app-level prototype id if present. */
    public static ColliderKey fromPrototypeId(String protoId, String lodParams) {
        return new ColliderKey(protoId + "|" + (lodParams == null ? "" : lodParams));
    }

    /** Fallback: derive from underlying mesh identity + LOD params. */
    public static ColliderKey fromMeshView(MeshView mv, String lodParams) {
        String meshId = "unknownMesh";
        if (mv.getMesh() instanceof TriangleMesh tm) {
            meshId = Integer.toHexString(System.identityHashCode(tm))
                    + ":" + (tm.getPoints() == null ? 0 : tm.getPoints().size())
                    + "x" + (tm.getFaces() == null ? 0 : tm.getFaces().size());
        }
        return new ColliderKey(meshId + "|" + (lodParams == null ? "" : lodParams));
    }

    public String id() { return id; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ColliderKey)) return false;
        ColliderKey that = (ColliderKey) o;
        return Objects.equals(id, that.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() { return id; }
}
