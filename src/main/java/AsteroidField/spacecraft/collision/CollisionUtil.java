package AsteroidField.spacecraft.collision;

import AsteroidField.asteroids.AsteroidNode;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.shape.MeshView;

/** Small helpers for collision. */
public final class CollisionUtil {
    private CollisionUtil() {}

    public static List<MeshView> collectMeshViews(Node n) {
        List<MeshView> out = new ArrayList<>();
        collectMeshViews(n, out);
        return out;
    }

    public static void collectMeshViews(Node n, List<MeshView> out) {
        if (n instanceof MeshView mv) {
            out.add(mv);
            return;
        }
        if (n instanceof AsteroidNode an) {
            out.add(an.getOuterShellView());
            out.add(an.getInnerShellView());
        } else if (n instanceof Parent p) {
            for (Node c : p.getChildrenUnmodifiable()) {
                collectMeshViews(c, out);
            }
        }
    }
}
