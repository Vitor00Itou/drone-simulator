package fr.enac.drone.view;

import fr.enac.drone.input.InputDevice;
import fr.enac.drone.input.KeyboardLayout;
import fr.enac.drone.model.SimulationState;
import fr.enac.drone.model.WorldPosition2D;
import fr.enac.drone.model.drone.DroneModel;
import fr.enac.drone.model.world.WorldConfiguration;
import fr.enac.drone.model.world.WorldObject;
import fr.enac.drone.view.settings.SettingsState;
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
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Handles the 3D rendering and FPV camera logic for the drone simulator.
 * The SubScene is bound to fill the entire window.
 */
public class SimulationView {
    private static final double GROUND_TEXTURE_TILE_SIZE = 500.0;

    private final BorderPane root;

    private final DroneModel model;

    private final Cylinder droneVisual;

    private final PerspectiveCamera camera;

    private final FpvHudView hudView;

    private final SettingsView settingsView;

    private final WorldConfiguration worldConfig;

    private final SceneMaterialFactory materialFactory;

    private SimulationState simulationState = SimulationState.READY;

    /**
     * Creates the main simulation view, including 3D scene, HUD, and settings drawer.
     *
     * @param model drone model to render
     * @param worldConfig world configuration to render
     * @param currentWorldFilename active world filename without extension
     * @param onWorldApplied callback invoked when settings apply a world change
     * @param settingsState persisted settings drawer state
     */
    public SimulationView(
            DroneModel model,
            WorldConfiguration worldConfig,
            String currentWorldFilename,
            BiConsumer<WorldConfiguration, String> onWorldApplied,
            SettingsState settingsState
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
        this.root.setFocusTraversable(true);
        this.materialFactory = new SceneMaterialFactory();

        // ── 3D scene root ──────────────────────────────────────────────
        Group sceneRoot = new Group();

        // Drone visual
        droneVisual = new Cylinder(model.getDroneRadius(), model.getDroneHeight());

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

        subScene.setFill(parseColor(worldConfig.getEnvironment().getSkyColor(), Color.rgb(135, 206, 235)));

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

        settingsView = new SettingsView(
                model,
                worldConfig,
                currentWorldFilename,
                onWorldApplied,
                settingsState
        );
        viewport.getChildren().add(settingsView);

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
     *
     * @param obj world object to convert into a JavaFX node
     * @return visual node, or {@code null} when the type is unsupported
     */
    private Node createVisualObject(WorldObject obj) {
        Node node = null;

        switch (obj.getType()) {

            case BOX:
                Box box = new Box(obj.getSizeX(), obj.getSizeY(), obj.getSizeZ());
                box.setMaterial(materialFactory.getMaterial(obj));
                node = box;

                break;

            case CYLINDER:
                Cylinder cylinder = new Cylinder(obj.getRadius(), obj.getSizeY());
                cylinder.setMaterial(materialFactory.getMaterial(obj));
                node = cylinder;

                break;

            case PLANE:
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

    /**
     * Creates a tiled ground plane so large textured grounds repeat predictably.
     *
     * @param obj plane object to render
     * @return group containing ground tiles
     */
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

    /**
     * Parses a JavaFX scene color with a fallback.
     *
     * @param colorValue RGB hex value or JavaFX color name
     * @param fallbackColor fallback color used when parsing fails
     * @return parsed color or fallback color
     */
    private Color parseColor(String colorValue, Color fallbackColor) {
        try {
            return Color.web(colorValue);
        } catch (IllegalArgumentException | NullPointerException e) {
            System.err.println("Invalid scene color '" + colorValue + "'. Using fallback color.");
            return fallbackColor;
        }
    }

    /**
     * Returns the root node for insertion into the JavaFX scene.
     *
     * @return root border pane
     */
    public BorderPane getRoot() {
        return root;
    }

    /**
     * Sets the simulation lifecycle state displayed by the HUD.
     *
     * @param simulationState simulation lifecycle state
     */
    public void setSimulationState(SimulationState simulationState) {
        this.simulationState =
                Objects.requireNonNull(
                        simulationState,
                        "Simulation state cannot be null"
                );
        hudView.updateSimulationState(simulationState);
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

        hudView.update(model, worldConfig, simulationState);
    }

    /**
     * Clears the current navigation target cross from the minimap.
     */
    public void clearMinimapTarget() {
        hudView.clearMinimapTarget();
    }
    
    /**
     * Adjusts the minimap zoom radius.
     *
     * @param delta zoom radius delta in meters
     */
    public void adjustMinimapZoom(double delta) {
        hudView.adjustMinimapZoom(delta);
    }
    
    /**
     * Returns the current minimap zoom radius.
     *
     * @return minimap zoom radius in meters
     */
    public double getMinimapZoom() {
        return hudView.getMinimapZoom();
    }
    
    /**
     * Sets the minimap zoom radius.
     *
     * @param zoom zoom radius in meters
     */
    public void setMinimapZoom(double zoom) {
        hudView.setMinimapZoom(zoom);
    }
    
    /**
     * Shows or hides the command help panel.
     *
     * @param visible {@code true} to show command help
     */
    public void setCommandHelpVisible(boolean visible) {
        hudView.setCommandHelpVisible(visible);
    }
    
    /**
     * Refreshes command help labels for the active input configuration.
     *
     * @param device active input device
     * @param layout active keyboard layout
     */
    public void updateCommandHelp(InputDevice device, KeyboardLayout layout) {
        hudView.updateCommandHelp(device, layout);
    }

    /**
     * Returns a copy of the recorded flight trail points in world coordinates.
     *
     * @return flight trail points
     */
    public List<WorldPosition2D> getTrail() {
        return hudView.getTrail();
    }

    /**
     * Clears the flight trail history from the HUD.
     */
    public void clearTrail() {
        hudView.clearTrail();
    }

    /**
     * Registers a callback to be invoked when the user clicks on the minimap.
     * The callback receives the world {x, z} coordinates of the clicked point.
     *
     * @param handler minimap target callback
     */
    public void setMinimapTargetHandler(Consumer<WorldPosition2D> handler) {
        hudView.setOnMinimapTargetClicked(handler);
    }

    /**
     * Returns whether the settings drawer is open or visible during transition.
     *
     * @return {@code true} when the settings drawer is open
     */
    public boolean isSettingsDrawerOpen() {
        return settingsView.isOpen();
    }

    /**
     * Closes the settings drawer.
     */
    public void closeSettingsDrawer() {
        settingsView.close();
    }

    /**
     * Registers a callback invoked after the settings drawer opens.
     *
     * @param handler callback to run on open
     */
    public void setSettingsDrawerOpenedHandler(Runnable handler) {
        settingsView.setOnOpened(handler);
    }

    /**
     * Registers a callback invoked after the settings drawer closes.
     *
     * @param handler callback to run on close
     */
    public void setSettingsDrawerClosedHandler(Runnable handler) {
        settingsView.setOnClosed(handler);
    }
    
    /**
     * Registers a callback for command help visibility changes.
     *
     * @param handler receives the new visibility flag
     */
    public void setCommandHelpVisibilityHandler(Consumer<Boolean> handler) {
        settingsView.setOnCommandHelpVisibilityChanged(handler);
    }
    
    /**
     * Registers a callback for keyboard layout changes.
     *
     * @param handler receives the selected keyboard layout
     */
    public void setKeyboardLayoutHandler(Consumer<KeyboardLayout> handler) {
        settingsView.setOnKeyboardLayoutChanged(handler);
    }
}
