package AsteroidField.editor;

import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.TriangleMesh;

import java.util.Objects;

/**
 * Image-backed diffuse map painter. Works in UV space (0..1 wrap).
 * - Paint brush dabs at (u,v)
 * - Fill a face's UV triangle
 * - Apply procedural UV color functions
 * Seam crossings at U=0/1 are handled robustly.
 */
public final class DiffuseMapPainter {

    @FunctionalInterface
    public interface UVColorFunction {
        Color color(double u, double v); // u,v in [0,1)
    }

    private final WritableImage image;
    private final PixelWriter pw;
    private final int w;
    private final int h;

    public DiffuseMapPainter(WritableImage image) {
        this.image = Objects.requireNonNull(image, "image");
        this.pw = image.getPixelWriter();
        this.w = (int) image.getWidth();
        this.h = (int) image.getHeight();
    }

    public WritableImage getImage() { return image; }
    public int getWidth() { return w; }
    public int getHeight() { return h; }

    /* ---------- Public ops ---------- */

    /** Paint a circular dab centered at UV with a radius in pixels. */
    public void paintBrushUV(double u, double v, double radiusPx, Color color) {
        if (radiusPx <= 0) return;
        int cx = clamp((int) Math.round(u * (w - 1)), 0, w - 1);
        int cy = clamp((int) Math.round(v * (h - 1)), 0, h - 1);
        int r = (int) Math.ceil(radiusPx);
        int minX = clamp(cx - r, 0, w - 1);
        int maxX = clamp(cx + r, 0, w - 1);
        int minY = clamp(cy - r, 0, h - 1);
        int maxY = clamp(cy + r, 0, h - 1);
        double r2 = radiusPx * radiusPx;

        for (int y = minY; y <= maxY; y++) {
            int dy = y - cy;
            for (int x = minX; x <= maxX; x++) {
                int dx = x - cx;
                if (dx * dx + dy * dy <= r2) {
                    pw.setColor(x, y, color);
                }
            }
        }
    }

    /** Fill the face’s UV triangle with a color. Handles U seam wrapping. */
    public void paintFaceUV(TriangleMesh mesh, int faceIndex, Color color) {
        if (mesh == null || faceIndex < 0) return;
        FaceUV f = FaceUV.from(mesh, faceIndex);
        if (f == null) return;
        fillWrappedTriangle(f.u0, f.v0, f.u1, f.v1, f.u2, f.v2, color);
    }

    /** Apply a procedural function across the entire texture. */
    public void applyProcedural(UVColorFunction fn) {
        for (int y = 0; y < h; y++) {
            double v = (y + 0.5) / h;
            for (int x = 0; x < w; x++) {
                double u = (x + 0.5) / w;
                pw.setColor(x, y, fn.color(u, v));
            }
        }
    }

    /* ---------- Seam-aware triangle fill ---------- */

    private void fillWrappedTriangle(double u0, double v0,
                                     double u1, double v1,
                                     double u2, double v2,
                                     Color color) {
        double[] U = new double[]{u0, u1, u2};
        double[] V = new double[]{v0, v1, v2};
        unwrapU(U);

        for (int y = 0; y < h; y++) {
            double v = (y + 0.5) / h;
            for (int x = 0; x < w; x++) {
                double u = (x + 0.5) / w;

                double au = projectNear(U[0], u);
                double bu = projectNear(U[1], u);
                double cu = projectNear(U[2], u);

                if (pointInTriangle(u, v, au, V[0], bu, V[1], cu, V[2])) {
                    pw.setColor(x, y, color);
                }
            }
        }
    }

    private static void unwrapU(double[] U) {
        double min = Math.min(U[0], Math.min(U[1], U[2]));
        double max = Math.max(U[0], Math.max(U[1], U[2]));
        if (max - min > 0.5) {
            for (int i = 0; i < 3; i++) if (U[i] < 0.5) U[i] += 1.0;
        }
    }

    private static double projectNear(double Up, double uSample) {
        double d = Up - uSample;
        if (d > 0.5) return Up - 1.0;
        if (d < -0.5) return Up + 1.0;
        return Up;
    }

    private static boolean pointInTriangle(double px, double py,
                                           double ax, double ay,
                                           double bx, double by,
                                           double cx, double cy) {
        double w0 = orient2d(bx, by, cx, cy, px, py);
        double w1 = orient2d(cx, cy, ax, ay, px, py);
        double w2 = orient2d(ax, ay, bx, by, px, py);
        boolean hasNeg = (w0 < 0) || (w1 < 0) || (w2 < 0);
        boolean hasPos = (w0 > 0) || (w1 > 0) || (w2 > 0);
        return !(hasNeg && hasPos);
    }

    private static double orient2d(double ax, double ay,
                                   double bx, double by,
                                   double cx, double cy) {
        return (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    /* ---------- FaceUV helper ---------- */

    public static final class FaceUV {
        public final double u0, v0, u1, v1, u2, v2;
        private FaceUV(double u0, double v0, double u1, double v1, double u2, double v2) {
            this.u0 = u0; this.v0 = v0; this.u1 = u1; this.v1 = v1; this.u2 = u2; this.v2 = v2;
        }
        public static FaceUV from(TriangleMesh mesh, int faceIndex) {
            if (mesh == null || faceIndex < 0) return null;
            var faces = mesh.getFaces();
            var tex = mesh.getTexCoords();
            int base = faceIndex * 6;
            if (base + 5 >= faces.size()) return null;
            int t0 = faces.get(base + 1);
            int t1 = faces.get(base + 3);
            int t2 = faces.get(base + 5);
            int t0i = t0 * 2, t1i = t1 * 2, t2i = t2 * 2;
            double u0 = tex.get(t0i),     v0 = tex.get(t0i + 1);
            double u1 = tex.get(t1i),     v1 = tex.get(t1i + 1);
            double u2 = tex.get(t2i),     v2 = tex.get(t2i + 1);
            return new FaceUV(u0, v0, u1, v1, u2, v2);
        }
    }

    /* ---------- Procedural utilities ---------- */

    /** Smooth HSB gradient + stripes. */
    public static UVColorFunction gradientStripes(double hue0, double hue1, double stripeFreq, double stripeAmp) {
        return (u, v) -> {
            double t = v;
            double hue = lerp(hue0, hue1, t);
            double stripes = 0.5 * (1 + Math.sin(2 * Math.PI * (u * stripeFreq)));
            double s = clamp01(0.3 + stripeAmp * stripes);
            return Color.hsb(hue, s, 1.0);
        };
    }

    /** Simple tileable noise for mottling. */
    public static UVColorFunction tileableNoise(double scale, double contrast) {
        return (u, v) -> {
            double x = u * scale;
            double y = v * scale;
            double n = tileNoise(x, y);
            double c = Math.pow(n, contrast);
            return Color.gray(c);
        };
    }

    private static double tileNoise(double x, double y) {
        int xi = (int) Math.floor(x);
        int yi = (int) Math.floor(y);
        double xf = x - xi;
        double yf = y - yi;
        double n00 = hash01(xi, yi);
        double n10 = hash01(xi + 1, yi);
        double n01 = hash01(xi, yi + 1);
        double n11 = hash01(xi + 1, yi + 1);
        double nx0 = lerp(n00, n10, smoothstep(xf));
        double nx1 = lerp(n01, n11, smoothstep(xf));
        return lerp(nx0, nx1, smoothstep(yf));
    }

    private static double hash01(int x, int y) {
        long n = x * 374761393L + y * 668265263L;
        n = (n ^ (n >> 13)) * 1274126177L;
        n = n ^ (n >> 16);
        return ((n & 0xFFFFFFFFL) / (double) 0xFFFFFFFFL);
    }

    private static double smoothstep(double t) { return t*t*(3-2*t); }
    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }
    private static double clamp01(double v) { return Math.max(0, Math.min(1, v)); }
}
