package AsteroidField;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        root.setBackground(Background.EMPTY);
        // 3D Asteroid Field View
        AsteroidField3DView fieldView = new AsteroidField3DView();
        root.setCenter(fieldView);

        // Bottom: Controls 
        HBox controlsBox = fieldView.createControls(); // Use the dynamic controls
//        controlsBox.setId("controls-box");
        root.setBottom(controlsBox);

        // Scene with your custom CSS
        Scene scene = new Scene(root, 1024, 768, true);
        scene.setFill(Color.BLACK);
        String CSS = StyleResourceProvider.getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(CSS);
        
        primaryStage.setTitle("Asteroid Field Demo");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}