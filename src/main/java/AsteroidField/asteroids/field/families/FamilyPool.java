package AsteroidField.asteroids.field.families;

import AsteroidField.asteroids.providers.AsteroidMeshProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Weighted random selection among enabled families. */
public final class FamilyPool {
    private final List<WeightedFamilyEntry> entries;

    public FamilyPool(List<WeightedFamilyEntry> entries) {
        this.entries = new ArrayList<WeightedFamilyEntry>(entries);
    }

    public AsteroidMeshProvider pick(Random rng) {
        double total = 0.0;
        for (WeightedFamilyEntry e : entries) {
            if (e.isEnabled()) total += e.getWeight();
        }
        if (total <= 0) throw new IllegalStateException("No asteroid families enabled.");

        double r = rng.nextDouble() * total;
        double acc = 0.0;
        for (WeightedFamilyEntry e : entries) {
            if (!e.isEnabled()) continue;
            acc += e.getWeight();
            if (r <= acc) return e.provider();
        }
        return entries.get(entries.size() - 1).provider();
    }

    public List<WeightedFamilyEntry> entries() { return entries; }
}
