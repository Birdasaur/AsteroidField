package AsteroidField.asteroids.geometry;

    // Helper: Pair for vert/texCoord indices in each face
    public class FaceRef {
        int vi0, ti0, vi1, ti1, vi2, ti2;
        FaceRef(int vi0, int ti0, int vi1, int ti1, int vi2, int ti2) {
            this.vi0 = vi0; this.ti0 = ti0; this.vi1 = vi1; this.ti1 = ti1; this.vi2 = vi2; this.ti2 = ti2;
        }
    }
