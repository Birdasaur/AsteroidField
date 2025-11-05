package AsteroidField.audio;

import AsteroidField.events.SfxEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.media.AudioClip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Low-latency SFX player using AudioClip. Supports aliasing, caching,
 * per-play params (volume/balance/rate), and directory preloading.
 */
public class SfxPlayer implements EventHandler<SfxEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(SfxPlayer.class);

    /** Base folder where you store SFX (optional). */
    public static String DEFAULT_SFX_PATH = "sfx/";

    private final Scene scene;
    private final Map<String, AudioClip> cache = new HashMap<>();
    private final Map<String, String> aliasMap = new HashMap<>();

    /** Master SFX volume [0..1]; multiplied by per-play volume. */
    private double masterVolume = 0.8;

    public SfxPlayer(Scene scene) {
        this.scene = scene;
    }

    /** Register a short alias (e.g., "dock_chime" → "sfx/dock.wav"). */
    public void registerAlias(String alias, String idOrPath) {
        if (alias == null || idOrPath == null) return;
        aliasMap.put(alias, idOrPath);
    }

    /** Preload a single SFX into the cache by id or path. */
    public void preload(String idOrPath) {
        resolveClip(idOrPath, /*preloadOnly=*/true);
    }

    /** Preload all .wav/.mp3 in a directory (filesystem). */
    public void preloadDirectory(String dirPath) {
        if (dirPath == null) return;
        File dir = new File(dirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            LOG.warn("SFX preload dir not found: {}", dirPath);
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            String name = f.getName();
            if (name.endsWith(".wav") || name.endsWith(".mp3") || name.endsWith(".aiff") || name.endsWith(".m4a")) {
                try {
                    URL url = f.toURI().toURL();
                    cache.computeIfAbsent(url.toExternalForm(), AudioClip::new);
                } catch (MalformedURLException e) {
                    LOG.warn("Bad SFX file URL: {}", f, e);
                }
            }
        }
    }

    /** Set master SFX volume [0..1]. */
    public void setMasterVolume(double v) {
        masterVolume = Math.max(0.0, Math.min(1.0, v));
    }

    /** Stop all playing AudioClips that were created by this player. */
    public void stopAll() {
        for (AudioClip clip : cache.values()) {
            try {
                clip.stop();
            } catch (Exception ignore) { }
        }
    }

    // --- Event Handler ---

    @Override
    public void handle(SfxEvent e) {
        if (e == null || e.getEventType() == null) return;

        if (e.getEventType() == SfxEvent.PLAY_SFX) {
            String id = (e.object instanceof String s) ? s : null;
            SfxEvent.SfxParams p = (e.object2 instanceof SfxEvent.SfxParams sp) ? sp : SfxEvent.SfxParams.defaults();
            play(id, p);
        } else if (e.getEventType() == SfxEvent.SET_SFX_VOLUME) {
            if (e.object instanceof Number n) setMasterVolume(n.doubleValue());
        } else if (e.getEventType() == SfxEvent.STOP_ALL_SFX) {
            stopAll();
        } else if (e.getEventType() == SfxEvent.REGISTER_SFX_ALIAS) {
            String alias = (e.object instanceof String s) ? s : null;
            String path  = (e.object2 instanceof String s2) ? s2 : null;
            registerAlias(alias, path);
        } else if (e.getEventType() == SfxEvent.PRELOAD_SFX) {
            String id = (e.object instanceof String s) ? s : null;
            preload(id);
        } else if (e.getEventType() == SfxEvent.PRELOAD_SFX_DIR) {
            String dir = (e.object instanceof String s) ? s : null;
            preloadDirectory(dir);
        }
    }

    // --- Core play ---

    /** Play by id or path. id can be an alias or a resource/file path. */
    public void play(String idOrPath, SfxEvent.SfxParams params) {
        if (idOrPath == null) return;

        // Resolve alias → real id/path
        String resolved = aliasMap.getOrDefault(idOrPath, idOrPath);

        AudioClip clip = resolveClip(resolved, /*preloadOnly=*/false);
        if (clip == null) return;

        double vol = clamp01(params != null ? params.volume : 1.0) * masterVolume;
        double bal = clamp(-1.0, +1.0, params != null ? params.balance : 0.0);
        double rate = clamp(0.5, 2.0, params != null ? params.rate : 1.0);

        try {
            // Note: AudioClip#play allows overlapping playback.
            clip.play(vol, /*balance*/ bal, /*rate*/ rate, /*pan*/ 0.0, /*priority*/ 0);
        } catch (Exception ex) {
            LOG.warn("Failed to play SFX '{}'", resolved, ex);
        }
    }

    // --- Helpers ---

    private AudioClip resolveClip(String idOrPath, boolean preloadOnly) {
        if (idOrPath == null || idOrPath.isEmpty()) return null;

        // Already cached? Done.
        AudioClip cached = cache.get(idOrPath);
        if (cached != null) return cached;

        // Try as classpath resource first (recommended for packaged SFX).
        URL res = getClass().getClassLoader().getResource(idOrPath);
        if (res == null) {
            // Try under default sfx folder
            res = getClass().getClassLoader().getResource(DEFAULT_SFX_PATH + idOrPath);
        }

        String urlStr = null;
        if (res != null) {
            urlStr = res.toExternalForm();
        } else {
            // Fallback: treat as filesystem path
            File f = new File(idOrPath);
            if (!f.exists()) f = new File(DEFAULT_SFX_PATH, idOrPath);
            if (f.exists()) {
                try {
                    urlStr = f.toURI().toURL().toString();
                } catch (MalformedURLException ex) {
                    LOG.warn("Bad SFX file path: {}", f, ex);
                    return null;
                }
            }
        }

        if (urlStr == null) {
            if (!preloadOnly) LOG.warn("SFX not found: {}", idOrPath);
            return null;
        }

        // Cache and return
        AudioClip clip = new AudioClip(urlStr);
        cache.put(idOrPath, clip);
        return clip;
    }

    private static double clamp01(double v) { return v < 0 ? 0 : (v > 1 ? 1 : v); }
    private static double clamp(double lo, double hi, double v) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
