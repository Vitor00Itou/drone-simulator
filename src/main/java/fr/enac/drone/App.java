package fr.enac.drone;

import fr.enac.drone.controller.DroneController;
import fr.enac.drone.model.DroneModel;
import fr.enac.drone.view.SimulationView;
import fr.enac.drone.view.SettingsView;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main entry point for the Drone Simulator application at ENAC.
 * Initializes the MVC components and manages the Game Loop.
 */
public class App extends Application {

    @Override
    public void start(Stage primaryStage) {

        // Initialize MVC components
        DroneModel model = new DroneModel();
        DroneController controller = new DroneController(model);
        SimulationView view = new SimulationView(model);

        // Configure the main scene
        Scene scene = new Scene(view.getRoot(), 800, 600);

        // 1. Register Key Presses
        scene.setOnKeyPressed(event -> controller.addKey(event.getCode()));

        // 2. Register Key Releases
        scene.setOnKeyReleased(event -> controller.removeKey(event.getCode()));
        
        // Stage configuration
        primaryStage.setTitle("FPV Drone Simulator");
        primaryStage.setScene(scene);

        /**
        * Handles the opening of the settings screen when the user clicks the settings button.
        * Creates a new settings view and switches the scene.
        */
        view.getSettingsButton().setOnAction(event -> {
            SettingsView newSettingsView = new SettingsView(model.getCurrentWorldMode());
            Scene settingsScene = new Scene(newSettingsView, 800, 600);

            // Handle back button: return to the simulation without applying changes
            newSettingsView.getBackButton().setOnAction(backEvent -> {
                primaryStage.setScene(scene);
                view.getRoot().requestFocus();
            });

            // Handle save button: apply selected world mode and refresh the environment
            newSettingsView.getSaveButton().setOnAction(saveEvent -> {
                model.setWorldMode(newSettingsView.getSelectedWorldMode());
                view.refreshWorld();

                primaryStage.setScene(scene);
                view.getRoot().requestFocus();
            });

            // Switch to the settings screen
            primaryStage.setScene(settingsScene);
        });
        
        // Request focus to ensure input capture on startup
        view.getRoot().requestFocus(); 
        
        primaryStage.show();

        // 3. The Game Loop (Dynamic FPS with Delta Time)
        AnimationTimer gameLoop = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                // Initialize the timer on the first frame
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }

                // Calculate the time elapsed in seconds
                double deltaTime = (now - lastUpdate) / 1_000_000_000.0;
                lastUpdate = now;

                // Pass the elapsed time to the controller
                controller.update(deltaTime); 
                view.render();
            }
        };
        gameLoop.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}