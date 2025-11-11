package AsteroidField;

import AsteroidField.physics.PhysicsSystem;
import AsteroidField.runtime.CollidableRegistry;
import AsteroidField.spacecraft.CameraKinematicAdapter;
import AsteroidField.spacecraft.collision.SpacecraftCollisionContributor;
import AsteroidField.spacecraft.control.ThrusterController;
import AsteroidField.tether.TetherController;
import AsteroidField.ui.scene3d.CubeAtlas;
import AsteroidField.ui.scene3d.Grid3D;
import AsteroidField.ui.scene3d.Skybox;
import AsteroidField.ui.scene3d.SphericalSkybox;
import AsteroidField.util.FpsLookController;
import AsteroidField.util.ResourceUtils;
import java.io.IOException;
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
    private final FpsLookController fpsLook; 

    // Optional craft proxy we can show/hide while docking
    private Node craftProxy;
    
    // supplier for tethers & collisions:
    private CollidableRegistry collidablesRegistry;
    // supplier for tethers & collisions:
    Supplier<List<Node>> collidableSupplier;    

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

        // --- Skybox ---
        double skySize = 10000D;
        try {
//            Image atlas = ResourceUtils.load3DTextureImage("planar-skybox");
            Image atlas = ResourceUtils.load3DTextureImage("stars_atlas-4k");
            // Slice -> six faces
            CubeAtlas.Faces f = CubeAtlas.slice(atlas);
            
            // Build skybox via MULTIPLE path (everything else stays the same)
            Skybox sky = new Skybox(
                f.top(), f.bottom(), f.left(), f.right(), f.front(), f.back(), skySize, camera
            );
            worldRoot.getChildren().add(0, sky);
            
//// inside your 3D setup code:
//Image spaceEQ = ResourceUtils.load3DTextureImage("tycho-skymap"); // 2:1 texture
//double radius = camera.getFarClip() * 0.45; // keep comfortably within far plane
//SphericalSkybox skysphere = new SphericalSkybox(spaceEQ, radius, camera);
//worldRoot.getChildren().add(0, skysphere);            

        } catch (IOException ex) {
            System.getLogger(Game3DView.class.getName())
                  .log(System.Logger.Level.ERROR, (String) null, ex);
        }
        
        Grid3D grid = new Grid3D(skySize, skySize, 100, 100);
        grid.setMajorEvery(10); // thicker line every 10 cells
        grid.setLineColor(Color.color(0.5, 0.5, 1, 0.15));       // subtle
        grid.setMajorLineColor(Color.color(1, 1, 1, 0.6));   // a bit stronger
        //grid.setMeshStyle(Grid3D.Style.CHECKERBOARD);
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

        // --- Collidables supplier initialization ---
//        Supplier<List<Node>> collidables = () -> Collections.emptyList();
        collidablesRegistry = new CollidableRegistry();
        collidableSupplier = () -> collidablesRegistry.getCollidables();    

        // --- Thrusters (physics contributor) ---
        this.thrusters = new ThrusterController(subScene, camera, craft);
        thrusters.setEnabled(true);
        thrusters.setThrustPower(480);
        thrusters.setVerticalPower(360);
        thrusters.setBrakePower(1400);
        thrusters.setDampenerPower(220);
        thrusters.setLookSensitivity(0.0); // mouse-look handled by fpsLook

        // --- Tethers (physics contributor + input handler gated externally) ---
        tethers = new TetherController(subScene, camera, worldRoot, collidableSupplier, craft);
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
        shipCollisions = new SpacecraftCollisionContributor(worldRoot, craft, collidableSupplier, 1.5);
        shipCollisions.setFrontFaceOnly(true);
        shipCollisions.setMaxIterations(2);
        shipCollisions.setRestitution(0.05);
        shipCollisions.setFriction(0.15);

        // --- Register contributors with physics ---
        //Order matters! spacecraft should have all forces accumulated before it integrates
        physics.addContributor(thrusters);
        physics.addContributor(tethers);
        physics.addContributor(shipCollisions);
        physics.addContributor(craft);

        // --- FPS mouse-look ---
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
    public void addCollidable(Node n) {
        if (n != null) {
            worldRoot.getChildren().add(n);
            collidablesRegistry.add(n);
        }
    }
    public void removeCollidable(Node n) {
        if (n != null) {
            worldRoot.getChildren().remove(n);
            collidablesRegistry.remove(n);
        }
    }

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
