package AsteroidField;

import AsteroidField.ui.CameraViewOverlay;
import AsteroidField.ui.CameraViewLite;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.setBackground(Background.EMPTY);

        // --- Main 3D view and existing UI ---
        AsteroidField3DView fieldView = new AsteroidField3DView();
        HBox sceneControls    = fieldView.createSceneControls();
        HBox asteroidControls = fieldView.createControls();
        VBox dynamicControls  = fieldView.getDynamicFamilyBox();

        root.setTop(sceneControls);
        root.setBottom(asteroidControls);
        root.setRight(dynamicControls);
        root.setLeft(fieldView.getTetherTuningBox());

        // --- HUD sizes ---
        final double HUD_W = 360;
        final double HUD_H = 240;

        // --- Overlays (each hosts its own off-graph camera) ---
        CameraViewOverlay topOverlay  = new CameraViewOverlay(fieldView.getSubScene(), "Top View",  HUD_W, HUD_H);
        CameraViewOverlay sideOverlay = new CameraViewOverlay(fieldView.getSubScene(), "Side View", HUD_W, HUD_H);

        // Compute scene center (eye/target coordinates)
        Bounds b  = fieldView.getWorldRoot().getBoundsInParent();
        double cx = (b.getMinX() + b.getMaxX()) * 0.5;
        double cy = (b.getMinY() + b.getMaxY()) * 0.5;
        double cz = (b.getMinZ() + b.getMaxZ()) * 0.5;

        // Desired fixed vantage distance from target
        final double DIST = 1200;   // tune to taste; larger → wider area in view
        final double FOV  = 60;     // vertical FOV; try 45–75 to taste

        // --- Pose the mini cameras with a proper lookAt ---
        CameraViewLite topView  = topOverlay.getCameraView();
        CameraViewLite sideView = sideOverlay.getCameraView();

//        // Top: from above (y = -DIST), looking down (+Y) at (cx,cy,cz)
//        CameraViewLite.poseLookAt(topView,  cx, -DIST, cz,   cx, cy, cz,  FOV);
//        // Side: from left (x = cx - DIST), looking right (+X) at (cx,cy,cz)
//        CameraViewLite.poseLookAt(sideView, cx - DIST, cy, cz,   cx, cy, cz,  FOV);

        // Top-down (+Y): from above
        CameraViewLite.poseLookAt(topView,  cx, cy - DIST, cz,  cx, cy, cz,  FOV);
        // Side (+X): from left
        CameraViewLite.poseLookAt(sideView, cx - DIST, cy, cz,  cx, cy, cz,  FOV);

        // --- Stack overlays over the main 3D view ---
        StackPane centerStack = new StackPane(fieldView, topOverlay, sideOverlay);
        StackPane.setAlignment(topOverlay,  Pos.TOP_LEFT);
        StackPane.setAlignment(sideOverlay, Pos.TOP_RIGHT);
        Insets hudPad = new Insets(12);
        StackPane.setMargin(topOverlay, hudPad);
        StackPane.setMargin(sideOverlay, hudPad);
        root.setCenter(centerStack);

        // --- Scene ---
        Scene scene = new Scene(root, 1600, 800, true);
        scene.setFill(Color.BLACK);
        String CSS = StyleResourceProvider.getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(CSS);

        primaryStage.setTitle("Asteroid Field Demo");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Start mini views AFTER layout so fitWidth/fitHeight are valid
        Platform.runLater(() -> {
            topOverlay.start();
            sideOverlay.start();
        });
    }

    public static void main(String[] args) { launch(args); }
}
