package fr.enac.drone;

import fr.enac.drone.controller.DroneController;
import fr.enac.drone.model.DroneModel;
import fr.enac.drone.model.world.WorldConfiguration;
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
            String[] saves = fr.enac.drone.model.world.WorldPersistence.listSaves();
            boolean defaultExists = false;
            for (String save : saves) {
                if (DEFAULT_WORLD_FILENAME.equals(save)) {
                    defaultExists = true;
                    break;
                }
            }

            if (defaultExists) {
                WorldConfiguration loaded = fr.enac.drone.model.world.WorldPersistence.loadWorldConfiguration(DEFAULT_WORLD_FILENAME);
                if (loaded != null) {
                    worldConfig = loaded;
                    return;
                }
            }

            // Create default world configuration if missing
            worldConfig = new WorldConfiguration("Default World");
            fr.enac.drone.model.world.WorldPersistence.saveWorldConfiguration(worldConfig, DEFAULT_WORLD_FILENAME);
        } catch (Exception ex) {
            System.err.println("Failed to load or create default world: " + ex.getMessage());
            worldConfig = new WorldConfiguration("Default World");
        }
    }
    
    /**
     * Initializes or reinitializes the simulation with current world configuration.
     */
    private void initializeSimulation() {
        // Stop existing game loop if running
        if (gameLoop != null) {
            gameLoop.stop();
        }
        
        // Create new MVC components
        model = new DroneModel();
        controller = new DroneController(model);
        view = new SimulationView(model, worldConfig);
        
        // Set drone spawn position from world configuration
        model.setSpawnPosition(
            worldConfig.droneSpawn.posX,
            worldConfig.droneSpawn.posY,
            worldConfig.droneSpawn.posZ,
            worldConfig.droneSpawn.yaw
        );
        
        // Update the scene center
        root.setCenter(view.getRoot());
        
        // Register input handlers
        scene.setOnKeyPressed(event -> controller.addKey(event.getCode()));
        scene.setOnKeyReleased(event -> controller.removeKey(event.getCode()));
        
        // Request focus
        view.getRoot().requestFocus();
        
        // Start new game loop
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
        worldConfigItem.setOnAction(e -> {
            WorldConfigMenu dialog = new WorldConfigMenu(worldConfig, currentWorldFilename, loadedConfig -> {
                worldConfig = loadedConfig;
                initializeSimulation();
            });
            dialog.showAndWait();
            this.currentWorldFilename = dialog.getSelectedFilename();
        });
        configMenu.getItems().add(worldConfigItem);
        menuBar.getMenus().add(configMenu);
    }

    public static void main(String[] args) {
        launch(args);
    }
}