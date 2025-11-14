package AsteroidField.events;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/**
 * Simple application-level event for broadcasting actions
 * such as showing or closing overlay panes.
 *
 * Two generic payload slots (object, object2) can carry any data
 * you need—e.g. text content and flags for streaming updates.
 */
public class ApplicationEvent extends Event {
    private static final long serialVersionUID = 1L;

    /** Base event type for all custom application events. */
    public static final EventType<ApplicationEvent> ANY =
            new EventType<>(Event.ANY, "APPLICATION_EVENT");

    /** event types for overlay management. */
    public static final EventType<ApplicationEvent> SHOW_TEXT_CONSOLE = new EventType<>(ANY, "SHOW_TEXT_CONSOLE");
    public static final EventType<ApplicationEvent> CLOSE_TEXT_CONSOLE = new EventType<>(ANY, "CLOSE_TEXT_CONSOLE");
public static final EventType<ApplicationEvent> SHOW_ASTEROID_LOD = new EventType<>(ANY, "SHOW_ASTEROID_LOD");
public static final EventType<ApplicationEvent> CLOSE_ASTEROID_LOD = new EventType<>(ANY, "CLOSE_ASTEROID_LOD");
    public static final EventType<ApplicationEvent> SHOW_FIELD_MANAGER = new EventType<>(ANY, "SHOW_FIELD_MANAGER");
    public static final EventType<ApplicationEvent> CLOSE_FIELD_MANAGER = new EventType<>(ANY, "CLOSE_FIELD_MANAGER");

    public static final EventType<ApplicationEvent> SHOW_BUSY_INDICATOR = new EventType(ANY, "SHOW_BUSY_INDICATOR");
    public static final EventType<ApplicationEvent> HIDE_BUSY_INDICATOR = new EventType(ANY, "HIDE_BUSY_INDICATOR");
    public static final EventType<ApplicationEvent> UPDATE_BUSY_INDICATOR = new EventType(ANY, "PROGRESS_BUSY_INDICATOR");
    public static final EventType<ApplicationEvent> SHOW_ABOUT = new EventType(ANY, "SHOW_ABOUT");
    public static final EventType<ApplicationEvent> SHUTDOWN = new EventType(ANY, "SHUTDOWN");
    public static final EventType<ApplicationEvent> RESTORE_PANES = new EventType(ANY, "RESTORE_PANES");

    /** Generic payload fields. */
    public final Object object;
    public final Object object2;

    /** Create a new ApplicationEvent with no payload. */
    public ApplicationEvent(EventType<? extends ApplicationEvent> eventType) {
        this(eventType, null, null);
    }

    /** Create a new ApplicationEvent with one payload object. */
    public ApplicationEvent(EventType<? extends ApplicationEvent> eventType,
                            Object object) {
        this(eventType, object, null);
    }

    /** Create a new ApplicationEvent with two payload objects. */
    public ApplicationEvent(EventType<? extends ApplicationEvent> eventType,
                            Object object, Object object2) {
        super(eventType);
        this.object = object;
        this.object2 = object2;
    }

    /** Full constructor allowing explicit source/target. */
    public ApplicationEvent(Object source, EventTarget target,
                            EventType<? extends ApplicationEvent> eventType,
                            Object object, Object object2) {
        super(source, target, eventType);
        this.object = object;
        this.object2 = object2;
    }

    @Override
    public ApplicationEvent copyFor(Object newSource, EventTarget newTarget) {
        return new ApplicationEvent(newSource, newTarget, getEventType(),
                                    object, object2);
    }

    @Override
    public EventType<? extends ApplicationEvent> getEventType() {
        return (EventType<? extends ApplicationEvent>) super.getEventType();
    }
}
