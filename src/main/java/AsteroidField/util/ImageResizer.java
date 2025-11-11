package AsteroidField.util;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public final class ImageResizer {
    private ImageResizer() {}

    /** Resample an Image to target size using JavaFX Canvas (image smoothing on/off). */
    public static WritableImage resample(Image src, int targetW, int targetH, boolean smooth) {
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
    }

    /** Scale by a scalar (e.g., 2.0 = double size). */
    public static WritableImage scale(Image src, double scale, boolean smooth) {
        int w = (int)Math.round(src.getWidth() * scale);
        int h = (int)Math.round(src.getHeight() * scale);
        return resample(src, w, h, smooth);
    }
}
