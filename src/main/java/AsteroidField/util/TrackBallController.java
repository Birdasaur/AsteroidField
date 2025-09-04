package AsteroidField.util;

import javafx.scene.SubScene;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.Node;
import javafx.scene.shape.MeshView;
import javafx.scene.transform.Rotate;
import java.util.List;
import javafx.scene.PerspectiveCamera;
import javafx.scene.input.MouseButton;

public class TrackBallController {

    private final SubScene scene;
    private final PerspectiveCamera camera;
    private final List<MeshView> asteroidViews;
    private MeshView selected = null;

    // Arcball angles
    private double anchorX, anchorY, anchorYaw, anchorPitch;
    private double yaw = 0, pitch = 0;

    private Rotate rotateYaw = new Rotate(0, Rotate.Y_AXIS);
    private Rotate rotatePitch = new Rotate(0, Rotate.X_AXIS);

    private boolean enabled = false;

    public TrackBallController(SubScene scene, PerspectiveCamera camera, List<MeshView> asteroidViews) {
        this.scene = scene;
        this.camera = camera;
        this.asteroidViews = asteroidViews;
    }

    public void setEnabled(boolean on) {
        enabled = on;
        if (on) {
            scene.setOnMousePressed(this::onMousePressed);
            scene.setOnMouseDragged(this::onMouseDragged);
            scene.setOnScroll(this::onScroll);
            scene.setOnMouseClicked(this::onMouseClicked);
        } else {
            scene.setOnMousePressed(null);
            scene.setOnMouseDragged(null);
            scene.setOnScroll(null);
            scene.setOnMouseClicked(null);
        }
    }

    private void onMouseClicked(MouseEvent event) {
        if (event.isControlDown() && event.getButton() == MouseButton.PRIMARY) {
            Node picked = event.getPickResult().getIntersectedNode();
            if (picked instanceof MeshView && asteroidViews.contains(picked)) {
                setSelected((MeshView)picked);
            }
        }
    }

    private void onMousePressed(MouseEvent event) {
        if (!enabled || selected == null) return;
        anchorX = event.getSceneX();
        anchorY = event.getSceneY();
        anchorYaw = yaw;
        anchorPitch = pitch;
    }

    private void onMouseDragged(MouseEvent event) {
        if (!enabled || selected == null) return;
        double deltaX = event.getSceneX() - anchorX;
        double deltaY = event.getSceneY() - anchorY;

        yaw = anchorYaw + deltaX;
        pitch = anchorPitch - deltaY;
        pitch = Math.max(-90, Math.min(90, pitch)); // clamp

        // Update transforms
        rotateYaw.setAngle(yaw);
        rotatePitch.setAngle(pitch);

        // Ensure the two rotates are the only transforms on the mesh (order: Y then X)
        if (selected.getTransforms().size() < 2 ||
                selected.getTransforms().get(0) != rotateYaw ||
                selected.getTransforms().get(1) != rotatePitch) {
            selected.getTransforms().clear();
            selected.getTransforms().addAll(rotateYaw, rotatePitch);
        }
    }

    private void onScroll(ScrollEvent event) {
        if (!enabled) return;
        double delta = event.getDeltaY();
        camera.setTranslateZ(camera.getTranslateZ() + delta * 0.5);
    }

    public MeshView getSelected() { return selected; }

    public void setSelected(MeshView meshView) {
        this.selected = meshView;
        yaw = 0;
        pitch = 0;
        rotateYaw.setAngle(yaw);
        rotatePitch.setAngle(pitch);

        if (selected != null) {
            selected.getTransforms().clear();
            selected.getTransforms().addAll(rotateYaw, rotatePitch);
        }
    }
}
