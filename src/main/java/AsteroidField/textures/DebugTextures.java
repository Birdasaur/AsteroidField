package AsteroidField.textures;

import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.transform.Transform;

public final class DebugTextures {
    private DebugTextures() {}

    /** Makes a labeled square test image (faceName printed, border, center cross, checker). */
    public static Image makeFace(String faceName, int size) {
        Canvas c = new Canvas(size, size);
        GraphicsContext g = c.getGraphicsContext2D();

        // background
        g.setFill(Color.BLACK);
        g.fillRect(0, 0, size, size);

        // checkerboard
        int tile = Math.max(16, size / 16);
        for (int y = 0; y < size; y += tile) {
            for (int x = 0; x < size; x += tile) {
                if (((x / tile) + (y / tile)) % 2 == 0) {
                    g.setFill(Color.gray(0.15));
                } else {
                    g.setFill(Color.gray(0.25));
                }
                g.fillRect(x, y, tile, tile);
            }
        }

        // center cross
        g.setStroke(Color.LIME);
        g.setLineWidth(3);
        g.strokeLine(0, size / 2.0, size, size / 2.0);
        g.strokeLine(size / 2.0, 0, size / 2.0, size);

        // thick border
        g.setStroke(Color.RED);
        g.setLineWidth(20);
        g.strokeRect(10, 10, size - 20, size - 20);

        // label
        g.setFill(Color.WHITE);
        g.setFont(Font.font("Arial", Math.max(24, size / 10)));
        g.fillText(faceName, 20, size * 0.15);

        // snapshot to Image
        SnapshotParameters sp = new SnapshotParameters();
        sp.setTransform(Transform.scale(1, 1));
        sp.setFill(Color.TRANSPARENT);
        return c.snapshot(sp, null);
    }
}
