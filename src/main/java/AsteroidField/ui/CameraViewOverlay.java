package AsteroidField.ui;

import javafx.geometry.Insets;
import javafx.scene.SubScene;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

public class CameraViewOverlay extends TitledPane {
    private final CameraViewLite camView;
    private final BorderPane contentPane;

    public CameraViewOverlay(SubScene subScene, String title, double prefW, double prefH) {
        contentPane = new BorderPane();
        setText(title != null ? title : "MiniCam");
        setCollapsible(true);
        setExpanded(true);

        camView = new CameraViewLite(subScene);
        camView.setPreserveRatio(true);
        camView.setSmooth(true);
        camView.setFitWidth(prefW);
        camView.setFitHeight(prefH);

        contentPane.setPadding(new Insets(6));
        contentPane.setCenter(camView);
        setContent(contentPane);

        // Fix size so StackPane doesn't stretch it
        setPrefSize(prefW, prefH + 28); // ~title bar; tweak to your CSS
        setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setBackground(new Background(
            new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));
    }

    public void start() { camView.startViewing(); }
    public void stop()  { camView.pause(); }

    public CameraViewLite getCameraView() { return camView; }

    /** Resize HUD later if needed (and propagate to the view). */
    public void setOverlaySize(double w, double h) {
        camView.setFitWidth(w);
        camView.setFitHeight(h);
        setPrefSize(w, h + 28);
        setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    }
}
