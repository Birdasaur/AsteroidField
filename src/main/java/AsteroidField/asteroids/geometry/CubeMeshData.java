package AsteroidField.asteroids.geometry;

import java.util.List;

/**
 *
 * Data container for mesh building
 */
public class CubeMeshData {
        public float[] vertsArray;
        public List<float[]> vertsList;
        public int[] facesArray;
        public List<int[]> facesList;
        
        public CubeMeshData(float[] va, List<float[]> vl, int[] fa, List<int[]> fl) {
            vertsArray = va; vertsList = vl; facesArray = fa; facesList = fl;
        }
    }
