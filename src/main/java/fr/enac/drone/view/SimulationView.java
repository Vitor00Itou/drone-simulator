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

import java.util.Objects;
import java.util.List;
import java.util.function.Consumer;

/**
 * Handles the 3D rendering and FPV camera logic for the drone simulator.
 * The SubScene is bound to fill the entire window.
 */
public class SimulationView {

    private final BorderPane root;

    private final DroneModel model;

    private final Box droneVisual;

    private final PerspectiveCamera camera;

    private final FpvHudView hudView;

    private final WorldConfiguration worldConfig;

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

        // ── 3D scene root ──────────────────────────────────────────────
        Group sceneRoot = new Group();

        // Drone visual
        droneVisual = new Box(50, 50, 50);

        droneVisual.setRotationAxis(Rotate.Y_AXIS);

        PhongMaterial droneMat = new PhongMaterial();

        droneMat.setDiffuseColor(Color.RED);

        droneVisual.setMaterial(droneMat);

        sceneRoot.getChildren().add(droneVisual);

        // ── Load world objects ─────────────────────────────────────────
        for (WorldObject obj : worldConfig.getObjects()) {

            Node visualObject = createVisualObject(obj);

            if (visualObject != null) {
                sceneRoot.getChildren().add(visualObject);
            }
        }

        // Lighting
        sceneRoot.getChildren().add(
                new AmbientLight(Color.WHITE)
        );

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

        hudView = new FpvHudView();

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

        PhongMaterial material = new PhongMaterial();

        material.setDiffuseColor(
                Color.web(obj.getColor())
        );

        Node node = null;

        switch (obj.getType().toLowerCase()) {

            case "box":

                Box box = new Box(
                        obj.getSizeX(),
                        obj.getSizeY(),
                        obj.getSizeZ()
                );

                box.setMaterial(material);

                node = box;

                break;

            case "cylinder":

                Cylinder cylinder = new Cylinder(
                        obj.getSizeX(),
                        obj.getSizeY()
                );

                cylinder.setMaterial(material);

                node = cylinder;

                break;

            case "plane":

                Box plane = new Box(
                        obj.getSizeX(),
                        obj.getSizeY(),
                        obj.getSizeZ()
                );

                plane.setMaterial(material);

                node = plane;

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