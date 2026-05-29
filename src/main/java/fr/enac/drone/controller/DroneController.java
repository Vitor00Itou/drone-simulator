package fr.enac.drone.controller;

import javafx.scene.input.KeyCode;
import java.util.HashSet;
import java.util.Set;
import fr.enac.drone.input.InputDevice;
import fr.enac.drone.input.KeyboardLayout;
import fr.enac.drone.model.drone.DroneModel;
import fr.enac.drone.model.drone.FlightState;
import fr.enac.drone.model.world.WorldCollision;
import fr.enac.drone.model.world.WorldCollisionDetector;
import fr.enac.drone.navigation.Autopilot;
import fr.enac.drone.navigation.FlightControlCommand;

/**
 * Tracks active keys and processes continuous movement logic
 * for a smooth Mode 2 flight simulation.
 *
 * <p>Keyboard shortcuts: O for takeoff, L for land, F for emergency stop,
 * and H for return to home.</p>
 */
public class DroneController {
    private final DroneModel model;
    private final WorldCollisionDetector collisionDetector;
    // Tracks currently pressed keys to allow simultaneous inputs
    private final Set<KeyCode> activeKeys = new HashSet<>();
    private final JoystickService joystickService;
    private Autopilot autopilot;
    private Runnable onAutopilotFinished; // Callback to clear the target
    private Runnable returnHomeAction;    // Callback for returning home
    private InputDevice lastUsedDevice = InputDevice.KEYBOARD;
    private KeyboardLayout keyboardLayout = KeyboardLayout.QWERTY;

    /**
     * Creates a controller bound to a drone model and collision detector.
     *
     * @param model drone model to update
     * @param collisionDetector collision detector for the active world
     */
    public DroneController(DroneModel model, WorldCollisionDetector collisionDetector) {
        this.model = model;
        this.collisionDetector = collisionDetector;
        this.joystickService = new JoystickService();
        this.joystickService.start();
    }

    /**
     * Returns whether a key is used for continuous piloting.
     *
     * @param code key code to inspect
     * @return {@code true} when the key can start or affect flight controls
     */
    public static boolean isPilotingKey(KeyCode code) {
        return code == KeyCode.W || code == KeyCode.S || code == KeyCode.A || code == KeyCode.D ||
               code == KeyCode.Z || code == KeyCode.Q ||
               code == KeyCode.UP || code == KeyCode.DOWN || code == KeyCode.LEFT || code == KeyCode.RIGHT;
    }

    /**
     * Returns whether a key can start a ready simulation session.
     *
     * @param code key code to inspect
     * @return {@code true} for piloting keys or the takeoff key
     */
    public static boolean isFlightStartKey(KeyCode code) {
        return isPilotingKey(code) || code == KeyCode.O;
    }

    /**
     * Polls joystick state and records the active input source.
     */
    public void updateInputDevices() {
        joystickService.update();
        if (joystickService.hasAnyInput()) {
            lastUsedDevice = InputDevice.JOYSTICK;
        }
    }

    /**
     * Returns whether the joystick pause control was just pressed.
     *
     * @return {@code true} on a pause button rising edge
     */
    public boolean isPauseJustPressed() {
        return joystickService.isPauseJustPressed();
    }

    /**
     * Returns whether the joystick reset control was just pressed.
     *
     * @return {@code true} on a reset button rising edge
     */
    public boolean isResetJustPressed() {
        return joystickService.isResetJustPressed();
    }

    /**
     * Returns whether joystick input should start a ready simulation session.
     *
     * @return {@code true} when a joystick flight-start control is active
     */
    public boolean hasJoystickFlightStartInput() {
        return joystickService.isArmPressed()
                || Math.abs(joystickService.getVerticalInput()) > 0
                || Math.abs(joystickService.getYawInput()) > 0
                || Math.abs(joystickService.getForwardInput()) > 0
                || Math.abs(joystickService.getLateralInput()) > 0;
    }

    /**
     * Stops controller services and releases joystick resources.
     */
    public void stop() {
        joystickService.stop();
    }

    /**
     * Adds a pressed key to the active set and handles one-shot flight actions.
     *
     * @param code pressed key code
     */
    public void addKey(KeyCode code) {
        activeKeys.add(code);
        lastUsedDevice = InputDevice.KEYBOARD;

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

    /**
     * Removes a released key from the active set.
     *
     * @param code released key code
     */
    public void removeKey(KeyCode code) {
        activeKeys.remove(code);
    }

    /**
     * Clears all active keyboard inputs.
     */
    public void clearKeys() {
        activeKeys.clear();
    }

    /**
     * Sets or clears the active autopilot.
     *
     * @param autopilot autopilot instance, or {@code null} for manual control
     */
    public void setAutopilot(Autopilot autopilot) {
        this.autopilot = autopilot;
        if (autopilot == null) {
            model.clearTargetYaw();
        }
    }

    /**
     * Registers a callback invoked when autopilot control is cancelled or completed.
     *
     * @param callback callback to run after autopilot finishes
     */
    public void setOnAutopilotFinished(Runnable callback) {
        this.onAutopilotFinished = callback;
    }

    /**
     * Registers the action used by the return-to-home command.
     *
     * @param action return-to-home action
     */
    public void setReturnHomeAction(Runnable action) {
        this.returnHomeAction = action;
    }

    /**
     * Cancels autopilot and notifies the registered completion callback.
     */
    private void cancelAutopilot() {
        if (autopilot == null) {
            return;
        }

        if (onAutopilotFinished != null) {
            onAutopilotFinished.run();
        }
        setAutopilot(null);
    }

    /**
     * Applies drone physics and resolves collisions using the drone inertia.
     */
    private void updatePhysicsWithCollision(
            double forwardInput,
            double lateralInput,
            double verticalInput,
            double deltaTime
    ) {
        // Raycast-like check for the surface exactly below the drone
        double groundYBelow = collisionDetector.getGroundHeightBelow(model);

        model.updatePhysics(
                forwardInput,
                lateralInput,
                verticalInput,
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
     * Evaluates active inputs and updates the model.
     * Designed to be called continuously inside the simulation loop.
     * Movement is blocked when the drone is not armed.
     * Gravity-based falling (after disarm) is handled inside DroneModel.
     *
     * @param deltaTime elapsed simulation time in seconds
     */
    public void update(double deltaTime) {

        if (joystickService.isArmPressed()) {
            model.takeoff();
            cancelAutopilot();
        }

        if (joystickService.isLandPressed()) {
            model.land();
            cancelAutopilot();
        }
        if (joystickService.isEmergencyPressed()) {
            model.disarm();
            cancelAutopilot();
        }

        if (returnHomeAction != null
                && (activeKeys.contains(KeyCode.H) || joystickService.isHomePressed())) {
            returnHomeAction.run();
            activeKeys.remove(KeyCode.H);
            cancelAutopilot();
        }

        double forwardInput = 0;
        double lateralInput = 0;
        double verticalInput = 0;

        // ---- Autopilot Mode ----
        if (autopilot != null && autopilot.isActive()) {
            boolean joystickMoved =
                    Math.abs(joystickService.getForwardInput()) > 0.05
                            || Math.abs(joystickService.getLateralInput()) > 0.05
                            || Math.abs(joystickService.getYawInput()) > 0.05
                            || Math.abs(joystickService.getVerticalInput()) > 0.05;

            if (!activeKeys.isEmpty() || joystickMoved) {
                cancelAutopilot();
            } else {
                FlightControlCommand command =
                        autopilot.computeControls(model.getX(), model.getZ(), model.getYaw());
                double yawInput = command.yawInput();
                double autopilotForwardInput = command.forwardInput();
                double autopilotLateralInput = command.lateralInput();

                double desiredYawRate = yawInput * model.getYawRateDegreesPerSecond();
                model.setTargetYawVelocity(desiredYawRate);
                model.updateYaw(deltaTime);

                updatePhysicsWithCollision(autopilotForwardInput, autopilotLateralInput, verticalInput, deltaTime);
                return;
            }
        }

        if (autopilot != null && !autopilot.isActive()) {
            cancelAutopilot();
        }

        KeyCode leftKey = keyboardLayout.getYawLeftKey();
        KeyCode verticalUpKey = keyboardLayout.getVerticalUpKey();
        
        double yawInput = 0.0;
        if (activeKeys.contains(leftKey)) yawInput -= 1.0;
        if (activeKeys.contains(KeyCode.D)) yawInput += 1.0;
        yawInput += joystickService.getYawInput();
        yawInput = clampInput(yawInput);

        if (Math.abs(yawInput) > 0.001) {
            model.setManualYawInput(yawInput);
        } else if ((autopilot == null || !autopilot.isActive())
                && model.getFlightState() != FlightState.RETURNING_HOME) {
            model.clearTargetYaw();
        }

        model.updateYaw(deltaTime);

        // Vertical movement
        if (activeKeys.contains(verticalUpKey)) verticalInput -= 1;
        if (activeKeys.contains(KeyCode.S)) verticalInput += 1;
        verticalInput += joystickService.getVerticalInput();

        // Horizontal movement relative to the drone heading
        if (activeKeys.contains(KeyCode.UP))    forwardInput += 1;
        if (activeKeys.contains(KeyCode.DOWN))  forwardInput -= 1;
        forwardInput += joystickService.getForwardInput();

        if (activeKeys.contains(KeyCode.RIGHT)) lateralInput  += 1;
        if (activeKeys.contains(KeyCode.LEFT))  lateralInput  -= 1;
        lateralInput += joystickService.getLateralInput();

        // Clamp inputs between -1 and 1 to prevent keyboard + joystick from doubling the speed
        forwardInput = clampInput(forwardInput);
        lateralInput = clampInput(lateralInput);
        verticalInput = clampInput(verticalInput);

        // Update drone movement while preserving inertia and handling collisions
        updatePhysicsWithCollision(
            forwardInput,
            lateralInput,
            verticalInput,
            deltaTime
        );
    }

    /**
     * Clamps an input axis to the normalized range.
     *
     * @param value input value
     * @return value clamped to {@code [-1, 1]}
     */
    private double clampInput(double value) {
        return Math.max(-1, Math.min(1, value));
    }

    /**
     * Returns the current minimap zoom input.
     *
     * @return zoom input in the range {@code [-1, 1]}
     */
    public double getZoomInput() {
        return joystickService.getZoomInput();
    }
    
    /**
     * Returns the last input device that provided controls.
     *
     * @return keyboard or joystick
     */
    public InputDevice getLastUsedDevice() {
        return lastUsedDevice;
    }
    
    /**
     * Sets the keyboard layout used for yaw and altitude controls.
     *
     * @param layout layout to use, or {@code null} to restore QWERTY
     */
    public void setKeyboardLayout(KeyboardLayout layout) {
        this.keyboardLayout = layout == null ? KeyboardLayout.QWERTY : layout;
    }
}
