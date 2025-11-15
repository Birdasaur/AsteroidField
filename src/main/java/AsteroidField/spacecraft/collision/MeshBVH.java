package AsteroidField.spacecraft.collision;

/**
 * Per-mesh BVH placeholder. Flat arrays are cache-friendly and refittable.
 * We’ll wire traversal later; for now it’s an optional field in ColliderBundle.
 */
public final class MeshBVH {
    // Minimal stub for now; real implementation will fill these.
    public final float[] minX, minY, minZ, maxX, maxY, maxZ;
    public final int[] left, right, firstTri, triCount;

    public MeshBVH(float[] minX, float[] minY, float[] minZ,
                   float[] maxX, float[] maxY, float[] maxZ,
                   int[] left, int[] right, int[] firstTri, int[] triCount) {
        this.minX = minX; this.minY = minY; this.minZ = minZ;
        this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
        this.left = left; this.right = right;
        this.firstTri = firstTri; this.triCount = triCount;
    }

    public int nodeCount() { return (minX == null) ? 0 : minX.length; }
}
