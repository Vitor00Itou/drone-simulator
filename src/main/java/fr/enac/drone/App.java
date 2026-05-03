package fr.enac.drone;

import fr.enac.drone.controller.DroneController;
import fr.enac.drone.model.DroneModel;
import fr.enac.drone.view.SimulationView;
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