package AsteroidField.events;

import AsteroidField.asteroids.field.AsteroidField;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * Minimal event signaling Asteroid Field lifecycle.
 *
 * Types:
 *  - ATTACHED:      field created & attached (includes the AsteroidField)
 *  - DETACHED:      field fully removed
 *  - RESET_REQUEST: request to clear & re-register LOD state
 */
public final class AsteroidFieldEvent extends Event {

    public static final EventType<AsteroidFieldEvent> ANY =
            new EventType<>(Event.ANY, "ASTEROID_FIELD_ANY");
    public static final EventType<AsteroidFieldEvent> ATTACHED =
            new EventType<>(ANY, "ASTEROID_FIELD_ATTACHED");
    public static final EventType<AsteroidFieldEvent> DETACHED =
            new EventType<>(ANY, "ASTEROID_FIELD_DETACHED");
    public static final EventType<AsteroidFieldEvent> RESET_REQUEST =
            new EventType<>(ANY, "ASTEROID_FIELD_RESET_REQUEST");

    private final AsteroidField field; // only present for ATTACHED

    private AsteroidFieldEvent(Object source, EventTarget target,
                               EventType<? extends Event> type,
                               AsteroidField field) {
        super(source, target, type);
        this.field = field;
    }

    /** ATTACHED event carrying the newly attached field. */
    public static AsteroidFieldEvent attached(Object source, EventTarget target, AsteroidField field) {
        return new AsteroidFieldEvent(source, target, ATTACHED, field);
    }

    /** DETACHED event, no payload. */
    public static AsteroidFieldEvent detached(Object source, EventTarget target) {
        return new AsteroidFieldEvent(source, target, DETACHED, null);
    }

    /** RESET_REQUEST event, no payload. */
    public static AsteroidFieldEvent resetRequest(Object source, EventTarget target) {
        return new AsteroidFieldEvent(source, target, RESET_REQUEST, null);
    }

    /** Present only for ATTACHED; null for others. */
    public AsteroidField getField() {
        return field;
    }

    @Override
    public AsteroidFieldEvent copyFor(Object newSource, EventTarget newTarget) {
        return new AsteroidFieldEvent(newSource, newTarget, getEventType(), field);
    }

    @Override
    public EventType<? extends AsteroidFieldEvent> getEventType() {
        return (EventType<? extends AsteroidFieldEvent>) super.getEventType();
    }

    @Override
    public String toString() {
        return "AsteroidFieldEvent{" +
                "type=" + getEventType() +
                ", field=" + (field == null ? "null" : "present") +
                '}';
    }
}
