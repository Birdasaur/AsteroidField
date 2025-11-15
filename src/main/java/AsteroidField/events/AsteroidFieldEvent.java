package AsteroidField.events;

import AsteroidField.asteroids.field.AsteroidField;
import AsteroidField.asteroids.field.AsteroidFieldGenerator;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * Event signaling asteroid-field lifecycle + requests.
 *
 * Types:
 *  - ATTACHED:          field created & attached (payload: AsteroidField)
 *  - DETACHED:          field fully removed
 *  - REGENERATE_REQUEST request to (re)build & attach a field (payload: Config)
 *  - CLEAR_REQUEST:     request to remove the current field
 *
 * Minimal payload surface to keep systems decoupled.
 */
public final class AsteroidFieldEvent extends Event {

    // Base
    public static final EventType<AsteroidFieldEvent> ANY =
            new EventType<>(Event.ANY, "ASTEROID_FIELD_ANY");

    // Lifecycle
    public static final EventType<AsteroidFieldEvent> ATTACHED =
            new EventType<>(ANY, "ASTEROID_FIELD_ATTACHED");
    public static final EventType<AsteroidFieldEvent> DETACHED =
            new EventType<>(ANY, "ASTEROID_FIELD_DETACHED");

    // Requests
    public static final EventType<AsteroidFieldEvent> REGENERATE_REQUEST =
            new EventType<>(ANY, "ASTEROID_FIELD_REGENERATE_REQUEST");
    public static final EventType<AsteroidFieldEvent> CLEAR_REQUEST =
            new EventType<>(ANY, "ASTEROID_FIELD_CLEAR_REQUEST");

    // Optional payloads
    private final AsteroidField field;                       // ATTACHED only
    private final AsteroidFieldGenerator.Config config;      // REGENERATE_REQUEST only

    private AsteroidFieldEvent(Object source, EventTarget target,
                               EventType<? extends Event> type,
                               AsteroidField field,
                               AsteroidFieldGenerator.Config config) {
        super(source, target, type);
        this.field = field;
        this.config = config;
    }

    // Factories
    public static AsteroidFieldEvent attached(Object source, EventTarget target, AsteroidField field) {
        return new AsteroidFieldEvent(source, target, ATTACHED, field, null);
    }

    public static AsteroidFieldEvent detached(Object source, EventTarget target) {
        return new AsteroidFieldEvent(source, target, DETACHED, null, null);
    }

    public static AsteroidFieldEvent regenerateRequest(Object source, EventTarget target,
                                                       AsteroidFieldGenerator.Config cfg) {
        return new AsteroidFieldEvent(source, target, REGENERATE_REQUEST, null, cfg);
    }

    public static AsteroidFieldEvent clearRequest(Object source, EventTarget target) {
        return new AsteroidFieldEvent(source, target, CLEAR_REQUEST, null, null);
    }

    // Accessors
    /** Present only for ATTACHED; null otherwise. */
    public AsteroidField getField() { return field; }

    /** Present only for REGENERATE_REQUEST; null otherwise. */
    public AsteroidFieldGenerator.Config getConfig() { return config; }

    @Override
    public AsteroidFieldEvent copyFor(Object newSource, EventTarget newTarget) {
        return new AsteroidFieldEvent(newSource, newTarget, getEventType(), field, config);
    }

    @Override
    public EventType<? extends AsteroidFieldEvent> getEventType() {
        return (EventType<? extends AsteroidFieldEvent>) super.getEventType();
    }

    @Override
    public String toString() {
        return "AsteroidFieldEvent{type=" + getEventType()
                + ", field=" + (field != null)
                + ", hasConfig=" + (config != null)
                + '}';
    }
}
