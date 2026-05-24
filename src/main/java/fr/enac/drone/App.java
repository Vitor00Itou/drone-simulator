package fr.enac.drone;

import fr.enac.drone.controller.Autopilot;
import fr.enac.drone.controller.DroneController;
import fr.enac.drone.model.PathPlanner;
import fr.enac.drone.model.drone.DroneControlSettings;
import fr.enac.drone.model.drone.DroneModel;
import fr.enac.drone.model.drone.DroneSpawn;
import fr.enac.drone.model.world.WorldConfiguration;
import fr.enac.drone.model.world.WorldPersistence;
import fr.enac.drone.model.world.WorldCollisionDetector;
import fr.enac.drone.view.SettingsView;
import fr.enac.drone.view.SimulationView;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.List;
import java.util.Objects;

public class App extends Application {

    private static final String DEFAULT_WORLD_FILENAME = "world_default";

    private String currentWorldFilename = DEFAULT_WORLD_FILENAME;

    private Scene scene;
    private BorderPane root;
    private EventHandler<KeyEvent> settingsDismissFilter;

    private WorldConfiguration worldConfig;
    private AnimationTimer gameLoop;

    private double droneYawSensitivity =
            DroneControlSettings.YAW_SENSITIVITY.getDefaultValue();
    private double droneMaxHorizontalSpeed =
            DroneControlSettings.MAX_HORIZONTAL_SPEED.getDefaultValue();
    private double droneMaxVerticalSpeed =
            DroneControlSettings.MAX_VERTICAL_SPEED.getDefaultValue();
    private final SettingsView.State settingsState = new SettingsView.State();

    private DroneModel model;
    private DroneController controller;
    private SimulationView view;

    @Override
    public void start(Stage primaryStage) {

        loadOrCreateDefaultWorld();

        root = new BorderPane();

        scene = new Scene(root, 800, 600);

        primaryStage.setTitle("FPV Drone Simulator");
        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
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

        PathPlanner pathPlanner = new PathPlanner(worldConfig);

        controller.setOnAutopilotFinished(() -> view.clearMinimapTarget());

        view.setMinimapTargetHandler(worldPos -> {
            double startX = model.getX();
            double startZ = model.getZ();

            List<double[]> waypoints =
                    pathPlanner.findPath(startX, startZ, worldPos[0], worldPos[1]);

            if (!waypoints.isEmpty()) {
                controller.setAutopilot(new Autopilot(waypoints));
            } else {
                view.clearMinimapTarget();
                System.out.println("Aucun chemin trouvé vers la cible.");
            }
        });

        // Return to Home action (touche H)
        controller.setReturnHomeAction(() -> {
            // Set the baseline to the drone's spawn altitude instead of a hardcoded Y=0
            double highestPointY = worldConfig.getDroneSpawn().getPosY();
            if (worldConfig != null && worldConfig.getObjects() != null) {
                for (var obj : worldConfig.getObjects()) {
                    // Note: Y axis is downwards, so higher obstacles have smaller/more negative Y values
                    double topY = obj.getPosY() - (obj.getSizeY() / 2.0);
                    if (topY < highestPointY) highestPointY = topY;
                }
            }
            model.returnToHome(highestPointY - 20.0); // Safe altitude = highest obstacle + 20 meters buffer
            view.clearMinimapTarget();
        });

        startGameLoop();
    }

    private void stopGameLoop() {
        if (gameLoop != null) gameLoop.stop();
    }

    private void createMvcComponents() {

        WorldCollisionDetector collisionDetector = new WorldCollisionDetector(worldConfig);

        model = new DroneModel();
        model.setYawSensitivity(droneYawSensitivity);
        model.setMaxHorizontalSpeed(droneMaxHorizontalSpeed);
        model.setMaxVerticalSpeed(droneMaxVerticalSpeed);

        controller = new DroneController(model, collisionDetector);
        view = new SimulationView(
                model,
                worldConfig,
                currentWorldFilename,
                this::handleWorldLoaded,
                settingsState
        );

        DroneSpawn spawn = worldConfig.getDroneSpawn();

        model.setSpawnPosition(
                spawn.getPosX(),
                spawn.getPosY(),
                spawn.getPosZ(),
                spawn.getYaw()
        );

        root.setCenter(view.getRoot());
    }

    private void setupEventHandlers() {

        Objects.requireNonNull(view).getRoot().requestFocus();

        view.setSettingsDrawerOpenedHandler(controller::clearKeys);
        view.setSettingsDrawerClosedHandler(() -> view.getRoot().requestFocus());

        if (settingsDismissFilter != null) {
            scene.removeEventFilter(KeyEvent.ANY, settingsDismissFilter);
        }

        settingsDismissFilter = event -> {
            if (view != null && view.isSettingsDrawerOpen()) {
                view.closeSettingsDrawer();
                controller.clearKeys();
                view.getRoot().requestFocus();
                event.consume();
            }
        };

        scene.addEventFilter(KeyEvent.ANY, settingsDismissFilter);
        scene.setOnKeyPressed(e -> controller.addKey(e.getCode()));
        scene.setOnKeyReleased(e -> controller.removeKey(e.getCode()));
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

    private void handleWorldLoaded(WorldConfiguration config, String filename) {

        preserveDroneSettings();

        this.worldConfig = Objects.requireNonNull(config);
        this.currentWorldFilename = Objects.requireNonNull(filename);
        settingsState.setSelectedWorldFilename(filename);

        initializeSimulation();
    }

    private void preserveDroneSettings() {
        if (model == null) {
            return;
        }

        droneYawSensitivity = model.getYawSensitivity();
        droneMaxHorizontalSpeed = model.getMaxHorizontalSpeed();
        droneMaxVerticalSpeed = model.getMaxVerticalSpeed();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
