package AsteroidField.ui;

import AsteroidField.events.CollisionEvent;
import AsteroidField.events.GameEventBus;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.util.Duration;

/** Small HUD banner that shows transient text on game events. */
public final class HudText extends StackPane {

    private final Label label = new Label("");
    private SequentialTransition currentAnim;

    // Default listener: show a line when the ship collides
    private final EventHandler<CollisionEvent> onShipCollision = e -> {
        double s = e.getRelativeSpeed();
        String msg = String.format("Collision: %.1f m/s  at (%.0f, %.0f, %.0f)",
                s, e.getPositionWorld().getX(), e.getPositionWorld().getY(), e.getPositionWorld().getZ());
        Color c = (s >= 90) ? Color.ORANGERED : (s >= 40 ? Color.GOLD : Color.LIGHTGREEN);
        show(msg, c);
    };

    public HudText() {
        setPickOnBounds(false);
        setMouseTransparent(true); // let mouse pass through to the 3D view
        setAlignment(Pos.TOP_LEFT);
        setPadding(new Insets(10, 10, 10, 10));

        label.setTextFill(Color.WHITE);
        label.setFont(Font.font("Consolas", 16));
        label.setStyle("-fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 8, 0.5, 0, 0);");
        label.setVisible(false);
        getChildren().add(label);

        // Subscribe to the bus
        GameEventBus.addHandler(CollisionEvent.SHIP_COLLISION, onShipCollision);
    }

    /** Show a message briefly, then fade out. */
    public void show(String text, Color color) {
        label.setText(text);
        label.setTextFill(color);
        label.setVisible(true);

        if (currentAnim != null) currentAnim.stop();

        FadeTransition fadeIn = new FadeTransition(Duration.millis(90), label);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);

        PauseTransition hold = new PauseTransition(Duration.millis(650));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(350), label);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setOnFinished(e -> label.setVisible(false));

        currentAnim = new SequentialTransition(fadeIn, hold, fadeOut);
        currentAnim.playFromStart();
    }

    /** Unsubscribe when removing the HUD to avoid leaks. */
    public void dispose() {
        GameEventBus.removeHandler(CollisionEvent.SHIP_COLLISION, onShipCollision);
    }
}
