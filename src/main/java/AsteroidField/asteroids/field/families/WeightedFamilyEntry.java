package AsteroidField.asteroids.field.families;

import AsteroidField.asteroids.providers.AsteroidMeshProvider;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;

/** Toggleable, weighted entry for a provider. */
public final class WeightedFamilyEntry {
    private final AsteroidMeshProvider provider;
    private final BooleanProperty enabled = new SimpleBooleanProperty(true);
    private final DoubleProperty weight = new SimpleDoubleProperty(1.0);

    public WeightedFamilyEntry(AsteroidMeshProvider provider) {
        this.provider = provider;
    }

    public AsteroidMeshProvider provider() { return provider; }
    public String displayName() { return provider.getDisplayName(); }

    public BooleanProperty enabledProperty() { return enabled; }
    public DoubleProperty weightProperty() { return weight; }

    public boolean isEnabled() { return enabled.get(); }
    public double getWeight() { return weight.get(); }
}
