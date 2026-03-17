package fr.enac.drone.controller;

import fr.enac.drone.model.DroneModel;
import javafx.scene.input.KeyCode;

/**
 * Maps keyboard inputs to DroneModel commands following the Mode 2 standard.
 */
public class DroneController {
    private final DroneModel model;

    public DroneController(DroneModel model) {
        this.model = model;
    }

    public void handleKeyPress(KeyCode code) {
        switch (code) {
            // Left Analog Stick: Throttle and Yaw
            case W -> model.throttleUp();
            case S -> model.throttleDown();
            case A -> model.yawLeft();
            case D -> model.yawRight();
            
            // Right Analog Stick: Pitch and Roll
            case UP -> model.pitchForward();
            case DOWN -> model.pitchBackward();
            case LEFT -> model.rollLeft();
            case RIGHT -> model.rollRight();
            
            default -> {}
        }
    }
}