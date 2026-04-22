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
 * Initializes the MVC components and manages the primary stage setup.
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

        // Handle keyboard input and trigger view updates
        scene.setOnKeyPressed(event -> {
            controller.handleKeyPress(event.getCode());
        });

        AnimationTimer renderLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                view.render();
            }
        };
        renderLoop.start();

        // Stage configuration
        primaryStage.setTitle("FPV Drone Simulator");
        primaryStage.setScene(scene);
        
        // Request focus to ensure input capture on startup
        view.getRoot().requestFocus(); 
        
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
