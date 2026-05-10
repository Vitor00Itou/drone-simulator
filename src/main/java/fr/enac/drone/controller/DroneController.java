package fr.enac.drone.controller;

import javafx.scene.input.KeyCode;
import java.util.HashSet;
import java.util.Set;

import fr.enac.drone.model.drone.DroneModel;

/**
 * Tracks active keys and processes continuous movement logic 
 * for a smooth Mode 2 flight simulation.
 */
public class DroneController {
    private final DroneModel model;
    
    // Tracks currently pressed keys to allow simultaneous inputs
    private final Set<KeyCode> activeKeys = new HashSet<>();

    public DroneController(DroneModel model) {
        this.model = model;
    }

    /** Adds a key to the active set when pressed. */
    public void addKey(KeyCode code) {
        activeKeys.add(code);
    }

    /** Removes a key from the active set when released. */
    public void removeKey(KeyCode code) {
        activeKeys.remove(code);
    }

    /**
     * Evaluates all currently active keys and updates the model.
     * Designed to be called continuously inside a Game Loop (60 FPS).
     */
    public void update(double deltaTime) {
        double pitchInput = 0;
        double rollInput = 0;
        double throttleInput = 0;

        // Yaw passes the deltaTime directly
        if (activeKeys.contains(KeyCode.A)) model.yawLeft(deltaTime);
        if (activeKeys.contains(KeyCode.D)) model.yawRight(deltaTime);
        
        // Update yaw inertia (always called to apply drag when no input)
        model.updateYaw(deltaTime);

        // Throttle
        if (activeKeys.contains(KeyCode.W)) throttleInput -= 1;
        if (activeKeys.contains(KeyCode.S)) throttleInput += 1;

        // Pitch and Roll
        if (activeKeys.contains(KeyCode.UP)) pitchInput += 1;
        if (activeKeys.contains(KeyCode.DOWN)) pitchInput -= 1;
        if (activeKeys.contains(KeyCode.RIGHT)) rollInput += 1;
        if (activeKeys.contains(KeyCode.LEFT)) rollInput -= 1;

        // Inject the exact time elapsed into the physics engine
        model.updatePhysics(pitchInput, rollInput, throttleInput, deltaTime);
    }
}