package AsteroidField.ui.scene3d;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.transform.Transform;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public final class StarAtlasGenerator {

    // ---------- Faces container ----------
    public static final class Faces {
        public final WritableImage top, bottom, left, right, front, back;
        public Faces(WritableImage top, WritableImage bottom, WritableImage left,
                     WritableImage right, WritableImage front, WritableImage back) {
            this.top = top; this.bottom = bottom; this.left = left;
            this.right = right; this.front = front; this.back = back;
        }
    }

    // ---------- Parameters ----------
    public static final class Params {
        // Output resolution (must be 4x3; cells must be square: width/4 == height/3)
        public int width  = 4096; // multiple of 4
        public int height = 3072; // multiple of 3
        public long seed  = 12345L;

        // Star field
        public double starDensity  = 0.00022; // stars per pixel
        public double colorBias    = 0.65;    // 0..1 (1 => more colored stars)
        public double radiusMean   = 0.333;   // px (Gaussian mean)
        public double radiusSigma  = 0.2;     // px (Gaussian sigma)
        public double radiusMax    = 1.0;     // px clamp
        public double brightChance = 0.02;    // occasional bright points
        public double brightMul    = 1.3;     // brightness multiplier

        // Nebula (soft haze)
        public int    nebulaBlobs  = 48;
        public double nebulaAlpha  = 0.04;    // per-blob alpha (LOW)
        public double nebulaMinR   = 40;      // px
        public double nebulaMaxR   = 160;     // px

        // Optional internal bleed inside each cell (duplicate edges inward)
        public int internalBleedPx = 0;

        // Milky Way band across the mid row (left→front→right→back)
        public boolean mwEnabled   = false;
        public double  mwIntensity = 0.6;     // brightness scalar
        public double  mwOpacity   = 0.35;    // band alpha
        public double  mwThickness = 80;      // px (sigma-ish)
        public double  mwHue       = 210;     // 0..360 (HSB hue)
        public double  mwSaturation= 0.25;    // 0..1
        public double  mwTiltDeg   = 0.0;     // rotation in strip
        public double  mwVOffset   = 0.0;     // -0.5..+0.5 of cell height
        public double  mwNoise     = 0.25;    // 0..1 patchiness

        // Debug overlay
        public boolean debugGrid   = false;   // draw cell borders + labels
        public double  debugAlpha  = 0.35;    // grid opacity
    }

    private StarAtlasGenerator() {}

    // ---------- Generate full 4x3 cross ----------
    public static WritableImage generate(Params p) {
        if (p.width % 4 != 0 || p.height % 3 != 0) {
            throw new IllegalArgumentException("Width must be multiple of 4 and height multiple of 3.");
        }
        final int W = p.width, H = p.height;
        final int cell = W / 4;
        if (cell != H / 3) {
            throw new IllegalArgumentException("Cells must be square (width/4 == height/3).");
        }

        final Canvas canvas = new Canvas(W, H);
        final GraphicsContext g = canvas.getGraphicsContext2D();

        // Background
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, W, H);

        final Random rng = new Random(p.seed);

        // --- Stars ---
        drawStars(g, W, H, rng, p);

        // --- Milky Way band (mid-row) ---
        if (p.mwEnabled) {
            renderMilkyWayBand(g, W, H, cell, rng, p);
        }

        // --- Nebula (soft)
        if (p.nebulaBlobs > 0 && p.nebulaAlpha > 0) {
            renderNebula(g, W, H, rng, p);
        }

        // --- Debug grid (optional)
        if (p.debugGrid) {
            renderDebugGrid(g, W, H, cell, p);
        }

        // Snapshot to image
        WritableImage out = new WritableImage(W, H);
        SnapshotParameters sp = new SnapshotParameters();
        sp.setTransform(Transform.scale(1, 1));
        sp.setFill(Color.BLACK);
        canvas.snapshot(sp, out);

        // Optional internal bleed
        if (p.internalBleedPx > 0) {
            addInternalBleed(out, cell, p.internalBleedPx);
        }

        return out;
    }

    // ---------- Slice faces ----------
    public static Faces sliceFaces(Image atlas) {
        final int W = (int) Math.round(atlas.getWidth());
        final int H = (int) Math.round(atlas.getHeight());
        if (W % 4 != 0 || H % 3 != 0) {
            throw new IllegalArgumentException("Atlas must be 4x3; got " + W + "x" + H);
        }
        final int cell = W / 4;
        if (cell != H / 3) {
            throw new IllegalArgumentException("Cells must be square (width/4 == height/3).");
        }
        PixelReader pr = atlas.getPixelReader();
        if (pr == null) throw new IllegalArgumentException("Atlas has no PixelReader.");

        WritableImage top    = new WritableImage(pr, 1 * cell, 0 * cell, cell, cell);
        WritableImage left   = new WritableImage(pr, 0 * cell, 1 * cell, cell, cell);
        WritableImage front  = new WritableImage(pr, 1 * cell, 1 * cell, cell, cell);
        WritableImage right  = new WritableImage(pr, 2 * cell, 1 * cell, cell, cell);
        WritableImage back   = new WritableImage(pr, 3 * cell, 1 * cell, cell, cell);
        WritableImage bottom = new WritableImage(pr, 1 * cell, 2 * cell, cell, cell);

        return new Faces(top, bottom, left, right, front, back);
    }

    public static Faces generateFaces(Params p) {
        return sliceFaces(generate(p));
    }

    // ---------- Save helpers ----------
    public static void savePng(WritableImage img, File file) throws IOException {
        ImageIO.write(SwingFXUtils.fromFXImage(img, null), "PNG", file);
    }

    public static void saveFaces(Faces f, File dir, String basename) throws IOException {
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Could not create directory: " + dir);
        }
        ImageIO.write(SwingFXUtils.fromFXImage(f.top,    null), "PNG", new File(dir, basename + "_top.png"));
        ImageIO.write(SwingFXUtils.fromFXImage(f.bottom, null), "PNG", new File(dir, basename + "_bottom.png"));
        ImageIO.write(SwingFXUtils.fromFXImage(f.left,   null), "PNG", new File(dir, basename + "_left.png"));
        ImageIO.write(SwingFXUtils.fromFXImage(f.right,  null), "PNG", new File(dir, basename + "_right.png"));
        ImageIO.write(SwingFXUtils.fromFXImage(f.front,  null), "PNG", new File(dir, basename + "_front.png"));
        ImageIO.write(SwingFXUtils.fromFXImage(f.back,   null), "PNG", new File(dir, basename + "_back.png"));
    }

    // ---------- Internals ----------

    private static void drawStars(GraphicsContext g, int W, int H, Random rng, Params p) {
        final int starCount = (int) Math.round(W * H * p.starDensity);
        for (int i = 0; i < starCount; i++) {
            final int x = rng.nextInt(W);
            final int y = rng.nextInt(H);

            Color c = pickStarColor(rng, p.colorBias);
            if (rng.nextDouble() < p.brightChance) {
                c = c.deriveColor(0, 1, p.brightMul, 1.0);
            }

            double rad = clamp(gaussian(rng, p.radiusMean, p.radiusSigma), 0.0, p.radiusMax);

            if (rad < 0.4) {
                g.getPixelWriter().setColor(x, y, c);
            } else {
                g.setFill(c);
                final double d = rad * 2.0;
                g.fillOval(x - rad, y - rad, d, d);
            }
        }
    }

    private static void renderNebula(GraphicsContext g, int W, int H, Random rng, Params p) {
        final Canvas neb = new Canvas(W, H);
        final GraphicsContext ng = neb.getGraphicsContext2D();

        for (int i = 0; i < p.nebulaBlobs; i++) {
            final int cx = rng.nextInt(W);
            final int cy = rng.nextInt(H);
            final double r  = lerp(p.nebulaMinR, p.nebulaMaxR, rng.nextDouble());
            final Color base = pickNebulaColor(rng).deriveColor(0, 1, 1, p.nebulaAlpha);

            // multi-ring "soft" stamp
            final int rings = 6;
            for (int k = rings; k >= 1; k--) {
                double rr = r * k / rings;
                double a = p.nebulaAlpha * (k / (double) rings) * 0.8;
                ng.setFill(base.deriveColor(0, 1, 1, a));
                ng.fillOval(cx - rr, cy - rr, rr * 2, rr * 2);
            }
        }

        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        g.drawImage(neb.snapshot(sp, null), 0, 0);
    }

    private static void renderMilkyWayBand(GraphicsContext g, int W, int H, int cell, Random rng, Params p) {
        final int stripW = 4 * cell;
        final int stripH = cell;

        Canvas band = new Canvas(stripW, stripH);
        GraphicsContext bg = band.getGraphicsContext2D();

        final double centerY = stripH * (0.5 + p.mwVOffset);
        final double sigma   = Math.max(1.0, p.mwThickness);
        final double twoSigma2 = 2.0 * sigma * sigma;

        final int noisePeriod = 64;
        final double hue = p.mwHue;
        final double sat = clamp01(p.mwSaturation);
        final double baseAlpha = clamp01(p.mwOpacity);
        final double intensity = Math.max(0.0, p.mwIntensity);
        final double noiseAmt  = clamp01(p.mwNoise);

        for (int x = 0; x < stripW; x++) {
            double nx = (Math.sin((x / (double) noisePeriod) * Math.PI * 2.0) * 0.5 + 0.5);
            nx = (1.0 - noiseAmt) + noiseAmt * nx; // mix noise

            for (int y = 0; y < stripH; y++) {
                double dy = y - centerY;
                double gauss = Math.exp(-(dy * dy) / twoSigma2); // 0..1
                double a = baseAlpha * gauss * nx * intensity;
                if (a < 0.01) continue;

                Color c = Color.hsb(hue, sat, 1.0, clamp01(a));
                bg.getPixelWriter().setColor(x, y, c);
            }
        }

        // Tilt within the strip if requested
        if (Math.abs(p.mwTiltDeg) > 0.01) {
            SnapshotParameters sp = new SnapshotParameters();
            sp.setFill(Color.TRANSPARENT);
            WritableImage raw = band.snapshot(sp, null);

            Canvas tilted = new Canvas(stripW, stripH);
            GraphicsContext tg = tilted.getGraphicsContext2D();
            tg.save();
            tg.translate(stripW / 2.0, stripH / 2.0);
            tg.rotate(p.mwTiltDeg);
            tg.translate(-stripW / 2.0, -stripH / 2.0);
            tg.drawImage(raw, 0, 0);
            tg.restore();
            band = tilted;
        }

        SnapshotParameters sp2 = new SnapshotParameters();
        sp2.setFill(Color.TRANSPARENT);
        WritableImage bandImg = band.snapshot(sp2, null);

        // Draw the band into the middle row
        g.drawImage(bandImg, 0, cell);
    }

    private static void renderDebugGrid(GraphicsContext g, int W, int H, int cell, Params p) {
        double a = clamp01(p.debugAlpha);
        g.setGlobalAlpha(a);
        g.setStroke(Color.color(0.3, 1.0, 0.3, a));
        g.setLineWidth(2.0);

        // Vertical grid lines for columns 0..4
        for (int c = 0; c <= 4; c++) g.strokeLine(c * cell, 0, c * cell, H);
        // Horizontal grid lines for rows 0..3
        for (int r = 0; r <= 3; r++) g.strokeLine(0, r * cell, W, r * cell);

        // Labels for cross cells
        g.setGlobalAlpha(0.9);
        g.setFill(Color.color(0.2, 1.0, 0.2, 0.9));
        g.setFont(javafx.scene.text.Font.font(24));

        labelCell(g, "TOP",    cell*1, cell*0, cell);
        labelCell(g, "LEFT",   cell*0, cell*1, cell);
        labelCell(g, "FRONT",  cell*1, cell*1, cell);
        labelCell(g, "RIGHT",  cell*2, cell*1, cell);
        labelCell(g, "BACK",   cell*3, cell*1, cell);
        labelCell(g, "BOTTOM", cell*1, cell*2, cell);

        g.setGlobalAlpha(1.0);
    }

    private static void labelCell(GraphicsContext g, String txt, int x, int y, int cell) {
        double cx = x + cell / 2.0;
        double cy = y + cell / 2.0;
        // outline
        g.setFill(Color.color(0,0,0,0.8));
        g.fillText(txt, cx - 40 + 1, cy + 1);
        // text
        g.setFill(Color.color(0.2, 1.0, 0.2, 0.9));
        g.fillText(txt, cx - 40, cy);
    }

    private static void addInternalBleed(WritableImage img, int cell, int bleed) {
        var pw = img.getPixelWriter();
        var pr = img.getPixelReader();

        int[][] cells = {
            {1,0}, // top
            {0,1}, // left
            {1,1}, // front
            {2,1}, // right
            {3,1}, // back
            {1,2}  // bottom
        };
        for (int[] rc : cells) {
            int cx = rc[0] * cell;
            int cy = rc[1] * cell;

            // left edge -> bleed inward
            for (int x = 0; x < bleed; x++) {
                for (int y = 0; y < cell; y++) {
                    var c = pr.getColor(cx + bleed, cy + y);
                    pw.setColor(cx + x, cy + y, c);
                }
            }
            // right edge
            for (int x = 0; x < bleed; x++) {
                for (int y = 0; y < cell; y++) {
                    var c = pr.getColor(cx + cell - bleed - 1, cy + y);
                    pw.setColor(cx + cell - 1 - x, cy + y, c);
                }
            }
            // top edge
            for (int y = 0; y < bleed; y++) {
                for (int x = 0; x < cell; x++) {
                    var c = pr.getColor(cx + x, cy + bleed);
                    pw.setColor(cx + x, cy + y, c);
                }
            }
            // bottom edge
            for (int y = 0; y < bleed; y++) {
                for (int x = 0; x < cell; x++) {
                    var c = pr.getColor(cx + x, cy + cell - bleed - 1);
                    pw.setColor(cx + x, cy + cell - 1 - y, c);
                }
            }
        }
    }

    // ---------- Utility color & math ----------

    private static Color pickStarColor(Random rng, double colorBias) {
        if (rng.nextDouble() >= colorBias) return Color.WHITE;
        switch (rng.nextInt(4)) {
            case 0: return Color.rgb(255, 236, 210); // warm
            case 1: return Color.rgb(215, 235, 255); // cool
            case 2: return Color.rgb(255, 210, 240); // magenta hint
            default:return Color.rgb(205, 255, 235); // greenish
        }
    }

    private static Color pickNebulaColor(Random rng) {
        switch (rng.nextInt(4)) {
            case 0: return Color.rgb( 80, 120, 255);
            case 1: return Color.rgb(255, 110, 150);
            case 2: return Color.rgb(255, 190,  90);
            default:return Color.rgb(100, 255, 190);
        }
    }

    private static double gaussian(Random r, double mean, double sigma) {
        // Box–Muller transform
        double u1 = 1.0 - r.nextDouble();
        double u2 = 1.0 - r.nextDouble();
        double z = Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
        return mean + z * sigma;
    }

    private static double clamp(double v, double lo, double hi) {
        return (v < lo) ? lo : (v > hi ? 1.0 : v);
    }

    private static double clamp01(double v) {
        return (v < 0) ? 0 : (v > 1 ? 1 : v);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
