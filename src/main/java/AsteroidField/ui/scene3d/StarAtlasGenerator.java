package AsteroidField.ui.scene3d;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Transform;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

public final class StarAtlasGenerator {

    // ----- Faces container -----
    public static final class Faces {
        public final WritableImage top, bottom, left, right, front, back;
        public Faces(WritableImage top, WritableImage bottom, WritableImage left,
                     WritableImage right, WritableImage front, WritableImage back) {
            this.top = top; this.bottom = bottom; this.left = left;
            this.right = right; this.front = front; this.back = back;
        }
    }

    // ----- Parameters -----
    public static final class Params {
        public int width  = 4096;   // multiple of 4
        public int height = 3072;   // multiple of 3
        public long seed  = 12345L;

        // stars
        public double starDensity  = 0.00022;
        public double colorBias    = 0.65;
        public double radiusMean   = 0.333;
        public double radiusSigma  = 0.2;
        public double radiusMax    = 1.0;
        public double brightChance = 0.02;
        public double brightMul    = 1.3;

        // nebula
        public int    nebulaBlobs  = 48;
        public double nebulaAlpha  = 0.04;
        public double nebulaMinR   = 40;
        public double nebulaMaxR   = 160;

        // optional edge bleed in each face
        public int internalBleedPx = 0;

        // Milky Way strip across middle row
        public boolean mwEnabled   = false;
        public double  mwIntensity = 0.6;
        public double  mwOpacity   = 0.35;
        public double  mwThickness = 80;
        public double  mwHue       = 210;
        public double  mwSaturation= 0.25;
        public double  mwTiltDeg   = 0.0;
        public double  mwVOffset   = 0.0;    // -0.5..+0.5 of cell height
        public double  mwNoise     = 0.25;

        // debug
        public boolean debugGrid   = false;
        public double  debugAlpha  = 0.35;

        // render only into the six cross cells (leave corners blank)
        public boolean strictCrossMask = true;
    }

    private StarAtlasGenerator() {}

    // ----- public API -----
    public static WritableImage generate(Params p) {
        if (p.width % 4 != 0 || p.height % 3 != 0)
            throw new IllegalArgumentException("width%4==0, height%3==0 required");
        final int W = p.width, H = p.height;
        final int cell = W / 4;
        if (cell != H / 3)
            throw new IllegalArgumentException("Cells must be square: width/4 == height/3.");

        Canvas canvas = new Canvas(W, H);
        GraphicsContext g = canvas.getGraphicsContext2D();

        // 1) Opaque background (prevents transparent exports)
        g.setGlobalAlpha(1.0);
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, W, H);

        Random rng = new Random(p.seed);

        Rects r = new Rects(cell);

        // 2) Draw stars (strict cross if enabled)
        if (p.strictCrossMask) drawStarsStrict(g, rng, p, r, cell);
        else                   drawStarsFull(g, W, H, rng, p);

        // 3) Milky Way (always constrained to middle row)
        if (p.mwEnabled) renderMilkyWayBand(g, W, H, cell, rng, p);

        // 4) Nebula
        if (p.nebulaBlobs > 0 && p.nebulaAlpha > 0) {
            if (p.strictCrossMask) renderNebulaStrict(g, rng, p, r);
            else                   renderNebula(g, W, H, rng, p);
        }

        // 5) Debug grid
        if (p.debugGrid) renderDebugGrid(g, W, H, cell, p);

        // 6) Ensure alpha back to 1.0 before snapshot
        g.setGlobalAlpha(1.0);

        // 7) Snapshot with opaque fill
        SnapshotParameters sp = new SnapshotParameters();
        sp.setTransform(Transform.scale(1, 1));
        sp.setFill(Color.BLACK);          // ← force opaque background in snapshot
        WritableImage out = new WritableImage(W, H);
        canvas.snapshot(sp, out);

        // 8) Internal bleed (optional)
        if (p.internalBleedPx > 0) addInternalBleed(out, cell, p.internalBleedPx);

        return out;
    }

    public static Faces sliceFaces(Image atlas) {
        final int W = (int)Math.round(atlas.getWidth());
        final int H = (int)Math.round(atlas.getHeight());
        if (W % 4 != 0 || H % 3 != 0) throw new IllegalArgumentException("Atlas must be 4x3.");
        final int cell = W / 4;
        if (cell != H / 3) throw new IllegalArgumentException("Cells must be square.");

        PixelReader pr = atlas.getPixelReader();
        if (pr == null) throw new IllegalArgumentException("Atlas has no PixelReader.");

        WritableImage top    = new WritableImage(pr, 1*cell, 0*cell, cell, cell);
        WritableImage left   = new WritableImage(pr, 0*cell, 1*cell, cell, cell);
        WritableImage front  = new WritableImage(pr, 1*cell, 1*cell, cell, cell);
        WritableImage right  = new WritableImage(pr, 2*cell, 1*cell, cell, cell);
        WritableImage back   = new WritableImage(pr, 3*cell, 1*cell, cell, cell);
        WritableImage bottom = new WritableImage(pr, 1*cell, 2*cell, cell, cell);
        return new Faces(top, bottom, left, right, front, back);
    }

    public static Faces generateFaces(Params p) {
        return sliceFaces(generate(p));
    }

    public static void savePng(WritableImage img, File file) throws IOException {
        ImageIO.write(SwingFXUtils.fromFXImage(img, null), "PNG", file);
    }

    /** Always flattens onto opaque black (no transparency in the saved file). */
    public static void savePngFlattened(WritableImage img, File file) throws IOException {
        BufferedImage src = SwingFXUtils.fromFXImage(img, null);
        BufferedImage dst = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        // Composite over black
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int argb = src.getRGB(x, y);
                // simple alpha-over black: keep RGB if alpha=255, else multiply by alpha
                int a = (argb >>> 24) & 0xFF;
                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>> 8)  & 0xFF;
                int b = (argb)        & 0xFF;
                if (a < 255) { // premultiply onto black
                    r = (r * a) / 255;
                    g = (g * a) / 255;
                    b = (b * a) / 255;
                }
                dst.setRGB(x, y, (0xFF << 24) | (r << 16) | (g << 8) | b);
            }
        }
        ImageIO.write(dst, "PNG", file);
    }
/** Save the 6 cube faces as PNG files in the given directory with the given basename.
 *  Files will be named: basename_top.png, _bottom.png, _left.png, _right.png, _front.png, _back.png
 *  Uses savePngFlattened(...) to guarantee opaque output (no accidental transparency).
 */
public static void saveFaces(Faces f, File dir, String basename) throws IOException {
    saveFaces(f, dir, basename, /*flatten*/ true);
}

/** Same as saveFaces(...) but lets you control whether to flatten alpha. */
public static void saveFaces(Faces f, File dir, String basename, boolean flatten) throws IOException {
    if (f == null) throw new IllegalArgumentException("Faces is null");
    if (dir == null) throw new IllegalArgumentException("Directory is null");
    if (!dir.exists() && !dir.mkdirs()) {
        throw new IOException("Could not create directory: " + dir.getAbsolutePath());
    }

    saveOneFace(f.top,    new File(dir, basename + "_top.png"),    flatten);
    saveOneFace(f.bottom, new File(dir, basename + "_bottom.png"), flatten);
    saveOneFace(f.left,   new File(dir, basename + "_left.png"),   flatten);
    saveOneFace(f.right,  new File(dir, basename + "_right.png"),  flatten);
    saveOneFace(f.front,  new File(dir, basename + "_front.png"),  flatten);
    saveOneFace(f.back,   new File(dir, basename + "_back.png"),   flatten);
}

private static void saveOneFace(WritableImage img, File file, boolean flatten) throws IOException {
    if (flatten) {
        savePngFlattened(img, file);
    } else {
        savePng(img, file);
    }
}

/** Convenience: generate (with current params) and immediately save the faces. */
public static void generateAndSaveFaces(Params p, File dir, String basename) throws IOException {
    Faces f = generateFaces(p);
    saveFaces(f, dir, basename);
}
    // ----- internals -----

    // active rectangles in a strict cross
    private static final class Rects {
        final Rectangle TOP, LEFT, FRONT, RIGHT, BACK, BOTTOM;
        final Rectangle[] ALL;
        Rects(int cell) {
            TOP    = new Rectangle(1*cell, 0*cell, cell, cell);
            LEFT   = new Rectangle(0*cell, 1*cell, cell, cell);
            FRONT  = new Rectangle(1*cell, 1*cell, cell, cell);
            RIGHT  = new Rectangle(2*cell, 1*cell, cell, cell);
            BACK   = new Rectangle(3*cell, 1*cell, cell, cell);
            BOTTOM = new Rectangle(1*cell, 2*cell, cell, cell);
            ALL = new Rectangle[]{TOP, LEFT, FRONT, RIGHT, BACK, BOTTOM};
        }
    }

    private static void drawStarsFull(GraphicsContext g, int W, int H, Random rng, Params p) {
        int starCount = (int)Math.round(W * H * p.starDensity);
        for (int i = 0; i < starCount; i++) drawStar(g, rng, p, rng.nextInt(W), rng.nextInt(H), null);
    }

    private static void drawStarsStrict(GraphicsContext g, Random rng, Params p, Rects r, int cell) {
        final double activePixels = 6.0 * cell * cell;
        final int starCount = (int)Math.round(activePixels * p.starDensity);
        for (int i = 0; i < starCount; i++) {
            Rectangle face = r.ALL[rng.nextInt(r.ALL.length)];
            int x = (int)(face.getX() + rng.nextInt((int)face.getWidth()));
            int y = (int)(face.getY() + rng.nextInt((int)face.getHeight()));
            drawStar(g, rng, p, x, y, face);
        }
    }

    private static void drawStar(GraphicsContext g, Random rng, Params p, int x, int y, Rectangle clipFace) {
        Color c = pickStarColor(rng, p.colorBias);
        if (rng.nextDouble() < p.brightChance) c = c.deriveColor(0, 1, p.brightMul, 1.0);
        double rad = clamp(gaussian(rng, p.radiusMean, p.radiusSigma), 0.0, p.radiusMax);

        if (rad < 0.4) {
            g.getPixelWriter().setColor(x, y, c);
        } else {
            if (clipFace != null) {
                g.save();
                g.beginPath();
                g.rect(clipFace.getX(), clipFace.getY(), clipFace.getWidth(), clipFace.getHeight());
                g.closePath();
                g.clip();
            }
            g.setFill(c);
            double d = rad * 2.0;
            g.fillOval(x - rad, y - rad, d, d);
            if (clipFace != null) g.restore();
        }
    }

    private static void renderNebula(GraphicsContext g, int W, int H, Random rng, Params p) {
        for (int i = 0; i < p.nebulaBlobs; i++) {
            double cx = rng.nextDouble() * W;
            double cy = rng.nextDouble() * H;
            stampNebulaBlob(g, cx, cy, rng, p);
        }
    }

    private static void renderNebulaStrict(GraphicsContext g, Random rng, Params p, Rects r) {
        int perFace = Math.max(1, p.nebulaBlobs / r.ALL.length);
        int rem = p.nebulaBlobs - perFace * r.ALL.length;
        for (int fi = 0; fi < r.ALL.length; fi++) {
            Rectangle face = r.ALL[fi];
            int blobs = perFace + (fi < rem ? 1 : 0);
            g.save();
            g.beginPath();
            g.rect(face.getX(), face.getY(), face.getWidth(), face.getHeight());
            g.closePath();
            g.clip();
            for (int i = 0; i < blobs; i++) {
                double cx = face.getX() + rng.nextDouble() * face.getWidth();
                double cy = face.getY() + rng.nextDouble() * face.getHeight();
                stampNebulaBlob(g, cx, cy, rng, p);
            }
            g.restore();
        }
    }

    private static void stampNebulaBlob(GraphicsContext g, double cx, double cy, Random rng, Params p) {
        double r  = lerp(p.nebulaMinR, p.nebulaMaxR, rng.nextDouble());
        Color base = pickNebulaColor(rng).deriveColor(0, 1, 1, p.nebulaAlpha);
        int rings = 6;
        for (int k = rings; k >= 1; k--) {
            double rr = r * k / rings;
            double a = p.nebulaAlpha * (k / (double)rings) * 0.8;
            g.setFill(base.deriveColor(0, 1, 1, a));
            g.fillOval(cx - rr, cy - rr, rr * 2, rr * 2);
        }
    }

    private static void renderMilkyWayBand(GraphicsContext g, int W, int H, int cell, Random rng, Params p) {
        final int stripW = 4 * cell;
        final int stripH = cell;

        Canvas band = new Canvas(stripW, stripH);
        GraphicsContext bg = band.getGraphicsContext2D();

        double centerY = stripH * (0.5 + p.mwVOffset);
        double sigma   = Math.max(1.0, p.mwThickness);
        double twoSigma2 = 2.0 * sigma * sigma;

        int noisePeriod = 64;
        double hue = p.mwHue;
        double sat = clamp01(p.mwSaturation);
        double baseAlpha = clamp01(p.mwOpacity);
        double intensity = Math.max(0.0, p.mwIntensity);
        double noiseAmt  = clamp01(p.mwNoise);

        for (int x = 0; x < stripW; x++) {
            double nx = (Math.sin((x / (double)noisePeriod) * Math.PI * 2.0) * 0.5 + 0.5);
            nx = (1.0 - noiseAmt) + noiseAmt * nx;
            for (int y = 0; y < stripH; y++) {
                double dy = y - centerY;
                double gauss = Math.exp(-(dy*dy)/twoSigma2);
                double a = baseAlpha * gauss * nx * intensity;
                if (a < 0.01) continue;
                Color c = Color.hsb(hue, sat, 1.0, clamp01(a));
                bg.getPixelWriter().setColor(x, y, c);
            }
        }

        if (Math.abs(p.mwTiltDeg) > 0.01) {
            SnapshotParameters sp = new SnapshotParameters();
            sp.setFill(Color.TRANSPARENT);
            WritableImage raw = band.snapshot(sp, null);

            Canvas tilted = new Canvas(stripW, stripH);
            GraphicsContext tg = tilted.getGraphicsContext2D();
            tg.save();
            tg.translate(stripW/2.0, stripH/2.0);
            tg.rotate(p.mwTiltDeg);
            tg.translate(-stripW/2.0, -stripH/2.0);
            tg.drawImage(raw, 0, 0);
            tg.restore();
            band = tilted;
        }

        SnapshotParameters sp2 = new SnapshotParameters();
        sp2.setFill(Color.TRANSPARENT);
        WritableImage bandImg = band.snapshot(sp2, null);

        g.drawImage(bandImg, 0, cell);
    }

    private static void renderDebugGrid(GraphicsContext g, int W, int H, int cell, Params p) {
        double a = clamp01(p.debugAlpha);
        g.setGlobalAlpha(a);
        g.setStroke(Color.color(0.3, 1.0, 0.3, a));
        g.setLineWidth(2.0);
        for (int c = 0; c <= 4; c++) g.strokeLine(c * cell, 0, c * cell, H);
        for (int r = 0; r <= 3; r++) g.strokeLine(0, r * cell, W, r * cell);

        g.setGlobalAlpha(0.9);
        g.setFill(Color.color(0.2, 1.0, 0.2, 0.9));
        g.setFont(javafx.scene.text.Font.font(24));
        label(g, "TOP",    cell*1, cell*0, cell);
        label(g, "LEFT",   cell*0, cell*1, cell);
        label(g, "FRONT",  cell*1, cell*1, cell);
        label(g, "RIGHT",  cell*2, cell*1, cell);
        label(g, "BACK",   cell*3, cell*1, cell);
        label(g, "BOTTOM", cell*1, cell*2, cell);
        g.setGlobalAlpha(1.0);
    }

    private static void label(GraphicsContext g, String txt, int x, int y, int cell) {
        double cx = x + cell/2.0, cy = y + cell/2.0;
        g.setFill(Color.color(0,0,0,0.8)); g.fillText(txt, cx-40+1, cy+1);
        g.setFill(Color.color(0.2,1.0,0.2,0.9)); g.fillText(txt, cx-40, cy);
    }

    private static void addInternalBleed(WritableImage img, int cell, int bleed) {
        var pw = img.getPixelWriter();
        var pr = img.getPixelReader();
        int[][] cells = {{1,0},{0,1},{1,1},{2,1},{3,1},{1,2}};
        for (int[] rc : cells) {
            int cx = rc[0]*cell, cy = rc[1]*cell;
            for (int x = 0; x < bleed; x++)
                for (int y = 0; y < cell; y++)
                    pw.setColor(cx + x, cy + y, pr.getColor(cx + bleed, cy + y));
            for (int x = 0; x < bleed; x++)
                for (int y = 0; y < cell; y++)
                    pw.setColor(cx + cell - 1 - x, cy + y, pr.getColor(cx + cell - bleed - 1, cy + y));
            for (int y = 0; y < bleed; y++)
                for (int x = 0; x < cell; x++)
                    pw.setColor(cx + x, cy + y, pr.getColor(cx + x, cy + bleed));
            for (int y = 0; y < bleed; y++)
                for (int x = 0; x < cell; x++)
                    pw.setColor(cx + x, cy + cell - 1 - y, pr.getColor(cx + x, cy + cell - bleed - 1));
        }
    }

    // ----- color & math -----
    private static Color pickStarColor(Random rng, double colorBias) {
        if (rng.nextDouble() >= colorBias) return Color.WHITE;
        switch (rng.nextInt(4)) {
            case 0: return Color.rgb(255,236,210);
            case 1: return Color.rgb(215,235,255);
            case 2: return Color.rgb(255,210,240);
            default:return Color.rgb(205,255,235);
        }
    }
    private static Color pickNebulaColor(Random rng) {
        switch (rng.nextInt(4)) {
            case 0: return Color.rgb( 80,120,255);
            case 1: return Color.rgb(255,110,150);
            case 2: return Color.rgb(255,190, 90);
            default:return Color.rgb(100,255,190);
        }
    }
    private static double gaussian(Random r, double mean, double sigma) {
        double u1 = 1.0 - r.nextDouble();
        double u2 = 1.0 - r.nextDouble();
        double z = Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2*Math.PI*u2);
        return mean + z * sigma;
    }
    private static double clamp(double v, double lo, double hi) {
        return (v < lo) ? lo : (v > hi ? hi : v);
    }
    private static double clamp01(double v) {
        return (v < 0) ? 0 : (v > 1 ? 1 : v);
    }
    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
