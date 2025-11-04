package AsteroidField.events;

import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

/** Media-layer events (music/SFX control). */
public class AudioEvent extends Event {
    private static final long serialVersionUID = 1L;

    /** Base type for all audio events. */
    public static final EventType<AudioEvent> ANY =
            new EventType<>(Event.ANY, "AUDIO_EVENT");

    // Music control
    public static final EventType<AudioEvent> NEW_AUDIO_FILE =
            new EventType<>(ANY, "NEW_AUDIO_FILE");
    public static final EventType<AudioEvent> PLAY_MUSIC_TRACK =
            new EventType<>(ANY, "PLAY_MUSIC_TRACK");
    public static final EventType<AudioEvent> RELOAD_MUSIC_FILES =
            new EventType<>(ANY, "RELOAD_MUSIC_FILES");
    public static final EventType<AudioEvent> MUSIC_FILES_RELOADED =
            new EventType<>(ANY, "MUSIC_FILES_RELOADED");
    public static final EventType<AudioEvent> ENABLE_MUSIC_TRACKS =
            new EventType<>(ANY, "ENABLE_MUSIC_TRACKS");
    public static final EventType<AudioEvent> SET_MUSIC_VOLUME =
            new EventType<>(ANY, "SET_MUSIC_VOLUME");
    public static final EventType<AudioEvent> ENABLE_FADE_TRACKS =
            new EventType<>(ANY, "ENABLE_CROSSFADE_TRACKS");
    public static final EventType<AudioEvent> CYCLE_MUSIC_TRACKS =
            new EventType<>(ANY, "CYCLE_MUSIC_TRACKS");
    public static final EventType<AudioEvent> CURRENTLY_PLAYING_TRACK =
            new EventType<>(ANY, "CURRENTLY_PLAYING_TRACK");

    /** Generic payloads (optional). */
    public final Object object;
    public final Object object2;

    public AudioEvent(EventType<? extends AudioEvent> type) {
        this(type, null, null);
    }

    public AudioEvent(EventType<? extends AudioEvent> type, Object object) {
        this(type, object, null);
    }

    public AudioEvent(EventType<? extends AudioEvent> type, Object object, Object object2) {
        super(type);
        this.object = object;
        this.object2 = object2;
    }

    public AudioEvent(Object source, EventTarget target,
                      EventType<? extends AudioEvent> type,
                      Object object, Object object2) {
        super(source, target, type);
        this.object = object;
        this.object2 = object2;
    }

    @Override
    public AudioEvent copyFor(Object newSource, EventTarget newTarget) {
        return new AudioEvent(newSource, newTarget, getEventType(), object, object2);
    }

    @Override
    public EventType<? extends AudioEvent> getEventType() {
        return (EventType<? extends AudioEvent>) super.getEventType();
    }
}
