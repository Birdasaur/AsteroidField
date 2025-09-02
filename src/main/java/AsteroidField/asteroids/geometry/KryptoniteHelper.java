package AsteroidField.asteroids.geometry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;


public class KryptoniteHelper {

    /**
     * Samples random base positions for a cluster in a disk/cap on the asteroid surface.
     * @param vertsList  List<float[]> vertex positions from the Icosphere.
     * @param clusterDir double[3] normalized direction for cluster center.
     * @param capAngleRadians How wide the cluster is, in radians (e.g., Math.toRadians(22.5) for tight).
     * @param count      How many crystal bases to sample.
     * @param rng        Random source.
     * @return           List of float[] positions (from vertsList) in the disk region.
     */
    public static List<float[]> sampleClusterOnIcosphere(
            List<float[]> vertsList, double[] clusterDir, double capAngleRadians, int count, Random rng)
    {
        List<float[]> candidates = new ArrayList<>();
        for (float[] v : vertsList) {
            double len = Math.sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2]);
            double[] dir = new double[]{v[0]/len, v[1]/len, v[2]/len};
            double dot = dir[0]*clusterDir[0] + dir[1]*clusterDir[1] + dir[2]*clusterDir[2];
            double angle = Math.acos(Math.max(-1, Math.min(1, dot)));
            if (angle < capAngleRadians) {
                candidates.add(v);
            }
        }
        if (candidates.isEmpty()) {
            System.out.println("KryptoniteHelper: No vertices found in cap for clusterDir=" +
                Arrays.toString(clusterDir) + " angle=" + capAngleRadians);
            return new ArrayList<>();
        }
        // Randomize and select count (or all available)
        Collections.shuffle(candidates, rng);
        if (candidates.size() > count) {
            return new ArrayList<>(candidates.subList(0, count));
        }
        return candidates;
    }

    /**
     * Utility to generate N random unit vectors (for multiple clusters).
     */
    public static List<double[]> generateRandomClusterCenters(int numClusters, Random rng) {
        List<double[]> dirs = new ArrayList<>(numClusters);
        for (int i = 0; i < numClusters; i++) {
            double theta = rng.nextDouble() * 2 * Math.PI;
            double phi = Math.acos(2 * rng.nextDouble() - 1);
            double x = Math.sin(phi) * Math.cos(theta);
            double y = Math.sin(phi) * Math.sin(theta);
            double z = Math.cos(phi);
            dirs.add(new double[]{x, y, z});
        }
        return dirs;
    }

    /**
     * For a cluster, get the (approximate) centroid for axis orientation.
     */
    public static double[] clusterCentroid(List<float[]> clusterBases) {
        double x = 0, y = 0, z = 0;
        for (float[] v : clusterBases) {
            x += v[0]; y += v[1]; z += v[2];
        }
        int n = clusterBases.size();
        return new double[]{x/n, y/n, z/n};
    }

    /**
     * For each base, compute an axis vector pointing away from the cluster centroid,
     * with random tilt added.
     */
    public static double[] computeCrystalAxis(float[] base, double[] centroid, double tiltRadians, Random rng) {
        double vx = base[0] - centroid[0];
        double vy = base[1] - centroid[1];
        double vz = base[2] - centroid[2];
        double len = Math.sqrt(vx*vx + vy*vy + vz*vz);
        if (len < 1e-6) len = 1.0;
        double[] axis = new double[]{vx/len, vy/len, vz/len};
        // Add random tilt
        return CrystalHelper.randomTiltedAxis(axis, tiltRadians, rng);
    }
}
