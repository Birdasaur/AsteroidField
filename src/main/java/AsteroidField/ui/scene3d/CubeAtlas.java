package AsteroidField.ui.scene3d;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;

public final class CubeAtlas {
    private CubeAtlas() {}

    public record Faces(
        Image top, Image bottom, Image left, Image right, Image front, Image back
    ) {}

    /** Slice a 4x3 horizontal cross atlas into six square faces. */
    public static Faces slice(Image atlas) {
        final int W = (int)Math.round(atlas.getWidth());
        final int H = (int)Math.round(atlas.getHeight());
        final int cellW = W / 4;
        final int cellH = H / 3;
        if (cellW <= 0 || cellH <= 0 || cellW != cellH || W != cellW*4 || H != cellH*3) {
            throw new IllegalArgumentException("Atlas must be 4x3 grid of equal squares. Got: "
                + W + "x" + H + " (cellW=" + cellW + ", cellH=" + cellH + ")");
        }
        final int s = cellW;
        PixelReader pr = atlas.getPixelReader();
        if (pr == null) throw new IllegalArgumentException("Atlas has no pixel reader");

        // mapping (x,y) in cells
        WritableImage top    = new WritableImage(pr, 1*s, 0*s, s, s);
        WritableImage left   = new WritableImage(pr, 0*s, 1*s, s, s);
        WritableImage front  = new WritableImage(pr, 1*s, 1*s, s, s);
        WritableImage right  = new WritableImage(pr, 2*s, 1*s, s, s);
        WritableImage back   = new WritableImage(pr, 3*s, 1*s, s, s);
        WritableImage bottom = new WritableImage(pr, 1*s, 2*s, s, s);

        return new Faces(top, bottom, left, right, front, back);
    }
}
