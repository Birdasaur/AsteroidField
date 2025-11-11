package AsteroidField.ui.scene3d;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * Slices a 4x3 horizontal-cross atlas into six cube faces.
 * Layout (each cell is square):
 *
 *           [   top   ]
 *  [ left ][ front ][ right ][ back ]
 *           [  bottom ]
 *
 * Coordinates in cells (cx, cy):
 *   top    = (1, 0)
 *   left   = (0, 1)
 *   front  = (1, 1)
 *   right  = (2, 1)
 *   back   = (3, 1)
 *   bottom = (1, 2)
 *
 * In addition to the basic slice, this class provides:
 *  - sliceScaled(atlas, scale, smooth): upscales/downscales the atlas once, then slices.
 *  - sliceAndScaleFaces(atlas, faceScale, smooth): slices, then scales each face independently.
 *
 * Notes:
 *  - Scaling > 1 increases memory usage. 8192x6144 RGBA ~ 192 MB in RAM.
 *  - Upscaling can improve on-screen texel density on very large skyboxes but does not add detail.
 */
public final class CubeAtlas {
    private CubeAtlas() {}

    /** Six faces of a cubemap. */
    public record Faces(Image top, Image bottom, Image left, Image right, Image front, Image back) {}

    /** Slice a 4x3 horizontal cross atlas into six square faces (no scaling). */
    public static Faces slice(Image atlas) {
        return sliceScaled(atlas, 1.0, true);
    }

    /**
     * Slice but first apply a uniform scalar to the atlas, then slice.
     * Example: scale=2.0 turns a 4096x3072 atlas into 8192x6144;
     *          face size goes from 1024x1024 to 2048x2048.
     *
     * @param atlas  4x3 cross atlas, cells must be square.
     * @param scale  > 0, e.g., 0.5, 1.0, 1.5, 2.0
     * @param smooth true = bilinear, false = nearest
     */
    public static Faces sliceScaled(Image atlas, double scale, boolean smooth) {
        if (atlas == null) throw new IllegalArgumentException("atlas is null");
        if (scale <= 0) throw new IllegalArgumentException("scale must be > 0");

        final Image source = (scale == 1.0) ? atlas : scale(atlas, scale, smooth);
        return doSlice(source);
    }

    /**
     * Slice first, then scale each face independently by faceScale.
     * Useful if you want to keep the original atlas unchanged, or apply different filters.
     *
     * @param atlas      4x3 cross atlas
     * @param faceScale  > 0; e.g., 2.0 makes each face 2x its original size
     * @param smooth     true = bilinear, false = nearest
     */
    public static Faces sliceAndScaleFaces(Image atlas, double faceScale, boolean smooth) {
        if (atlas == null) throw new IllegalArgumentException("atlas is null");
        if (faceScale <= 0) throw new IllegalArgumentException("faceScale must be > 0");

        Faces f = doSlice(atlas);
        if (faceScale == 1.0) return f;

        return new Faces(
            scale(f.top(),    faceScale, smooth),
            scale(f.bottom(), faceScale, smooth),
            scale(f.left(),   faceScale, smooth),
            scale(f.right(),  faceScale, smooth),
            scale(f.front(),  faceScale, smooth),
            scale(f.back(),   faceScale, smooth)
        );
    }

    // --- Core slicing (no scaling here) ---
    private static Faces doSlice(Image atlas) {
        final int W = (int)Math.round(atlas.getWidth());
        final int H = (int)Math.round(atlas.getHeight());
        final int cellW = W / 4;
        final int cellH = H / 3;

        if (cellW <= 0 || cellH <= 0 || cellW != cellH || W != cellW * 4 || H != cellH * 3) {
            throw new IllegalArgumentException(
                "Atlas must be 4x3 grid of equal squares. Got: " + W + "x" + H +
                " (cellW=" + cellW + ", cellH=" + cellH + ")"
            );
        }

        final int s = cellW;
        PixelReader pr = atlas.getPixelReader();
        if (pr == null) throw new IllegalArgumentException("Atlas has no PixelReader");

        WritableImage top    = new WritableImage(pr, 1*s, 0*s, s, s);
        WritableImage left   = new WritableImage(pr, 0*s, 1*s, s, s);
        WritableImage front  = new WritableImage(pr, 1*s, 1*s, s, s);
        WritableImage right  = new WritableImage(pr, 2*s, 1*s, s, s);
        WritableImage back   = new WritableImage(pr, 3*s, 1*s, s, s);
        WritableImage bottom = new WritableImage(pr, 1*s, 2*s, s, s);

        return new Faces(top, bottom, left, right, front, back);
    }

    // --- Local resampling helpers (Canvas snapshot; no external utility needed) ---

    /** Scale by a scalar (e.g., 2.0 = double size). */
    private static WritableImage scale(Image src, double factor, boolean smooth) {
        int w = Math.max(1, (int)Math.round(src.getWidth()  * factor));
        int h = Math.max(1, (int)Math.round(src.getHeight() * factor));
        return resample(src, w, h, smooth);
    }

    /** Resample an Image to target size using JavaFX Canvas. */
    private static WritableImage resample(Image src, int targetW, int targetH, boolean smooth) {
        if (targetW <= 0 || targetH <= 0) throw new IllegalArgumentException("Invalid target size");

        Canvas c = new Canvas(targetW, targetH);
        GraphicsContext g = c.getGraphicsContext2D();
        g.setImageSmoothing(smooth);
        g.drawImage(src, 0, 0, targetW, targetH);

        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);

        WritableImage out = new WritableImage(targetW, targetH);
        c.snapshot(sp, out);
        return out;
        // Note: If you prefer nearest-neighbor strictly, set smooth=false.
        // For crisp star pixels, nearest can be desirable; for nebula, bilinear looks better.
    }
}
