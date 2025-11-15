package AsteroidField.events;

import AsteroidField.asteroids.field.AsteroidField;
import AsteroidField.asteroids.field.AsteroidFieldGenerator;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;

/**
 * Event signaling asteroid-field lifecycle + requests.
 *
 * Types:
 *  - ATTACHED:            field created & attached (payload: AsteroidField)
 *  - DETACHED:            field fully removed
 *  - REGENERATE_REQUEST:  request to (re)build & attach a field (payload: Config)
 *  - CLEAR_REQUEST:       request to remove the current field
 *  - RENDER_MODE_REQUEST: request to set MeshView draw mode (payload: DrawMode)
 *  - CULLFACE_REQUEST:    request to set MeshView cull face (payload: CullFace)
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

    // Requests (field lifecycle)
    public static final EventType<AsteroidFieldEvent> REGENERATE_REQUEST =
            new EventType<>(ANY, "ASTEROID_FIELD_REGENERATE_REQUEST");
    public static final EventType<AsteroidFieldEvent> CLEAR_REQUEST =
            new EventType<>(ANY, "ASTEROID_FIELD_CLEAR_REQUEST");

    // Requests (render debug)
    public static final EventType<AsteroidFieldEvent> RENDER_MODE_REQUEST =
            new EventType<>(ANY, "ASTEROID_FIELD_RENDER_MODE_REQUEST");
    public static final EventType<AsteroidFieldEvent> CULLFACE_REQUEST =
            new EventType<>(ANY, "ASTEROID_FIELD_CULLFACE_REQUEST");

    // Optional payloads
    private final AsteroidField field;                       // ATTACHED only
    private final AsteroidFieldGenerator.Config config;      // REGENERATE_REQUEST only
    private final DrawMode drawMode;                         // RENDER_MODE_REQUEST only
    private final CullFace cullFace;                         // CULLFACE_REQUEST only

    private AsteroidFieldEvent(Object source, EventTarget target,
                               EventType<? extends Event> type,
                               AsteroidField field,
                               AsteroidFieldGenerator.Config config,
                               DrawMode drawMode,
                               CullFace cullFace) {
        super(source, target, type);
        this.field = field;
        this.config = config;
        this.drawMode = drawMode;
        this.cullFace = cullFace;
    }

    // ----- Factories (lifecycle) -----
    public static AsteroidFieldEvent attached(Object source, EventTarget target, AsteroidField field) {
        return new AsteroidFieldEvent(source, target, ATTACHED, field, null, null, null);
    }

    public static AsteroidFieldEvent detached(Object source, EventTarget target) {
        return new AsteroidFieldEvent(source, target, DETACHED, null, null, null, null);
    }

    public static AsteroidFieldEvent regenerateRequest(Object source, EventTarget target,
                                                       AsteroidFieldGenerator.Config cfg) {
        return new AsteroidFieldEvent(source, target, REGENERATE_REQUEST, null, cfg, null, null);
    }

    public static AsteroidFieldEvent clearRequest(Object source, EventTarget target) {
        return new AsteroidFieldEvent(source, target, CLEAR_REQUEST, null, null, null, null);
    }

    // ----- Factories (render debug) -----
    public static AsteroidFieldEvent renderModeRequest(Object source, EventTarget target, DrawMode mode) {
        return new AsteroidFieldEvent(source, target, RENDER_MODE_REQUEST, null, null, mode, null);
    }

    public static AsteroidFieldEvent cullFaceRequest(Object source, EventTarget target, CullFace face) {
        return new AsteroidFieldEvent(source, target, CULLFACE_REQUEST, null, null, null, face);
    }

    // ----- Accessors -----
    /** Present only for ATTACHED; null otherwise. */
    public AsteroidField getField() { return field; }

    /** Present only for REGENERATE_REQUEST; null otherwise. */
    public AsteroidFieldGenerator.Config getConfig() { return config; }

    /** Present only for RENDER_MODE_REQUEST; null otherwise. */
    public DrawMode getDrawMode() { return drawMode; }

    /** Present only for CULLFACE_REQUEST; null otherwise. */
    public CullFace getCullFace() { return cullFace; }

    @Override
    public AsteroidFieldEvent copyFor(Object newSource, EventTarget newTarget) {
        return new AsteroidFieldEvent(newSource, newTarget, getEventType(), field, config, drawMode, cullFace);
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
                + ", drawMode=" + (drawMode != null ? drawMode : "null")
                + ", cullFace=" + (cullFace != null ? cullFace : "null")
                + '}';
    }
}
