package AsteroidField.textures;

import java.io.InputStream;
import java.net.URL;

/**
 * Provides convenient access to texture image resources in the classpath.
 * Example usage:
 *   Image img = new Image(TextureResourceProvider.getResource("/textures/stars_atlas_2k.png").toExternalForm());
 */
public class TextureResourceProvider {

    /** Returns an InputStream to read the texture resource directly. */
    public static InputStream getResourceAsStream(String name) {
        return TextureResourceProvider.class.getResourceAsStream(name);
    }

    /** Returns a URL pointing to the texture resource (useful for JavaFX Image). */
    public static URL getResource(String name) {
        return TextureResourceProvider.class.getResource(name);
    }
}
