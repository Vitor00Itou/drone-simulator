package fr.enac.drone.view;

import fr.enac.drone.model.drone.DroneModel;
import fr.enac.drone.model.world.WorldConfiguration;
import fr.enac.drone.model.world.WorldObject;
import javafx.scene.Group;
import javafx.scene.Node;
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

import java.util.Objects;
import java.util.function.Consumer;

public class SimulationView {
    private BorderPane root;
    private DroneModel model;
    private Box droneVisual;
    private PerspectiveCamera camera;
    private FpvHudView hudView;
    private WorldConfiguration worldConfig;

    public SimulationView(DroneModel model, WorldConfiguration worldConfig) {
        this.model = Objects.requireNonNull(model);
        this.worldConfig = Objects.requireNonNull(worldConfig);
        this.root = new BorderPane();
        init3DView();
        render();
    }

    private void init3DView() {
        Group sceneRoot = new Group();

        droneVisual = new Box(50, 50, 50);
        droneVisual.setRotationAxis(Rotate.Y_AXIS);
        PhongMaterial droneMat = new PhongMaterial();
        droneMat.setDiffuseColor(Color.RED);
        droneVisual.setMaterial(droneMat);
        sceneRoot.getChildren().add(droneVisual);

        for (WorldObject obj : worldConfig.getObjects()) {
            Node visualObject = createVisualObject(obj);
            if (visualObject != null) sceneRoot.getChildren().add(visualObject);
        }

        AmbientLight light = new AmbientLight(Color.WHITE);
        sceneRoot.getChildren().add(light);

        StackPane viewport = new StackPane();
        SubScene subScene = new SubScene(sceneRoot, 800, 600, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.web(worldConfig.getEnvironment().getSkyColor()));
        subScene.widthProperty().bind(viewport.widthProperty());
        subScene.heightProperty().bind(viewport.heightProperty());

        camera = new PerspectiveCamera(true);
        camera.setRotationAxis(Rotate.Y_AXIS);
        camera.setNearClip(0.1);
        camera.setFarClip(5000.0);
        subScene.setCamera(camera);

        hudView = new FpvHudView();
        viewport.getChildren().addAll(subScene, hudView);
        root.setCenter(viewport);
    }

    private Node createVisualObject(WorldObject obj) {
        PhongMaterial mat = new PhongMaterial();
        mat.setDiffuseColor(Color.web(obj.getColor()));
        Node node = null;
        switch (obj.getType().toLowerCase()) {
            case "box":
                Box box = new Box(obj.getSizeX(), obj.getSizeY(), obj.getSizeZ());
                box.setMaterial(mat);
                node = box;
                break;
            case "cylinder":
                Cylinder cyl = new Cylinder(obj.getSizeX(), obj.getSizeY());
                cyl.setMaterial(mat);
                node = cyl;
                break;
            case "plane":
                Box plane = new Box(obj.getSizeX(), obj.getSizeY(), obj.getSizeZ());
                plane.setMaterial(mat);
                node = plane;
                break;
            default:
                System.err.println("Unknown object type: " + obj.getType());
                return null;
        }
        node.setTranslateX(obj.getPosX());
        node.setTranslateY(obj.getPosY());
        node.setTranslateZ(obj.getPosZ());
        return node;
    }
    public void clearMinimapTarget() {
        hudView.clearMinimapTarget();
    }

    public BorderPane getRoot() { return root; }

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

    // Méthode ajoutée pour connecter le callback du minimap
    public void setMinimapTargetHandler(Consumer<double[]> handler) {
        hudView.setOnMinimapTargetClicked(handler);
    }
}