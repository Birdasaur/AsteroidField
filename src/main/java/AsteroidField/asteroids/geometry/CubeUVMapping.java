package AsteroidField.asteroids.geometry;

/**
 *
 * Data container for mesh building
 */
public class CubeUVMapping {
    public float[] uvArray;
    public int[] faceArray;
    public CubeUVMapping(float[] uva, int[] fa) { 
        uvArray = uva; 
        faceArray = fa; 
    }
}
