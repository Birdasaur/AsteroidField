package AsteroidField;

import javafx.scene.Node;
import java.util.function.Consumer;

public interface AsteroidFamilyUI {
    /**
     * Create the dynamic controls UI for this asteroid family.
     * Call onChange.accept(newParams) whenever a parameter changes.
     */
    Node createDynamicControls(AsteroidParameters params, Consumer<AsteroidParameters> onChange);

    /**
     * Update all UI controls to reflect the values in params (e.g., for preset loading).
     */
    void setControlsFromParams(AsteroidParameters params);

    /**
     * Build and return the current AsteroidParameters object from the state of the controls.
     */
    AsteroidParameters getParamsFromControls();

    /**
     * Return a new AsteroidParameters instance representing the default for this family.
     * Useful when switching to this family for the first time.
     */
    AsteroidParameters getDefaultParameters();

    /**
     * Optional: Build sensible parameters for this family based on previous family params.
     * Default: returns getDefaultParameters(), but can be overridden for smarter transfer.
     */
    default AsteroidParameters buildDefaultParamsFrom(AsteroidParameters previous) {
        return getDefaultParameters();
    }

    /**
     * Human-friendly name for this family (for combo boxes, etc.)
     */
    default String getDisplayName() {
        return getClass().getSimpleName();
    }
}
