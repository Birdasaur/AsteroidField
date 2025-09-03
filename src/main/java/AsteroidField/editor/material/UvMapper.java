package AsteroidField.editor.material;

import javafx.scene.shape.TriangleMesh;

/** Caches per-face UVs for quick lookup. */
public final class UvMapper {
    public static record FaceUV(double u0, double v0, double u1, double v1, double u2, double v2) {}

    private final FaceUV[] faces;

    public UvMapper(TriangleMesh mesh) {
        var facesArr = mesh.getFaces();
        var tex = mesh.getTexCoords();
        int faceCount = facesArr.size() / 6;
        faces = new FaceUV[faceCount];
        for (int fi = 0; fi < faceCount; fi++) {
            int base = fi * 6;
            int t0 = facesArr.get(base + 1);
            int t1 = facesArr.get(base + 3);
            int t2 = facesArr.get(base + 5);
            int t0i = t0 * 2, t1i = t1 * 2, t2i = t2 * 2;
            double u0 = tex.get(t0i),     v0 = tex.get(t0i + 1);
            double u1 = tex.get(t1i),     v1 = tex.get(t1i + 1);
            double u2 = tex.get(t2i),     v2 = tex.get(t2i + 1);
            faces[fi] = new FaceUV(u0, v0, u1, v1, u2, v2);
        }
    }

    public FaceUV get(int faceIndex) {
        if (faceIndex < 0 || faceIndex >= faces.length) return null;
        return faces[faceIndex];
    }

    public int faceCount() { return faces.length; }
}
