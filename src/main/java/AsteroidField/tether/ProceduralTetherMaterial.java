package AsteroidField.tether.materials;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

/**
 * High-performance procedural material:
 *  - DiffuseMap  : grayscale banding (cached per radius-bucket). Color comes from diffuseColor.
 *  - BumpMap     : grayscale height ribs (cached per radius-bucket).
 *  - EmissiveMap : small tinted masks at discrete strengths (cached per strength bucket).
 *
 * NO per-frame image painting. We just:
 *   - set material.diffuseColor for tint changes,
 *   - swap a cached selfIlluminationMap for emissive strength steps.
 */
public final class ProceduralTetherMaterial {

    private final PhongMaterial mat = new PhongMaterial();

    // Public knobs (fast setters)
    private Color baseColor = Color.CYAN;         // Applied via diffuseColor (fast)
    private Color lockTint  = Color.LIMEGREEN;    // Tints emissive maps at creation time (cached variants)
    private double emissiveStrength = 0.0;        // 0..1, discretized to a small bucket set

    // Cache sizing
    private static final int DIFF_W = 512; // around U
    private static final int DIFF_H = 512; // along V (grayscale banding; tile-safe)
    private static final int BUMP_W = 512;
    private static final int BUMP_H = 512;

    // Radius “buckets” to avoid over-caching
    private static final double RADIUS_BUCKET = 5.0; // 5 unit buckets

    // Emissive strengths we support (discrete)
    private static final double[] EMISSIVE_STEPS = {0.0, 0.25, 0.5, 0.75, 1.0};
    private static final int EMIS_W = 128, EMIS_H = 256; // small

    // Caches (static so all tethers share)
    private static final Map<DiffKey, Image> DIFFUSE_CACHE = new HashMap<>();
    private static final Map<DiffKey, Image> BUMP_CACHE    = new HashMap<>();
    private static final Map<EmKey,  Image> EMISSIVE_CACHE = new HashMap<>();

    // Last applied keys
    private DiffKey lastDiffKey = null;
    private DiffKey lastBumpKey = null;
    private EmKey  lastEmKey    = null;

    // Random for diffuse noise generation
    private final Random rng = new Random(1337);

    public ProceduralTetherMaterial() {
        mat.setSpecularColor(Color.WHITE);
        mat.setSpecularPower(64);
        mat.setDiffuseColor(baseColor); // tint applied here (not baked into diffuse map)
    }

    public PhongMaterial material() { return mat; }

    // --------- public setters (fast) ---------

    /** Fast tint. Does not rebuild textures. */
    public void setBaseColor(Color c) {
        if (c == null) return;
        baseColor = c;
        mat.setDiffuseColor(baseColor);
    }

    /** Changes the emissive tint for the cached images. Only rebuilt lazily if a new tint is requested. */
    public void setLockTint(Color c) {
        if (c == null) return;
        if (!colorsClose(lockTint, c)) {
            lockTint = c;
            // We do NOT invalidate caches aggressively. Only new EmKey (tint+strength) will be built on demand.
            lastEmKey = null; // force a refresh next time setEmissiveStrength runs
        }
    }

    /**
     * Set emissive strength in 0..1. We pick the nearest cached bucket and swap the image.
     * No image painting here.
     */
    public void setEmissiveStrength(double s) {
        emissiveStrength = clamp01(s);
        EmKey key = new EmKey(nearestStep(emissiveStrength), quantizeColor(lockTint));
        if (!key.equals(lastEmKey)) {
            Image em = EMISSIVE_CACHE.computeIfAbsent(key, k -> buildEmissive(EMIS_W, EMIS_H, k.tint, k.step));
            mat.setSelfIlluminationMap(em);
            lastEmKey = key;
        }
    }

    /**
     * Ensure diffuse/bump maps are applied for the current radius bucket.
     * Call this when radius changes (or once in constructor).
     */
    public void ensureStaticMaps(double radius) {
        DiffKey key = new DiffKey(quantize(radius));
        if (!key.equals(lastDiffKey)) {
            Image diff = DIFFUSE_CACHE.computeIfAbsent(key, k -> buildDiffuseGray(DIFF_W, DIFF_H));
            mat.setDiffuseMap(diff);
            lastDiffKey = key;
        }
        if (!key.equals(lastBumpKey)) {
            Image bump = BUMP_CACHE.computeIfAbsent(key, k -> buildBumpGray(BUMP_W, BUMP_H));
            mat.setBumpMap(bump);
            lastBumpKey = key;
        }
        // diffuseColor already set via setBaseColor
        // emissive set via setEmissiveStrength
    }

    // --------- image builders (one-time per key) ---------

    // Grayscale diffuse with radial center emphasis + longitudinal bands
    private Image buildDiffuseGray(int w, int h) {
        WritableImage img = new WritableImage(w, h);
        var pw = img.getPixelWriter();

        int bands = 6;
        double[] bandY = new double[bands];
        for (int r = 0; r < bands; r++) bandY[r] = (r + 0.5) * (h / (double)bands);

        for (int y = 0; y < h; y++) {
            double bandAccum = 0.0;
            for (int r = 0; r < bands; r++) {
                double dy = (y - bandY[r]) / 14.0;
                bandAccum += Math.exp(-dy * dy) * (0.06 - r * 0.007);
            }
            for (int x = 0; x < w; x++) {
                double u = x / (double)(w - 1);
                // radial center emphasis (brighter center)
                double du = Math.abs(u - 0.5) / 0.5;
                double radial = 1.0 - Math.pow(du, 1.2);
                // subtle noise
                double n = (rng.nextDouble() - 0.5) * 0.03;
                double g = clamp01(0.55 - 0.35 * radial - bandAccum + n); // grayscale
                pw.setColor(x, y, new Color(g, g, g, 1.0));
            }
        }
        return img;
    }

    // Grayscale bump: faint ribs along V; edge falls off near U edges
    private Image buildBumpGray(int w, int h) {
        WritableImage img = new WritableImage(w, h);
        PixelWriter pw = img.getPixelWriter();
        int repeats = 8; // fixed -> no rebuild with length
        double freq = repeats * 2.0 * Math.PI;

        for (int y = 0; y < h; y++) {
            double v = y / (double)(h - 1);
            double rib = 0.5 + 0.15 * Math.sin(v * freq); // 0.35..0.65
            for (int x = 0; x < w; x++) {
                double u = x / (double)(w - 1);
                double edge = 1.0 - Math.pow(Math.abs(u - 0.5) / 0.5, 1.6);
                double g = clamp01(rib * edge);
                pw.setColor(x, y, new Color(g, g, g, 1.0));
            }
        }
        return img;
    }

    // Emissive: tinted core+band at a given discrete strength
    private Image buildEmissive(int w, int h, Color tint, double strength) {
        WritableImage img = new WritableImage(w, h);
        PixelWriter pw = img.getPixelWriter();

        // Fixed band location/width; we’re not animating it per-frame anymore
        double bandCenter = 0.55;
        double bandWidth  = 0.12;

        for (int y = 0; y < h; y++) {
            double v = y / (double)(h - 1);
            double dv = Math.abs(v - bandCenter) / bandWidth;
            double band = Math.exp(-dv * dv * 3.0);

            for (int x = 0; x < w; x++) {
                double u = x / (double)(w - 1);
                double du = Math.abs(u - 0.5) / 0.5;
                double radial = Math.exp(-du * du * 3.0);

                double e = clamp01(strength * (0.55 * radial + 0.70 * band));
                // bake tint
                Color c = new Color(
                        clamp01(tint.getRed()),
                        clamp01(tint.getGreen()),
                        clamp01(tint.getBlue()),
                        e);
                pw.setColor(x, y, c);
            }
        }
        return img;
    }

    // --------- helpers & keys ---------

    private static boolean colorsClose(Color a, Color b) {
        return Math.abs(a.getRed()-b.getRed())<1e-3 &&
               Math.abs(a.getGreen()-b.getGreen())<1e-3 &&
               Math.abs(a.getBlue()-b.getBlue())<1e-3;
    }
    private static double clamp01(double v){ return Math.max(0, Math.min(1, v)); }
    private static double quantize(double v){ return Math.round(v / RADIUS_BUCKET) * RADIUS_BUCKET; }

    private static Color quantizeColor(Color c) {
        // Quantize RGB to 32 steps to maximize cache hits
        double r = Math.round(c.getRed()*31.0)/31.0;
        double g = Math.round(c.getGreen()*31.0)/31.0;
        double b = Math.round(c.getBlue()*31.0)/31.0;
        return new Color(r,g,b,1.0);
    }

    private static double nearestStep(double s) {
        double best = EMISSIVE_STEPS[0];
        double bestD = Math.abs(s - best);
        for (double v : EMISSIVE_STEPS) {
            double d = Math.abs(s - v);
            if (d < bestD) { bestD = d; best = v; }
        }
        return best;
    }

    private static final class DiffKey {
        final double radiusQ;
        DiffKey(double r){ this.radiusQ = r; }
        @Override public boolean equals(Object o){ return (o instanceof DiffKey dk) && dk.radiusQ==radiusQ; }
        @Override public int hashCode(){ return Objects.hash(radiusQ); }
    }

    private static final class EmKey {
        final double step;
        final Color  tint;
        EmKey(double s, Color t){ this.step=s; this.tint=t; }
        @Override public boolean equals(Object o){
            if (!(o instanceof EmKey k)) return false;
            return k.step==step &&
                   Math.abs(k.tint.getRed()-tint.getRed())<1e-6 &&
                   Math.abs(k.tint.getGreen()-tint.getGreen())<1e-6 &&
                   Math.abs(k.tint.getBlue()-tint.getBlue())<1e-6;
        }
        @Override public int hashCode(){ return Objects.hash(step, tint.getRed(), tint.getGreen(), tint.getBlue()); }
    }
}
