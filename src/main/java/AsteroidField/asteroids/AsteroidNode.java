package AsteroidField.asteroids;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import AsteroidField.asteroids.geometry.Portal;
import javafx.scene.Group;
import javafx.scene.shape.MeshView;

/** Group for multi-part asteroids (outer + inner MeshViews) with metadata. */
public final class AsteroidNode extends Group {

    private final MeshView outerShellView;
    private final MeshView innerShellView;
    private final double innerRadius;
    private final double outerRadius;
    private final List<Portal> portals;

    public AsteroidNode(MeshView outerShellView,
                        MeshView innerShellView,
                        double innerRadius,
                        double outerRadius,
                        List<Portal> portals) {
        if (outerShellView == null) throw new IllegalArgumentException("outerShellView is null");
        if (innerShellView == null) throw new IllegalArgumentException("innerShellView is null");
        if (outerRadius <= innerRadius) throw new IllegalArgumentException("outerRadius <= innerRadius");

        this.outerShellView = outerShellView;
        this.innerShellView = innerShellView;
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
        this.portals = (portals == null)
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(portals));

        getChildren().addAll(outerShellView, innerShellView);
    }

    public MeshView getOuterShellView() { return outerShellView; }
    public MeshView getInnerShellView() { return innerShellView; }
    public double getInnerRadius() { return innerRadius; }
    public double getOuterRadius() { return outerRadius; }
    public List<Portal> getPortals() { return portals; }
}
