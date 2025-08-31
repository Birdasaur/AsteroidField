package AsteroidField.asteroids;

import AsteroidField.asteroids.parameters.AsteroidParameters;
import AsteroidField.asteroids.providers.AsteroidMeshProvider;
import javafx.scene.shape.TriangleMesh;

public class AsteroidGenerator {
    private final AsteroidMeshProvider meshProvider;
    private final AsteroidParameters params;

    public AsteroidGenerator(AsteroidMeshProvider meshProvider, AsteroidParameters params) {
        this.meshProvider = meshProvider;
        this.params = params;
    }

    public TriangleMesh generateAsteroid() {
        return meshProvider.generateMesh(params);
    }

    public AsteroidParameters getParameters() { return params; }
    public AsteroidMeshProvider getMeshProvider() { return meshProvider; }
}