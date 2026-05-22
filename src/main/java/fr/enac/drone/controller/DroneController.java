package fr.enac.drone.controller;

import javafx.scene.input.KeyCode;
import java.util.HashSet;
import java.util.Set;
import fr.enac.drone.model.drone.DroneModel;

/**
 * Tracks active keys and processes continuous movement logic
 * for a smooth Mode 2 flight simulation.
 *
 * Shortcuts:
 *   O → Arm the drone (motors ON  – enables all movement)
 *   F → Disarm / cut motors       (drone falls under gravity to the ground)
 *   H → Return to Home (follows trail in reverse)
 */
public class DroneController {

    private final DroneModel model;
    // Tracks currently pressed keys to allow simultaneous inputs
    private final Set<KeyCode> activeKeys = new HashSet<>();
    private Autopilot autopilot;
    private Runnable onAutopilotFinished; // callback pour effacer la cible
    private Runnable returnHomeAction;    // callback pour retour à la maison

    public DroneController(DroneModel model) {
        this.model = model;
    }

    /** Adds a key to the active set when pressed, and handles one-shot actions. */
    public void addKey(KeyCode code) {
        activeKeys.add(code);

        // One-shot actions (handled on key-down, not in the game loop)
        if (code == KeyCode.O) {
            model.arm();
        } else if (code == KeyCode.F) {
            // Motors cut: horizontal velocity zeroed, drone falls under gravity
            model.disarm();
        }
    }


    public void removeKey(KeyCode code) {
        activeKeys.remove(code);
    }

    public void setAutopilot(Autopilot autopilot) {
        this.autopilot = autopilot;
        if (autopilot == null) {
            model.clearTargetYaw();
        }
    }

    public void setOnAutopilotFinished(Runnable callback) {
        this.onAutopilotFinished = callback;
    }

    public void setReturnHomeAction(Runnable action) {
        this.returnHomeAction = action;
    }

    /**
     * Evaluates all currently active keys and updates the model.
     * Designed to be called continuously inside a Game Loop (60 FPS).
     * Movement is blocked when the drone is not armed.
     * Gravity-based falling (after disarm) is handled inside DroneModel.
     */
    public void update(double deltaTime) {

        if (returnHomeAction != null && activeKeys.contains(KeyCode.H)) {
            returnHomeAction.run();
            activeKeys.remove(KeyCode.H); 
        }

        double pitchInput    = 0;
        double rollInput     = 0;
        double throttleInput = 0;

        // ---- Autopilot Mode ----
        if (autopilot != null && autopilot.isActive()) {
            if (!activeKeys.isEmpty()) {
                if (onAutopilotFinished != null) onAutopilotFinished.run();
                setAutopilot(null);
            } else {
                double[] cmd = autopilot.computeControls(model.getX(), model.getZ(), model.getYaw());
                double yawCmd = cmd[0];        // -1..1, >0 = droite
                double pitchCmd = cmd[1];      // -1..1, >0 = avant

                double desiredYawRate = yawCmd * 40.0; // °/s
                model.setTargetYawVelocity(desiredYawRate);
                model.updateYaw(deltaTime);

                pitchInput = pitchCmd;
                model.updatePhysics(pitchInput, 0, throttleInput, deltaTime);
                return;
            }
        }

        if (autopilot != null && !autopilot.isActive()) {
            if (onAutopilotFinished != null) onAutopilotFinished.run();
            setAutopilot(null);
        }

        // Yaw passes deltaTime directly (DroneModel guards against !armed internally)
        if (activeKeys.contains(KeyCode.A)) model.yawLeft(deltaTime);
        if (activeKeys.contains(KeyCode.D)) model.yawRight(deltaTime);

        // Update yaw inertia (always called to apply drag when no input)
        model.updateYaw(deltaTime);

        // Throttle
        if (activeKeys.contains(KeyCode.W)) throttleInput -= 1;
        if (activeKeys.contains(KeyCode.S)) throttleInput += 1;

        // Pitch and Roll
        if (activeKeys.contains(KeyCode.UP))    pitchInput += 1;
        if (activeKeys.contains(KeyCode.DOWN))  pitchInput -= 1;
        if (activeKeys.contains(KeyCode.RIGHT)) rollInput  += 1;
        if (activeKeys.contains(KeyCode.LEFT))  rollInput  -= 1;

        // Inject the exact time elapsed into the physics engine
        // DroneModel.updatePhysics handles armed/falling states internally
        model.updatePhysics(pitchInput, rollInput, throttleInput, deltaTime);
    }
}