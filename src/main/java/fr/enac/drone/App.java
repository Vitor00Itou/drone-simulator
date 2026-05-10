package fr.enac.drone;

import fr.enac.drone.controller.DroneController;
import fr.enac.drone.model.drone.DroneModel;
import fr.enac.drone.model.drone.DroneSpawn;
import fr.enac.drone.model.world.WorldConfiguration;
import fr.enac.drone.model.world.WorldPersistence;
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
import java.util.Objects;

/**
 * Main entry point for the Drone Simulator application at ENAC.
 * Initializes the MVC components and manages the Game Loop.
 */
public class App extends Application {
    private static final String DEFAULT_WORLD_FILENAME = "world_default";

    private String currentWorldFilename = DEFAULT_WORLD_FILENAME;
    private Scene scene;
    private BorderPane root;
    private MenuBar menuBar;
    private WorldConfiguration worldConfig;
    private AnimationTimer gameLoop;
    
    private DroneModel model;
    private DroneController controller;
    private SimulationView view;

    @Override
    public void start(Stage primaryStage) {
        // Create menu bar (persistent across restarts)
        createMenuBar();
        
        // Create root pane
        this.root = new BorderPane();
        root.setTop(menuBar);

        // Initialize or load the default world configuration
        loadOrCreateDefaultWorld();
        
        // Configure the main scene
        this.scene = new Scene(root, 800, 600);
        
        // Stage configuration
        primaryStage.setTitle("FPV Drone Simulator");
        primaryStage.setScene(scene);
        primaryStage.show();
        
        // Initialize the simulation
        initializeSimulation();
    }

    private void loadOrCreateDefaultWorld() {
        try {
            // Try to load existing default world
            worldConfig = WorldPersistence.loadWorldConfiguration(DEFAULT_WORLD_FILENAME);
            if (worldConfig != null) {
                return;
            }
        } catch (Exception e) {
            System.err.println("Error loading default world: " + e.getMessage());
        }

        // Create and save default world configuration
        try {
            worldConfig = new WorldConfiguration("Default World");
            WorldPersistence.saveWorldConfiguration(worldConfig, DEFAULT_WORLD_FILENAME);
        } catch (Exception ex) {
            System.err.println("Failed to create/save default world: " + ex.getMessage());
            // Fallback: Create in-memory default world
            worldConfig = new WorldConfiguration("Default World");
        }
    }
    
    /**
     * Initializes or reinitializes the simulation with current world configuration.
     */
    private void initializeSimulation() {
        stopGameLoop();
        createMvcComponents();
        setupEventHandlers();
        startGameLoop();
    }

    private void stopGameLoop() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
    }

    private void createMvcComponents() {
        // Create new MVC components
        model = new DroneModel();
        controller = new DroneController(model);
        view = new SimulationView(model, worldConfig);
        
        // Set drone spawn position from world configuration
        DroneSpawn spawn = worldConfig.getDroneSpawn();
        model.setSpawnPosition(
            spawn.getPosX(),
            spawn.getPosY(),
            spawn.getPosZ(),
            spawn.getYaw()
        );
        
        // Update the scene center
        root.setCenter(view.getRoot());
    }

    private void setupEventHandlers() {
        // Request focus
        Objects.requireNonNull(view, "View cannot be null").getRoot().requestFocus();
        
        // Register input handlers
        scene.setOnKeyPressed(event -> controller.addKey(event.getCode()));
        scene.setOnKeyReleased(event -> controller.removeKey(event.getCode()));
    }

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
     * Creates the menu bar with configuration options.
     */
    private void createMenuBar() {
        menuBar = new MenuBar();
        Menu configMenu = new Menu("Configuration");
        MenuItem worldConfigItem = new MenuItem("World Configuration");
        worldConfigItem.setOnAction(e -> handleWorldConfigurationMenu());
        configMenu.getItems().add(worldConfigItem);
        menuBar.getMenus().add(configMenu);
    }

    private void handleWorldConfigurationMenu() {
        WorldConfigMenu dialog = new WorldConfigMenu(worldConfig, currentWorldFilename, this::handleWorldLoaded);
        dialog.showAndWait();
    }

    private void handleWorldLoaded(WorldConfiguration config, String filename) {
        this.worldConfig = Objects.requireNonNull(config, "Loaded configuration cannot be null");
        this.currentWorldFilename = Objects.requireNonNull(filename, "Filename cannot be null");
        this.initializeSimulation();
    }

    public static void main(String[] args) {
        launch(args);
    }
}