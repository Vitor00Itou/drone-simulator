package fr.enac.drone.view;

import fr.enac.drone.model.drone.DroneModel;
import fr.enac.drone.model.world.WorldConfiguration;
import fr.enac.drone.model.world.WorldObject;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import javafx.scene.PointLight;

import java.util.Objects;
import java.util.List;
import java.util.function.Consumer;

/**
 * Handles the 3D rendering and FPV camera logic for the drone simulator.
 * The SubScene is bound to fill the entire window.
 */
public class SimulationView {
    private static final double GROUND_TEXTURE_TILE_SIZE = 500.0;

    private final BorderPane root;

    private final DroneModel model;

    private final Box droneVisual;

    private final PerspectiveCamera camera;

    private final FpvHudView hudView;

    private final WorldConfiguration worldConfig;

    private final SceneMaterialFactory materialFactory;

    public SimulationView(
            DroneModel model,
            WorldConfiguration worldConfig
    ) {

        this.model =
                Objects.requireNonNull(
                        model,
                        "Model cannot be null"
                );

        this.worldConfig =
                Objects.requireNonNull(
                        worldConfig,
                        "World configuration cannot be null"
                );

        this.root = new BorderPane();
        this.materialFactory = new SceneMaterialFactory();

        // ── 3D scene root ──────────────────────────────────────────────
        Group sceneRoot = new Group();

        // Drone visual
        droneVisual = new Box(50, 50, 50);

        droneVisual.setRotationAxis(Rotate.Y_AXIS);
        droneVisual.setMaterial(materialFactory.createDroneMaterial());
        sceneRoot.getChildren().add(droneVisual);

        // ── Load world objects ─────────────────────────────────────────
        for (WorldObject obj : worldConfig.getObjects()) {

            Node visualObject = createVisualObject(obj);

            if (visualObject != null) {
                sceneRoot.getChildren().add(visualObject);
            }
        }

        // Soft ambient light to avoid completely dark areas
        AmbientLight ambientLight = new AmbientLight(Color.rgb(110, 110, 110));
        sceneRoot.getChildren().add(ambientLight);

        // Main light source simulating sunlight
        PointLight mainLight = new PointLight(Color.WHITE);
        mainLight.setTranslateX(-1200);
        mainLight.setTranslateY(-1800);
        mainLight.setTranslateZ(-1000);
        sceneRoot.getChildren().add(mainLight);

        // Secondary softer light to illuminate the opposite side
        PointLight fillLight = new PointLight(Color.rgb(170, 170, 170));
        fillLight.setTranslateX(1200);
        fillLight.setTranslateY(-700);
        fillLight.setTranslateZ(1000);
        sceneRoot.getChildren().add(fillLight);

        // ── SubScene ───────────────────────────────────────────────────
        SubScene subScene = new SubScene(
                sceneRoot,
                800,
                600,
                true,
                SceneAntialiasing.BALANCED
        );

        subScene.setFill(
                Color.web(
                        worldConfig.getEnvironment().getSkyColor()
                )
        );

        // Camera
        camera = new PerspectiveCamera(true);

        camera.setRotationAxis(Rotate.Y_AXIS);

        camera.setNearClip(0.1);

        camera.setFarClip(5000.0);

        subScene.setCamera(camera);

        // ── Responsive viewport ───────────────────────────────────────
        Pane sizingPane = new Pane();

        subScene.widthProperty().bind(
                sizingPane.widthProperty()
        );

        subScene.heightProperty().bind(
                sizingPane.heightProperty()
        );

        StackPane viewport =
                new StackPane(sizingPane, subScene);

        hudView = new FpvHudView(model);

        viewport.getChildren().add(hudView);

        // Root
        root.setCenter(viewport);

        root.setMaxSize(
                Double.MAX_VALUE,
                Double.MAX_VALUE
        );

        render();
    }

    /**
     * Creates visual objects from world configuration.
     */
    private Node createVisualObject(WorldObject obj) {
        Node node = null;

        switch (obj.getType().toLowerCase()) {

            case "box":
                Box box = new Box(obj.getSizeX(), obj.getSizeY(), obj.getSizeZ());
                box.setMaterial(materialFactory.getMaterial(obj));
                node = box;

                break;

            case "cylinder":
                Cylinder cylinder = new Cylinder(obj.getSizeX(), obj.getSizeY());
                cylinder.setMaterial(materialFactory.getMaterial(obj));
                node = cylinder;

                break;

            case "plane":
                node = createGroundPlane(obj);
                break;

            default:

                System.err.println(
                        "Unknown object type: "
                                + obj.getType()
                );

                return null;
        }

        node.setTranslateX(obj.getPosX());

        node.setTranslateY(obj.getPosY());

        node.setTranslateZ(obj.getPosZ());

        return node;
    }

    private Group createGroundPlane(WorldObject obj) {
        Group ground = new Group();
        PhongMaterial groundMaterial = materialFactory.getMaterial(obj);
        int tilesX = Math.max(1, (int) Math.ceil(obj.getSizeX() / GROUND_TEXTURE_TILE_SIZE));
        int tilesZ = Math.max(1, (int) Math.ceil(obj.getSizeZ() / GROUND_TEXTURE_TILE_SIZE));
        double startX = -obj.getSizeX() / 2.0;
        double startZ = -obj.getSizeZ() / 2.0;

        for (int x = 0; x < tilesX; x++) {
            double tileWidth = Math.min(GROUND_TEXTURE_TILE_SIZE, obj.getSizeX() - x * GROUND_TEXTURE_TILE_SIZE);
            double tileCenterX = startX + x * GROUND_TEXTURE_TILE_SIZE + tileWidth / 2.0;

            for (int z = 0; z < tilesZ; z++) {
                double tileDepth = Math.min(GROUND_TEXTURE_TILE_SIZE, obj.getSizeZ() - z * GROUND_TEXTURE_TILE_SIZE);
                double tileCenterZ = startZ + z * GROUND_TEXTURE_TILE_SIZE + tileDepth / 2.0;

                Box tile = new Box(tileWidth, obj.getSizeY(), tileDepth);
                tile.setMaterial(groundMaterial);
                tile.setTranslateX(tileCenterX);
                tile.setTranslateZ(tileCenterZ);
                ground.getChildren().add(tile);
            }
        }

        return ground;
    }

    public BorderPane getRoot() {
        return root;
    }

    /**
     * Synchronizes visuals and camera with the model state.
     */
    public void render() {

        droneVisual.setTranslateX(model.getX());

        droneVisual.setTranslateY(model.getY());

        droneVisual.setTranslateZ(model.getZ());

        droneVisual.setRotate(model.getYaw());

        camera.setTranslateX(model.getX());

        camera.setTranslateY(model.getY());

        camera.setTranslateZ(model.getZ());

        camera.setRotate(model.getYaw());

        hudView.update(model, worldConfig);
    }

    /**
     * Clears the current navigation target cross from the minimap.
     */
    public void clearMinimapTarget() {
        hudView.clearMinimapTarget();
    }

    /**
     * Returns a copy of the recorded flight trail points.
     * Each element is a double[] {x, z} in world coordinates.
     */
    public List<double[]> getTrail() {
        return hudView.getTrail();
    }

    /**
     * Registers a callback to be invoked when the user clicks on the minimap.
     * The callback receives the world {x, z} coordinates of the clicked point.
     */
    public void setMinimapTargetHandler(Consumer<double[]> handler) {
        hudView.setOnMinimapTargetClicked(handler);
    }
}
