package AsteroidField;

import java.util.LinkedHashMap;
import java.util.Map;
import javafx.scene.shape.TriangleMesh;

public interface AsteroidMeshProvider {
    TriangleMesh generateMesh(AsteroidParameters params);

    // Default implementation: deformed icosphere
    public static class Default implements AsteroidMeshProvider {
        @Override
        public TriangleMesh generateMesh(AsteroidParameters params) {
            // Real implementation comes next section!
            return IcosphereDeformer.generate(params);
        }
    }
    
    // Ordered mapping: name → provider
    public static final Map<String, AsteroidMeshProvider> PROVIDERS = createProviders();

    private static Map<String, AsteroidMeshProvider> createProviders() {
        Map<String, AsteroidMeshProvider> map = new LinkedHashMap<>();
        map.put("Classic Rocky", new AsteroidMeshProvider.Default());
        map.put("Cubic", new CubicAsteroidMeshProvider());
        map.put("Spiky", new SpikyAsteroidMeshProvider());
// map.put("Cratered", new CrateredAsteroidMeshProvider());
        return map;
    }
}
