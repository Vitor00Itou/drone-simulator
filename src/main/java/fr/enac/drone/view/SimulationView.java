package fr.enac.drone.view;

import fr.enac.drone.model.DroneModel;
import fr.enac.drone.model.world.WorldConfiguration;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.AmbientLight;
import javafx.scene.transform.Rotate;

/**
 * Handles the 3D rendering and FPV camera logic for the drone simulator.
 */
public class SimulationView {
    private BorderPane root;
    private DroneModel model;
    private Box droneVisual;
    private PerspectiveCamera camera;
    private FpvHudView hudView;
    private WorldConfiguration worldConfig;

    public SimulationView(DroneModel model, WorldConfiguration worldConfig) {
        this.model = model;
        this.worldConfig = worldConfig;
        this.root = new BorderPane();
        init3DView();
        render();
    }

    /**
     * Initializes the 3D world, including the drone, environment, and loading all objects from configuration.
     */
    private void init3DView() {
        Group sceneRoot = new Group();
        
        // Drone visual representation
        droneVisual = new Box(50, 50, 50);
        droneVisual.setRotationAxis(Rotate.Y_AXIS);
        
        PhongMaterial droneMat = new PhongMaterial();
        droneMat.setDiffuseColor(Color.RED);
        droneVisual.setMaterial(droneMat);
        sceneRoot.getChildren().add(droneVisual);

        // Load all objects from world configuration
        for (WorldConfiguration.WorldObject obj : worldConfig.objects) {
            javafx.scene.Node visualObject = createVisualObject(obj);
            if (visualObject != null) {
                sceneRoot.getChildren().add(visualObject);
            }
        }

        // Lighting
        AmbientLight light = new AmbientLight(Color.WHITE);
        sceneRoot.getChildren().add(light);

        StackPane viewport = new StackPane();

        // 3D SubScene configuration
        SubScene subScene = new SubScene(sceneRoot, 800, 600, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web(worldConfig.environment.skyColor));
        subScene.widthProperty().bind(viewport.widthProperty());
        subScene.heightProperty().bind(viewport.heightProperty());

        // FPV Camera configuration
        camera = new PerspectiveCamera(true);
        camera.setRotationAxis(Rotate.Y_AXIS);
        camera.setNearClip(0.1);
        camera.setFarClip(5000.0);
        subScene.setCamera(camera);

        hudView = new FpvHudView();
        viewport.getChildren().addAll(subScene, hudView);
        root.setCenter(viewport);
    }

    /**
     * Creates a visual 3D object from a WorldObject configuration.
     */
    private javafx.scene.Node createVisualObject(WorldConfiguration.WorldObject obj) {
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(Color.web(obj.color));

        javafx.scene.Node node = null;

        switch (obj.type.toLowerCase()) {
            case "box":
                Box box = new Box(obj.sizeX, obj.sizeY, obj.sizeZ);
                box.setMaterial(material);
                node = box;
                break;
            case "cylinder":
                Cylinder cylinder = new Cylinder(obj.sizeX, obj.sizeY);
                cylinder.setMaterial(material);
                node = cylinder;
                break;
            case "plane":
                // Ground plane - special handling
                Box plane = new Box(obj.sizeX, obj.sizeY, obj.sizeZ);
                plane.setMaterial(material);
                node = plane;
                break;
            default:
                System.err.println("Unknown object type: " + obj.type);
                return null;
        }

        if (node != null) {
            node.setTranslateX(obj.posX);
            node.setTranslateY(obj.posY);
            node.setTranslateZ(obj.posZ);
        }

        return node;
    }

    public BorderPane getRoot() {
        return root;
    }
    
    /**
     * Synchronizes the visual objects and camera with the model's state.
     */
    public void render() {
        // Synchronize drone position and yaw
        droneVisual.setTranslateX(model.getX());
        droneVisual.setTranslateY(model.getY());
        droneVisual.setTranslateZ(model.getZ());
        droneVisual.setRotate(model.getYaw());

        // Synchronize FPV camera with drone movement and orientation
        camera.setTranslateX(model.getX());
        camera.setTranslateY(model.getY());
        camera.setTranslateZ(model.getZ()); 
        camera.setRotate(model.getYaw());

        hudView.update(model.getTelemetry());
    }
}
