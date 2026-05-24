package fr.enac.drone.controller;

import javafx.scene.input.KeyCode;
import java.util.HashSet;
import java.util.Set;
import fr.enac.drone.model.drone.DroneModel;
import fr.enac.drone.model.world.WorldCollision;
import fr.enac.drone.model.world.WorldCollisionDetector;

/**
 * Tracks active keys and processes continuous movement logic
 * for a smooth Mode 2 flight simulation.
 *
 * Shortcuts:
 *   O → Takeoff (Arms drone and ascends to hover altitude)
 *   L → Land (Descends automatically and disarms on touch down)
 *   F → Emergency Stop (Cuts motors, drone falls to the ground)
 *   H → Return to Home (follows trail in reverse)
 */
public class DroneController {

    private final DroneModel model;
    private final WorldCollisionDetector collisionDetector;
    // Tracks currently pressed keys to allow simultaneous inputs
    private final Set<KeyCode> activeKeys = new HashSet<>();
    private Autopilot autopilot;
    private Runnable onAutopilotFinished; // Callback to clear the target
    private Runnable returnHomeAction;    // Callback for returning home
    private final JoystickService joystickService;

    public DroneController(DroneModel model, WorldCollisionDetector collisionDetector) {
        this.model = model;
        this.collisionDetector = collisionDetector;
        this.joystickService = new JoystickService();
        this.joystickService.start();
    }

    // Useful to gracefully shut down SDL when the window is closed
    public void stop() {
        this.joystickService.stop();
    }

    /** Adds a key to the active set when pressed, and handles one-shot actions. */
    public void addKey(KeyCode code) {
        activeKeys.add(code);

        // One-shot actions (handled on key-down, not in the game loop)
        if (code == KeyCode.O) {
            model.takeoff();
            cancelAutopilot();
        } else if (code == KeyCode.L) {
            model.land();
            cancelAutopilot();
        } else if (code == KeyCode.F) {
            // Motors cut: horizontal velocity zeroed, drone falls under gravity
            model.disarm();
            cancelAutopilot();
        }
    }

    private void cancelAutopilot() {
        if (autopilot != null) {
            if (onAutopilotFinished != null) onAutopilotFinished.run();
            setAutopilot(null);
        }
    }

    public void removeKey(KeyCode code) {
        activeKeys.remove(code);
    }

    public void clearKeys() {
        activeKeys.clear();
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
     * Applies drone physics and resolves collisions using the drone inertia.
     */
    private void updatePhysicsWithCollision(
            double pitchInput,
            double rollInput,
            double throttleInput,
            double deltaTime
    ) {
        // Raycast-like check for the surface exactly below the drone
        double groundYBelow = collisionDetector.getGroundHeightBelow(model);

        model.updatePhysics(
                pitchInput,
                rollInput,
                throttleInput,
                groundYBelow,
                deltaTime
        );

        // Allows handling of up to 3 collisions at a time
        for (int i = 0; i < 3; i++) {
            WorldCollision collision =
                    collisionDetector.findCollision(model);

            if (collision == null) {
                break;
            }

            model.resolveCollision(collision);
        }
    }

    /**
     * Evaluates all currently active keys and updates the model.
     * Designed to be called continuously inside a Game Loop (60 FPS).
     * Movement is blocked when the drone is not armed.
     * Gravity-based falling (after disarm) is handled inside DroneModel.
     */
    public void update(double deltaTime) {

        joystickService.update();
        
        if (joystickService.isArmPressed()) {
            model.takeoff();
            cancelAutopilot();
        }
        if (joystickService.isDisarmPressed()) {
            model.disarm();
            cancelAutopilot();
        }

        if (returnHomeAction != null && (activeKeys.contains(KeyCode.H) || joystickService.isHomePressed())) {
            returnHomeAction.run();
            activeKeys.remove(KeyCode.H); 
            cancelAutopilot();
        }

        double pitchInput    = 0;
        double rollInput     = 0;
        double throttleInput = 0;

        // ---- Autopilot Mode ----
        if (autopilot != null && autopilot.isActive()) {
            boolean joystickMoved = Math.abs(joystickService.getPitch()) > 0.05 ||
                                    Math.abs(joystickService.getRoll()) > 0.05 ||
                                    Math.abs(joystickService.getYaw()) > 0.05 ||
                                    Math.abs(joystickService.getThrottle()) > 0.05;

            if (!activeKeys.isEmpty() || joystickMoved) {
                cancelAutopilot();
            } else {
                double[] cmd = autopilot.computeControls(model.getX(), model.getZ(), model.getYaw());
                double yawCmd = cmd[0];        // -1..1, >0 = right
                double pitchCmd = cmd[1];      // -1..1, >0 = forward

                double desiredYawRate = yawCmd * model.getYawRateDegreesPerSecond();
                model.setTargetYawVelocity(desiredYawRate);
                model.updateYaw(deltaTime);

                pitchInput = pitchCmd;
                updatePhysicsWithCollision(pitchInput, 0, throttleInput, deltaTime);
                return;
            }
        }

        if (autopilot != null && !autopilot.isActive()) {
            cancelAutopilot();
        }

        // Yaw passes deltaTime directly (DroneModel guards against !armed internally)
        if (activeKeys.contains(KeyCode.A)) model.yawLeft(deltaTime);
        if (activeKeys.contains(KeyCode.D)) model.yawRight(deltaTime);

        double joyYaw = joystickService.getYaw();
        if (joyYaw < 0) model.yawLeft(deltaTime * Math.abs(joyYaw));
        if (joyYaw > 0) model.yawRight(deltaTime * joyYaw);

        // Update yaw inertia (always called to apply drag when no input)
        model.updateYaw(deltaTime);

        // Throttle
        if (activeKeys.contains(KeyCode.W)) throttleInput -= 1;
        if (activeKeys.contains(KeyCode.S)) throttleInput += 1;
        throttleInput += joystickService.getThrottle();

        // Pitch and Roll
        if (activeKeys.contains(KeyCode.UP))    pitchInput += 1;
        if (activeKeys.contains(KeyCode.DOWN))  pitchInput -= 1;
        pitchInput += joystickService.getPitch();

        if (activeKeys.contains(KeyCode.RIGHT)) rollInput  += 1;
        if (activeKeys.contains(KeyCode.LEFT))  rollInput  -= 1;
        rollInput += joystickService.getRoll();

        // Clamp inputs between -1 and 1 to prevent keyboard + joystick from doubling the speed
        pitchInput = Math.max(-1, Math.min(1, pitchInput));
        rollInput = Math.max(-1, Math.min(1, rollInput));
        throttleInput = Math.max(-1, Math.min(1, throttleInput));

        // Update drone movement while preserving inertia and handling collisions
        updatePhysicsWithCollision(
            pitchInput,
            rollInput,
            throttleInput,
            deltaTime
        );
    }
}
