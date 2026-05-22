package fr.enac.drone;

import fr.enac.drone.controller.Autopilot;
import fr.enac.drone.controller.DroneController;
import fr.enac.drone.model.PathPlanner;
import fr.enac.drone.model.drone.DroneModel;
import fr.enac.drone.model.drone.DroneSpawn;
import fr.enac.drone.model.world.WorldCollisionDetector;
import fr.enac.drone.model.world.WorldConfiguration;
import fr.enac.drone.model.world.WorldPersistence;
import fr.enac.drone.utils.Vector3D;
import fr.enac.drone.view.SimulationView;
import fr.enac.drone.view.WorldConfigMenu;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Main application class for the FPV Drone Simulator.
 * Handles application lifecycle, world loading, UI setup, and game loop.
 */
public class App extends Application {

    /** Default world configuration filename */
    private static final String DEFAULT_WORLD_FILENAME = "world_default";

    /** Currently loaded world filename */
    private String currentWorldFilename = DEFAULT_WORLD_FILENAME;

    /** Main application scene */
    private Scene scene;
    
    /** Root layout pane */
    private BorderPane root;
    
    /** Application menu bar */
    private MenuBar menuBar;

    /** Current world configuration */
    private WorldConfiguration worldConfig;
    
    /** Main game loop timer */
    private AnimationTimer gameLoop;

    /** Drone model (physics state) */
    private DroneModel model;
    
    /** Drone controller (input handling) */
    private DroneController controller;
    
    /** Main simulation view (3D rendering) */
    private SimulationView view;

    /**
     * JavaFX application entry point.
     * Initializes the UI and starts the simulation.
     * 
     * @param primaryStage the primary stage for this application
     */
    @Override
    public void start(Stage primaryStage) {

        loadOrCreateDefaultWorld();
        createMenuBar();
        root = new BorderPane();
        root.setTop(menuBar);
        scene = new Scene(root, 800, 600);

        primaryStage.setTitle("FPV Drone Simulator");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        primaryStage.show();

        initializeSimulation();
    }

    /**
     * Loads the default world configuration from disk.
     * Creates a new default world if loading fails.
     */
    private void loadOrCreateDefaultWorld() {
        try {
            worldConfig = WorldPersistence.loadWorldConfiguration(DEFAULT_WORLD_FILENAME);
            if (worldConfig != null) return;
        } catch (Exception e) {
            System.err.println("Error loading default world: " + e.getMessage());
        }

        try {
            worldConfig = new WorldConfiguration("Default World");
            WorldPersistence.saveWorldConfiguration(worldConfig, DEFAULT_WORLD_FILENAME);
        } catch (Exception ex) {
            System.err.println("Failed to create/save default world: " + ex.getMessage());
            worldConfig = new WorldConfiguration("Default World");
        }
    }

    /**
     * Initializes the simulation: creates MVC components,
     * sets up event handlers, and starts the game loop.
     */
    private void initializeSimulation() {

        stopGameLoop();
        createMvcComponents();
        setupEventHandlers();

        PathPlanner pathPlanner = new PathPlanner(worldConfig);

        // Clear minimap target when autopilot finishes
        controller.setOnAutopilotFinished(() -> view.clearMinimapTarget());

        // Handle minimap click: plan path and start autopilot
        view.setMinimapTargetHandler(worldPos -> {
            double startX = model.getX();
            double startZ = model.getZ();

            Vector3D start = new Vector3D(startX, 0, startZ);
            Vector3D target = new Vector3D(worldPos[0], 0, worldPos[1]);

            List<Vector3D> waypoints = pathPlanner.findPath(start, target);

            if (!waypoints.isEmpty()) {
                List<double[]> waypointsArray = new ArrayList<>();
                for (Vector3D wp : waypoints) {
                    waypointsArray.add(new double[]{wp.x, wp.z});
                }
                Autopilot autopilot = new Autopilot(waypointsArray);
                controller.setAutopilot(autopilot);
            } else {
                view.clearMinimapTarget();
                System.out.println("No path found to target!");
            }
        });

        // Return to Home action (activated by pressing H key)
        controller.setReturnHomeAction(() -> {
            List<double[]> trail = view.getTrail();
            if (trail.size() < 2) return;

            List<double[]> reversed = new ArrayList<>(trail);
            Collections.reverse(reversed);

            Autopilot autopilot = new Autopilot(reversed);
            controller.setAutopilot(autopilot);
            view.clearMinimapTarget();
        });

        startGameLoop();
    }

    /**
     * Stops the game loop if it is currently running.
     */
    private void stopGameLoop() {
        if (gameLoop != null) gameLoop.stop();
    }

    /**
     * Creates the Model-View-Controller components.
     * Also sets up collision detection and spawn position.
     */
    private void createMvcComponents() {

        model = new DroneModel();
        
        // Create collision detector for obstacle detection
        WorldCollisionDetector collisionDetector = new WorldCollisionDetector(worldConfig);
        controller = new DroneController(model, collisionDetector);
        view = new SimulationView(model, worldConfig);

        // Set drone spawn position from world configuration
        DroneSpawn spawn = worldConfig.getDroneSpawn();
        model.setSpawnPosition(
                spawn.getPosX(),
                spawn.getPosY(),
                spawn.getPosZ(),
                spawn.getYaw()
        );

        root.setCenter(view.getRoot());
    }

    /**
     * Sets up keyboard event handlers for drone control.
     */
    private void setupEventHandlers() {

        Objects.requireNonNull(view).getRoot().requestFocus();

        scene.setOnKeyPressed(e -> controller.addKey(e.getCode()));
        scene.setOnKeyReleased(e -> controller.removeKey(e.getCode()));
    }

    /**
     * Starts the main game loop using JavaFX AnimationTimer.
     * Updates physics and rendering at ~60 FPS.
     */
    private void startGameLoop() {

        gameLoop = new AnimationTimer() {

            private long lastUpdate = 0;

            @Override
            public void handle(long now) {

                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }

                double deltaTime = (now - lastUpdate) / 1_000_000_000.0;
                lastUpdate = now;

                controller.update(deltaTime);
                view.render();
            }
        };

        gameLoop.start();
    }

    /**
     * Creates the application menu bar.
     */
    private void createMenuBar() {

        menuBar = new MenuBar();

        Menu configMenu = new Menu("Configuration");

        MenuItem worldConfigItem = new MenuItem("World Configuration");
        worldConfigItem.setOnAction(e -> handleWorldConfigurationMenu());

        configMenu.getItems().add(worldConfigItem);
        menuBar.getMenus().add(configMenu);
    }

    /**
     * Opens the world configuration dialog.
     */
    private void handleWorldConfigurationMenu() {
        WorldConfigMenu dialog = new WorldConfigMenu(
                worldConfig,
                currentWorldFilename,
                this::handleWorldLoaded
        );
        dialog.showAndWait();
    }

    /**
     * Callback for when a new world configuration is loaded.
     * Reinitializes the simulation with the new world.
     * 
     * @param config the new world configuration
     * @param filename the filename the world was loaded from
     */
    private void handleWorldLoaded(WorldConfiguration config, String filename) {
        this.worldConfig = Objects.requireNonNull(config);
        this.currentWorldFilename = Objects.requireNonNull(filename);
        initializeSimulation();
    }

    /**
     * Application entry point.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}