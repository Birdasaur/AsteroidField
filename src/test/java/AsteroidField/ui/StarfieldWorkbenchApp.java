package AsteroidField.ui;

import AsteroidField.ui.components.StarfieldGeneratorPane;
import AsteroidField.ui.scene3d.StarAtlasGenerator;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class StarfieldWorkbenchApp extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("Starfield Workbench — 4×3 Skybox Atlas");

        StarfieldGeneratorPane pane = new StarfieldGeneratorPane();

        // --- Buttons ---
        Button btnSaveRaw = new Button("Save Full 4×3 Atlas (RAW)…");
        btnSaveRaw.setOnAction(e -> {
            try {
                // FULL-RES, FRESH 4×3 CROSS — NOT the preview
                var params = pane.getParams();
                var img = StarAtlasGenerator.generate(params);

                FileChooser fc = new FileChooser();
                fc.setTitle("Save Full 4×3 Atlas (RAW)");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
                fc.setInitialFileName("stars_atlas-4k.png");
                File f = fc.showSaveDialog(stage);
                if (f != null) {
                    StarAtlasGenerator.savePngFlattened(img, f);
                    System.out.println("Saved RAW atlas: " + f.getAbsolutePath()
                            + " (" + (int)img.getWidth() + "×" + (int)img.getHeight() + ")");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Button btnExportFaces = new Button("Export Faces (6 PNGs) …");
        btnExportFaces.setOnAction(e -> {
            try {
                // Generate fresh atlas, then slice faces
                var params = pane.getParams();
                var faces = StarAtlasGenerator.generateFaces(params);

                DirectoryChooser dc = new DirectoryChooser();
                dc.setTitle("Choose Export Directory");
                File dir = dc.showDialog(stage);
                if (dir != null) {
                    StarAtlasGenerator.saveFaces(faces, dir, "stars");
                    System.out.println("Exported faces to: " + dir.getAbsolutePath());
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Button btnSavePreview = new Button("Save Preview Snapshot…");
        btnSavePreview.setOnAction(e -> {
            try {
                // Whatever is currently shown in the preview (for mockups only)
                var img = pane.getAtlasImage(); // NOTE: in our pane this calls generate(params)
                FileChooser fc = new FileChooser();
                fc.setTitle("Save Preview Snapshot");
                fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
                fc.setInitialFileName("stars_preview.png");
                File f = fc.showSaveDialog(stage);
                if (f != null) {
                    StarAtlasGenerator.savePngFlattened(img, f);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        ToolBar tb = new ToolBar(btnSaveRaw, btnExportFaces, btnSavePreview);

        BorderPane root = new BorderPane(pane);
        root.setTop(tb);
        BorderPane.setMargin(pane, new Insets(0));

        Scene scene = new Scene(root, 1200, 820, true);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
