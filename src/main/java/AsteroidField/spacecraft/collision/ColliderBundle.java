package AsteroidField.spacecraft.collision;

/** Shared prototype data: LOD collider + (optional) BVH + static metadata. */
public final class ColliderBundle {
    private final ColliderLOD lod;
    private final MeshBVH bvh;       // null until we add BVH building
    private final int triCountHint;  // useful for perf logs

    public ColliderBundle(ColliderLOD lod, MeshBVH bvh, int triCountHint) {
        this.lod = lod; this.bvh = bvh; this.triCountHint = triCountHint;
    }

    public ColliderLOD lod() { return lod; }
    public MeshBVH bvh() { return bvh; }
    public int triCountHint() { return triCountHint; }
}
