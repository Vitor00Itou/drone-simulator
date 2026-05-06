package fr.enac.drone.view;

import fr.enac.drone.model.DroneModel;
import fr.enac.drone.model.Obstacle;
import fr.enac.drone.model.World;
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
import javafx.scene.control.Button;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

/**
 * Handles the 3D rendering and FPV camera logic for the drone simulator.
 */
public class SimulationView {
    private BorderPane root;
    private DroneModel model;
    private World worldModel;
    private Box droneVisual;
    private PerspectiveCamera camera;
    private FpvHudView hudView;
    private Group sceneRoot;
    private Button settingsButton;

    public SimulationView(DroneModel model, World worldModel) {
        this.model = model;
        this.worldModel = worldModel;
        this.root = new BorderPane();
        init3DView();
        render();
    }

    /**
     * Initializes the 3D world, including the drone, environment, and lighting.
     */
    private void init3DView() {
        sceneRoot = new Group();
        
        // Drone visual representation
        droneVisual = new Box(50, 50, 50);
        droneVisual.setRotationAxis(Rotate.Y_AXIS);
        
        PhongMaterial droneMat = new PhongMaterial();
        droneMat.setDiffuseColor(Color.RED);
        droneVisual.setMaterial(droneMat);
        sceneRoot.getChildren().add(droneVisual);

        // Ground plane
        Box floor = new Box(5000, 1, 5000);
        PhongMaterial floorMat = new PhongMaterial();
        floorMat.setDiffuseColor(Color.DARKGREEN);
        floor.setMaterial(floorMat);
        floor.setTranslateY(30);
        sceneRoot.getChildren().add(floor);

        // Initializes and adds all obstacle visuals to the 3D scene
        createObstacles();

        // Lighting
        AmbientLight light = new AmbientLight(Color.WHITE);
        sceneRoot.getChildren().add(light);

        StackPane viewport = new StackPane();

        // 3D SubScene configuration
        SubScene subScene = new SubScene(sceneRoot, 800, 600, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.LIGHTSKYBLUE);
        subScene.widthProperty().bind(viewport.widthProperty());
        subScene.heightProperty().bind(viewport.heightProperty());

        // FPV Camera configuration
        camera = new PerspectiveCamera(true);
        camera.setRotationAxis(Rotate.Y_AXIS);
        camera.setNearClip(0.1);
        camera.setFarClip(5000.0);
        subScene.setCamera(camera);

        hudView = new FpvHudView();

        settingsButton = new Button("⚙");
        settingsButton.setStyle(
            "-fx-background-color: rgba(67, 66, 80, 0.9);" +
            "-fx-text-fill: white;" +
            "-fx-font-size: 20px;" +
            "-fx-font-weight: bold;" +
            "-fx-background-radius: 18;" +
            "-fx-padding: 6 12 6 12;"
        );

        StackPane.setAlignment(settingsButton, Pos.TOP_RIGHT);
        StackPane.setMargin(settingsButton, new Insets(24, 24, 0, 0));

        viewport.getChildren().addAll(subScene, hudView, settingsButton);
        root.setCenter(viewport);
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

    /**
    * Creates and renders obstacle objects in the 3D scene based on the model data.
    */
    private void createObstacles() {
        PhongMaterial obstacleMat = new PhongMaterial();
        obstacleMat.setDiffuseColor(Color.ORANGE);

        for (Obstacle obstacle : worldModel.getObstacles()) {
            Cylinder cylinder = new Cylinder(obstacle.getRadius(), obstacle.getHeight());
            cylinder.setMaterial(obstacleMat);

            cylinder.setTranslateX(obstacle.getX());
            cylinder.setTranslateY(obstacle.getY());
            cylinder.setTranslateZ(obstacle.getZ());

            sceneRoot.getChildren().add(cylinder);
        }
    }

    /**
    * Refreshes the world by removing existing obstacles and regenerating them.
    */
    public void refreshWorld() {
        sceneRoot.getChildren().removeIf(node -> node instanceof Cylinder);
        createObstacles();
    }

    public Button getSettingsButton() {
        return settingsButton;
    }
}
