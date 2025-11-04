package AsteroidField.ui.overlay;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Compile-safe, dependency-free placeholder for your terminal overlay.
 * We’ll replace this with your LitPathPane/TextPane in a later step.
 */
final class PlaceholderTextConsole extends StackPane {
    private final Label header;
    private final Label content;
    private final ScrollPane scroller;

    PlaceholderTextConsole() {
        setMaxWidth(700);
        setMaxHeight(360);
        setPadding(new Insets(10));
        setBackground(new Background(new BackgroundFill(Color.color(0, 0, 0, 0.82),
                new CornerRadii(8), Insets.EMPTY)));
        setBorder(new Border(new BorderStroke(Color.CYAN, BorderStrokeStyle.SOLID,
                new CornerRadii(8), BorderStroke.THIN)));
        setStyle("-fx-effect: dropshadow(gaussian, rgba(0,255,255,0.4), 20, 0.3, 0, 0);");

        header = new Label("=== COLONYNET TERMINAL (placeholder) ===");
        header.setTextFill(Color.CYAN);

        content = new Label();
        content.setTextFill(Color.LIGHTCYAN);
        content.setWrapText(true);

        scroller = new ScrollPane(content);
        scroller.setFitToWidth(true);
        scroller.setPrefViewportWidth(660);
        scroller.setPrefViewportHeight(260);
        scroller.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        BorderPane bp = new BorderPane();
        bp.setTop(header);
        BorderPane.setAlignment(header, Pos.CENTER);
        bp.setCenter(scroller);
        bp.setPadding(new Insets(8));

        getChildren().add(bp);

        // simple drag-to-move UX so it behaves like a floating pane
        final Delta drag = new Delta();
        setOnMousePressed(e -> {
            drag.x = getTranslateX() - e.getSceneX();
            drag.y = getTranslateY() - e.getSceneY();
            setCursor(Cursor.MOVE);
            toFront();
        });
        setOnMouseDragged(e -> {
            setTranslateX(drag.x + e.getSceneX());
            setTranslateY(drag.y + e.getSceneY());
        });
        setOnMouseReleased(e -> setCursor(Cursor.DEFAULT));
    }

    void setText(String text) {
        content.setText(text != null ? text : "");
        scroller.setVvalue(1.0);
    }

    void appendText(String text) {
        if (text == null || text.isEmpty()) return;
        String existing = content.getText();
        content.setText(existing == null || existing.isEmpty() ? text : existing + text);
        scroller.setVvalue(1.0);
    }

    void slideIn() {
        setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(180), this);
        ft.setToValue(1.0);
        ft.play();
    }

    private static final class Delta { double x, y; }
}
