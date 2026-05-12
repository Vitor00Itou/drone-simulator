package fr.enac.drone;

import fr.enac.drone.controller.Autopilot;
import fr.enac.drone.controller.DroneController;
import fr.enac.drone.model.PathPlanner;
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

import java.util.List;
import java.util.Objects;

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
        createMenuBar();
        this.root = new BorderPane();
        root.setTop(menuBar);

        loadOrCreateDefaultWorld();

        this.scene = new Scene(root, 800, 600);
        primaryStage.setTitle("FPV Drone Simulator");
        primaryStage.setScene(scene);
        primaryStage.show();

        initializeSimulation();
    }

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

    private void initializeSimulation() {
        stopGameLoop();
        createMvcComponents();
        setupEventHandlers();

        // ----- GOTO : clic sur la minimap -----
        PathPlanner pathPlanner = new PathPlanner(worldConfig);
        controller.setOnAutopilotFinished(() -> view.clearMinimapTarget());
        view.setMinimapTargetHandler(worldPos -> {
            double startX = model.getX();
            double startZ = model.getZ();
            List<double[]> waypoints = pathPlanner.findPath(startX, startZ, worldPos[0], worldPos[1]);
            if (!waypoints.isEmpty()) {
                Autopilot autopilot = new Autopilot(waypoints);
                controller.setAutopilot(autopilot);
            } else {
                view.clearMinimapTarget(); // pas de chemin → on enlève la croix
                System.out.println("Aucun chemin trouvé vers la cible.");
            }
        });

        startGameLoop();
    }

    private void stopGameLoop() {
        if (gameLoop != null) gameLoop.stop();
    }

    private void createMvcComponents() {
        model = new DroneModel();
        controller = new DroneController(model);
        view = new SimulationView(model, worldConfig);

        DroneSpawn spawn = worldConfig.getDroneSpawn();
        model.setSpawnPosition(spawn.getPosX(), spawn.getPosY(), spawn.getPosZ(), spawn.getYaw());

        root.setCenter(view.getRoot());
    }

    private void setupEventHandlers() {
        view.getRoot().requestFocus();
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
        this.worldConfig = Objects.requireNonNull(config);
        this.currentWorldFilename = Objects.requireNonNull(filename);
        this.initializeSimulation();
    }

    public static void main(String[] args) {
        launch(args);
    }
}