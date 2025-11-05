package AsteroidField;

import AsteroidField.ui.scene3d.Skybox;
import AsteroidField.util.ResourceUtils;
import java.io.IOException;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

public class Game3DView extends Pane {

    private final Group worldRoot;
    private final PerspectiveCamera camera;
    private final SubScene subScene;

    // NEW: optional craft proxy we can show/hide while docking
    private Node craftProxy;

    public Game3DView() {
        this.worldRoot = new Group();

        this.camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(100000.0);
        camera.setFieldOfView(60);
        camera.setTranslateZ(-800);

        this.subScene = new SubScene(worldRoot, 800, 600, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.BLACK);
        subScene.setCamera(camera);

        getChildren().add(subScene);
        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());
        
        // Load atlas for Skybox
        Image atlas;
        try {
            atlas = ResourceUtils.load3DTextureImage("planar-skybox");
            double skySize = 20000;
            Skybox sky = new Skybox(atlas, skySize, camera);
            worldRoot.getChildren().add(sky);        
        } catch (IOException ex) {
            System.getLogger(Game3DView.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }

    // --- New helpers for Step 4 ---
    /** Provide a craft proxy node to the world so we can hide/show it when docked. */
    public void setCraftProxy(Node proxy) {
        if (craftProxy != null) worldRoot.getChildren().remove(craftProxy);
        craftProxy = proxy;
        if (craftProxy != null && !worldRoot.getChildren().contains(craftProxy)) {
            worldRoot.getChildren().add(craftProxy);
        }
    }
    public Node getCraftProxy() { return craftProxy; }
    public void setCraftProxyVisible(boolean v) {
        if (craftProxy != null) craftProxy.setVisible(v);
    }

    public SubScene getSubScene() { return subScene; }
    public Group getWorldRoot() { return worldRoot; }
    public PerspectiveCamera getCamera() { return camera; }
}
