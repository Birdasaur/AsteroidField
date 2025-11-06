package AsteroidField;

import AsteroidField.physics.PhysicsSystem;
import AsteroidField.spacecraft.CameraKinematicAdapter;
import AsteroidField.spacecraft.collision.SpacecraftCollisionContributor;
import AsteroidField.spacecraft.control.ThrusterController;
import AsteroidField.tether.TetherController;
import AsteroidField.textures.DebugTextures;
import AsteroidField.ui.scene3d.CubeAtlas;
import AsteroidField.ui.scene3d.Grid3D;
import AsteroidField.ui.scene3d.Skybox;
import AsteroidField.util.FpsLookController;
import AsteroidField.util.ResourceUtils;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;

/**
 * Playable-demo 3D view:
 * - Owns world root, camera, subscene, skybox
 * - Hosts PhysicsSystem (120 Hz) and wires contributors:
 *     CameraKinematicAdapter, ThrusterController, TetherController, Ship Collisions
 * - Adds FpsLookController (default ON) for mouse-look
 * - Exposes helpers to pause/resume physics and show/hide a craft proxy
 */
public class Game3DView extends Pane {

    private final Group worldRoot;
    private final PerspectiveCamera camera;
    private final SubScene subScene;

    // Physics
    private final PhysicsSystem physics;

    // Craft rig (camera-centered kinematic adapter)
    private final CameraKinematicAdapter craft;

    // Contributors
    private final ThrusterController thrusters;
    private final TetherController tethers;
    private final SpacecraftCollisionContributor shipCollisions;

    // View controls
    private final FpsLookController fpsLook; // NEW

    // Optional craft proxy we can show/hide while docking
    private Node craftProxy;

    public Game3DView() {
        // --- World root + camera + subscene ---
        this.worldRoot = new Group();

        this.camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(200000.0);
        camera.setFieldOfView(45);
        camera.setTranslateZ(-800);
        camera.setTranslateY(-400);

        this.subScene = new SubScene(worldRoot, 800, 600, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.GAINSBORO);
        subScene.setCamera(camera);

        getChildren().add(subScene);
        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());

        // --- Skybox (your existing setup) ---
        double skySize = 10000D;
        try {
//            Image atlas = ResourceUtils.load3DTextureImage("planar-skybox");
//            Image atlas = ResourceUtils.load3DTextureImage("tycho-skybox");
            Image atlas = ResourceUtils.load3DTextureImage("stars_atlas_4k_colorC_gaussian_small");


            //            Skybox sky = new Skybox(atlas, skySize, camera);

            
// Slice -> six faces
CubeAtlas.Faces f = CubeAtlas.slice(atlas);
// Build skybox via MULTIPLE path (everything else stays the same)
Skybox sky = new Skybox(
    f.top(), f.bottom(), f.left(), f.right(), f.front(), f.back(), skySize, camera
);

//// Force MULTIPLE path with debug faces (e.g., 512x512)
//int s = 1024;
//Image top    = DebugTextures.makeFace("TOP", s);
//Image bottom = DebugTextures.makeFace("BOTTOM", s);
//Image left   = DebugTextures.makeFace("LEFT", s);
//Image right  = DebugTextures.makeFace("RIGHT", s);
//Image front  = DebugTextures.makeFace("FRONT", s);
//Image back   = DebugTextures.makeFace("BACK", s);
//
            
//Image top    = ResourceUtils.load3DTextureImage("top");
//Image bottom = ResourceUtils.load3DTextureImage("bottom");
//Image right   = ResourceUtils.load3DTextureImage("left");
//Image left  = ResourceUtils.load3DTextureImage("right");
//Image front  = ResourceUtils.load3DTextureImage("front");
//Image back   = ResourceUtils.load3DTextureImage("back");
//
//Skybox sky = new Skybox(top, bottom, left, right, front, back, skySize, camera);

            worldRoot.getChildren().add(sky);
        } catch (IOException ex) {
            System.getLogger(Game3DView.class.getName())
                  .log(System.Logger.Level.ERROR, (String) null, ex);
        }
        
Grid3D grid = new Grid3D(skySize, skySize, 100, 100);
grid.setMajorEvery(10); // thicker line every 10 cells
grid.setLineColor(Color.color(0.5, 0.5, 1, 0.15));       // subtle
grid.setMajorLineColor(Color.color(1, 1, 1, 0.6));   // a bit stronger
grid.setMeshStyle(Grid3D.Style.CHECKERBOARD);
// Optional: grid.setCheckA(Color.color(1,1,1,0.10));
grid.setCheckB(Color.color(1,1,1,0.05));    
grid.setCheckA(Color.LIGHTSLATEGREY.deriveColor(1, 1, 1, 0.1));
worldRoot.getChildren().add(grid);

        // --- Physics system (120 Hz) ---
        this.physics = new PhysicsSystem(120);
        this.physics.setEnabled(true); // running by default

        // --- Craft rig (camera becomes the "ship") ---
        this.craft = CameraKinematicAdapter.attach(subScene, worldRoot);
        craft.setMass(1.5);
        craft.setLinearDampingPerSecond(0.18);
        craft.setMaxSpeed(650);

        // --- Collidables supplier (empty for now; replace when we spawn asteroids) ---
        Supplier<List<Node>> collidables = () -> Collections.emptyList();

        // --- Thrusters (physics contributor) ---
        this.thrusters = new ThrusterController(subScene, camera, craft);
        thrusters.setEnabled(true);
        thrusters.setThrustPower(480);
        thrusters.setVerticalPower(360);
        thrusters.setBrakePower(1400);
        thrusters.setDampenerPower(220);
        thrusters.setLookSensitivity(0.0); // mouse-look handled by fpsLook

        // --- Tethers (physics contributor + input handler gated externally) ---
        this.tethers = new TetherController(subScene, camera, worldRoot, collidables, craft);
        tethers.setSymmetricWingOffsets(20, 50, 5);
        tethers.setTetherInputEnabled(false); // toggle from UI when ready
        for (int i = 0; i < 2; i++) {
            var t = tethers.getTether(i);
            if (t != null) {
                t.setRayFrontFaceOnly(true);
                t.setAllowAabbFallbackOnMeshMiss(false);
                t.setStiffness(160);
                t.setDampingRatio(0.9);
                t.setMaxForce(900);
                t.setSlackEps(0.02);
                t.setReelRate(240);
                t.setShowEndMarker(true);
                t.setDebugPersistOnMiss(true);
            }
        }

        // --- Spacecraft collisions (physics contributor) ---
        this.shipCollisions = new SpacecraftCollisionContributor(worldRoot, craft, collidables, 1.5);
        shipCollisions.setFrontFaceOnly(true);
        shipCollisions.setMaxIterations(2);
        shipCollisions.setRestitution(0.05);
        shipCollisions.setFriction(0.15);

        // --- Register contributors with physics ---
        physics.addContributor(thrusters);
        physics.addContributor(tethers);
        physics.addContributor(shipCollisions);

        // --- FPS mouse-look (NEW) ---
        this.fpsLook = new FpsLookController(subScene, camera);
        fpsLook.setEnabled(true);
        fpsLook.setSensitivity(0.20);
        fpsLook.setSmoothing(0.35);
        fpsLook.setPitchLimits(-85, 85);
        fpsLook.setYawPitch(0, 0);
    }

    // ---------- Physics helpers ----------
    public PhysicsSystem getPhysics() { return physics; }
    public void pausePhysics()  { physics.setEnabled(false); }
    public void resumePhysics() { physics.setEnabled(true); }

    // ---------- Craft access ----------
    public CameraKinematicAdapter getCraft() { return craft; }

    // ---------- Feature access ----------
    public ThrusterController getThrusters() { return thrusters; }
    public TetherController getTethers() { return tethers; }
    public SpacecraftCollisionContributor getShipCollisions() { return shipCollisions; }

    // ---------- View control helpers ----------
    public FpsLookController getFpsLook() { return fpsLook; }
    public void setFpsLookEnabled(boolean on) {
        fpsLook.setEnabled(on);
        if (on) fpsLook.capturePointer(); else fpsLook.releasePointer();
    }
    public void capturePointer() { fpsLook.capturePointer(); }
    public void releasePointer() { fpsLook.releasePointer(); }

    // ---------- Craft proxy helpers ----------
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

    // ---------- Accessors ----------
    public SubScene getSubScene() { return subScene; }
    public Group getWorldRoot() { return worldRoot; }
    public PerspectiveCamera getCamera() { return camera; }
}
