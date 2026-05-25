package fr.enac.drone;

import fr.enac.drone.controller.DroneController;
import fr.enac.drone.input.InputDevice;
import fr.enac.drone.model.SimulationState;
import fr.enac.drone.model.WorldPosition2D;
import fr.enac.drone.model.drone.DroneControlSettings;
import fr.enac.drone.model.drone.DroneModel;
import fr.enac.drone.model.drone.DroneSpawn;
import fr.enac.drone.model.world.WorldConfiguration;
import fr.enac.drone.model.world.WorldCollisionDetector;
import fr.enac.drone.navigation.Autopilot;
import fr.enac.drone.navigation.PathPlanner;
import fr.enac.drone.persistence.WorldPersistence;
import fr.enac.drone.view.SimulationView;
import fr.enac.drone.view.settings.SettingsState;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
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
    private SimulationState simulationState = SimulationState.READY;

    private double droneYawSensitivity =
            DroneControlSettings.YAW_SENSITIVITY.getDefaultValue();
    private double droneMaxHorizontalSpeed =
            DroneControlSettings.MAX_HORIZONTAL_SPEED.getDefaultValue();
    private double droneMaxVerticalSpeed =
            DroneControlSettings.MAX_VERTICAL_SPEED.getDefaultValue();
    private double minimapZoom = 1000.0;
    private final SettingsState settingsState = new SettingsState();

    private InputDevice currentInputDevice = InputDevice.KEYBOARD;

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
        primaryStage.setOnCloseRequest(event -> stopGameLoop());

        // Prevent ghost keys from getting stuck when clicking outside the window or Alt-Tabbing
        primaryStage.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (!isFocused && controller != null) {
                controller.clearKeys();
            }
        });

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
        simulationState = SimulationState.READY;
        createMvcComponents();
        setupEventHandlers();

        PathPlanner pathPlanner = new PathPlanner(worldConfig);

        controller.setOnAutopilotFinished(() -> view.clearMinimapTarget());

        view.setMinimapTargetHandler(worldPos -> {
            if (simulationState != SimulationState.RUNNING) {
                view.clearMinimapTarget();
                return;
            }

            double startX = model.getX();
            double startY = model.getY();
            double startZ = model.getZ();

            Thread pathfindingThread = new Thread(() -> {
                List<WorldPosition2D> waypoints =
                        pathPlanner.findPath(startX, startY, startZ, worldPos.x(), worldPos.z());
                
                Platform.runLater(() -> {
                    if (!waypoints.isEmpty()) {
                        controller.setAutopilot(new Autopilot(waypoints));
                    } else {
                        view.clearMinimapTarget();
                        System.out.println("No path found to the target.");
                    }
                });
            });
            pathfindingThread.setDaemon(true);
            pathfindingThread.start();
        });

        // Return to Home action (H key)
        controller.setReturnHomeAction(() -> {
            // Set the baseline to the drone's spawn altitude instead of a hardcoded Y=0
            double highestPointY = worldConfig.getDroneSpawn().getPosY();
            if (worldConfig != null && worldConfig.getObjects() != null) {
                for (var obj : worldConfig.getObjects()) {
                    // Note: Y axis is downwards, so higher obstacles have smaller/more negative Y values
                    double topY = obj.getTopY();
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
        stopController();
    }

    private void stopController() {
        if (controller != null) {
            controller.stop();
            controller = null;
        }
    }

    private void createMvcComponents() {

        WorldCollisionDetector collisionDetector = new WorldCollisionDetector(worldConfig);

        model = new DroneModel();
        model.setYawSensitivity(droneYawSensitivity);
        model.setMaxHorizontalSpeed(droneMaxHorizontalSpeed);
        model.setMaxVerticalSpeed(droneMaxVerticalSpeed);

        DroneSpawn spawn = worldConfig.getDroneSpawn();

        model.setSpawnPosition(
                spawn.getPosX(),
                spawn.getPosY(),
                spawn.getPosZ(),
                spawn.getYaw()
        );

        controller = new DroneController(model, collisionDetector);
        controller.setKeyboardLayout(settingsState.getKeyboardLayout());

        view = new SimulationView(
                model,
                worldConfig,
                currentWorldFilename,
                this::handleWorldLoaded,
                settingsState
        );
        view.setSimulationState(simulationState);
        view.clearTrail();
        view.setMinimapZoom(minimapZoom);
        view.setCommandHelpVisible(settingsState.isShowCommandHelp());
        view.updateCommandHelp(currentInputDevice, settingsState.getKeyboardLayout());
        
        view.setCommandHelpVisibilityHandler(visible -> {
            view.setCommandHelpVisible(visible);
        });
        view.setKeyboardLayoutHandler(layout -> {
            if (controller != null) {
                controller.setKeyboardLayout(layout);
            }
            view.updateCommandHelp(currentInputDevice, layout);
        });

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
                if (event.getEventType() == KeyEvent.KEY_PRESSED) {
                    view.closeSettingsDrawer();
                    if (controller != null) {
                        controller.clearKeys();
                    }
                    view.getRoot().requestFocus();
                }
                event.consume();
            }
        };

        scene.addEventFilter(KeyEvent.ANY, settingsDismissFilter);
        scene.setOnKeyPressed(this::handleKeyPressed);
        scene.setOnKeyReleased(e -> {
            if (controller != null) {
                controller.removeKey(e.getCode());
            }
        });
    }

    private void handleKeyPressed(KeyEvent event) {
        KeyCode code = event.getCode();

        if (code == KeyCode.ESCAPE) {
            closeApplication();
            event.consume();
            return;
        }

        if (code == KeyCode.SPACE || code == KeyCode.ENTER) {
            toggleStartPauseResume();
            event.consume();
            return;
        }

        if (code == KeyCode.R) {
            resetSimulationSession();
            event.consume();
            return;
        }

        if (simulationState == SimulationState.READY
                && DroneController.isFlightStartKey(code)) {
            setSimulationState(SimulationState.RUNNING);
            controller.addKey(code);
            event.consume();
            return;
        }

        if (simulationState == SimulationState.RUNNING) {
            controller.addKey(code);
            event.consume();
        }
    }

    private void toggleStartPauseResume() {
        switch (simulationState) {
            case READY -> setSimulationState(SimulationState.RUNNING);
            case RUNNING -> pauseSimulationSession();
            case PAUSED -> setSimulationState(SimulationState.RUNNING);
        }
    }

    private void pauseSimulationSession() {
        controller.clearKeys();
        setSimulationState(SimulationState.PAUSED);
    }

    private void resetSimulationSession() {
        controller.clearKeys();
        controller.setAutopilot(null);
        view.clearMinimapTarget();
        view.clearTrail();
        resetDroneToWorldSpawn();
        setSimulationState(SimulationState.READY);
    }

    private void resetDroneToWorldSpawn() {
        DroneSpawn spawn = worldConfig.getDroneSpawn();

        model.setSpawnPosition(
                spawn.getPosX(),
                spawn.getPosY(),
                spawn.getPosZ(),
                spawn.getYaw()
        );
    }

    private void setSimulationState(SimulationState simulationState) {
        this.simulationState =
                Objects.requireNonNull(
                        simulationState,
                        "Simulation state cannot be null"
                );

        if (view != null) {
            view.setSimulationState(simulationState);
            view.render();
        }
    }

    private void closeApplication() {
        stopGameLoop();
        Platform.exit();
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

                // Cap deltaTime to prevent physics explosions/teleportation after lag spikes
                if (deltaTime > 0.05) {
                    deltaTime = 0.05;
                }

                if (controller == null) {
                    return;
                }

                controller.updateInputDevices();

                double zoomInput = controller.getZoomInput();
                if (zoomInput != 0.0) {
                    view.adjustMinimapZoom(zoomInput * 2000.0 * deltaTime);
                }

                if (controller.isPauseJustPressed()) {
                    toggleStartPauseResume();
                }

                if (controller.isResetJustPressed()) {
                    resetSimulationSession();
                }

                if (simulationState == SimulationState.READY
                        && controller.hasJoystickFlightStartInput()) {
                    setSimulationState(SimulationState.RUNNING);
                }

                handleInputDeviceChanged(controller.getLastUsedDevice());

                if (simulationState == SimulationState.RUNNING) {
                    controller.update(deltaTime);
                }

                view.render();
            }
        };

        gameLoop.start();
    }

    private void handleInputDeviceChanged(InputDevice newDevice) {
        if (newDevice == currentInputDevice) {
            return;
        }

        currentInputDevice = newDevice;
        view.updateCommandHelp(currentInputDevice, settingsState.getKeyboardLayout());
    }

    private void handleWorldLoaded(WorldConfiguration config, String filename) {

        preserveDroneSettings();

        this.worldConfig = Objects.requireNonNull(config);
        this.currentWorldFilename = Objects.requireNonNull(filename);
        settingsState.setSelectedWorldFilename(filename);
        simulationState = SimulationState.READY;

        initializeSimulation();
    }

    private void preserveDroneSettings() {
        if (model == null) {
            return;
        }

        droneYawSensitivity = model.getYawSensitivity();
        droneMaxHorizontalSpeed = model.getMaxHorizontalSpeed();
        droneMaxVerticalSpeed = model.getMaxVerticalSpeed();
        
        if (view != null) {
            minimapZoom = view.getMinimapZoom();
        }
    }

    @Override
    public void stop() {
        stopGameLoop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
