package AsteroidField;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.scene.Node;
import javafx.scene.shape.TriangleMesh;

public interface AsteroidMeshProvider {
    TriangleMesh generateMesh(AsteroidParameters params);
    
    /** 
     * For families with custom controls, override to provide dynamic controls. 
     * @param onChange
     * @param current
     * @return by default emptyList() which will tell the parent GUI not to render anything
     */
    default List<Node> createParameterControls(Consumer<AsteroidParameters> onChange, AsteroidParameters current) {
        return java.util.Collections.emptyList();
    }
    
    // For convenience, override so a display name can be supplied via a method 
    default String getDisplayName() { return getClass().getSimpleName(); }
    
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
        map.put("Crystalline", new CrystallineAsteroidMeshProvider());
        map.put("Cubic", new CubicAsteroidMeshProvider());
        map.put("Spiky", new SpikyAsteroidMeshProvider());
        map.put("Cratered", new CrateredAsteroidMeshProvider());
        map.put("Capsule", new CapsuleAsteroidMeshProvider());

        return map;
    }
}
