package fr.enac.drone.controller;

import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Wraps game controller polling and maps controller state to simulator inputs.
 */
public class JoystickService {
    private ControllerManager controllers;
    private double verticalInput = 0;
    private double yawInput = 0;
    private double forwardInput = 0;
    private double lateralInput = 0;
    private boolean armPressed = false;
    private boolean homePressed = false;
    private boolean landPressed = false;
    private boolean emergencyPressed = false;
    private boolean pausePressed = false;
    private boolean pauseWasPressed = false;
    private boolean resetPressed = false;
    private boolean resetWasPressed = false;
    private double zoomInput = 0.0;

    /**
     * Creates the controller manager used by Jamepad.
     */
    public JoystickService() {
        controllers = new ControllerManager();
    }

    /**
     * Initializes SDL gamepad support and loads bundled controller mappings.
     */
    public void start() {
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(new OutputStream() {
            /**
             * Discards a byte while SDL initialization output is muted.
             *
             * @param b ignored byte
             */
            @Override
            public void write(int b) {}
        }));
        try {
            controllers.initSDLGamepad();
        } finally {
            System.setErr(originalErr);
        }

        // Load and inject custom mappings.
        try {
            InputStream is = getClass().getResourceAsStream("/gamecontrollerdb.txt");
            if (is != null) {
                File temp = File.createTempFile("gamecontrollerdb", ".txt");
                temp.deleteOnExit(); // Ensure the file is deleted when the game closes
                Files.copy(is, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
                controllers.addMappingsFromFile(temp.getAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("Warning: failed to inject gamecontrollerdb.txt mappings.");
        }

        System.out.println("Joystick service started.");
    }

    /**
     * Releases SDL gamepad resources.
     */
    public void stop() {
        controllers.quitSDLGamepad();
        System.out.println("Joystick service stopped.");
    }

    /**
     * Polls the first connected controller and updates normalized input fields.
     */
    public void update() {
        controllers.update();
        ControllerState state = controllers.getState(0); // Get the first controller (index 0)

        pauseWasPressed = pausePressed;
        resetWasPressed = resetPressed;

        if (state.isConnected) {
            // Mode 2 - Add 10% (0.1) deadzone for loose axes
            verticalInput = applyDeadzone(state.leftStickY, 0.1);
            yawInput      = applyDeadzone(state.leftStickX, 0.1);
            forwardInput  = applyDeadzone(-state.rightStickY, 0.1);
            lateralInput  = applyDeadzone(state.rightStickX, 0.1);
            
            armPressed = state.a;
            landPressed = state.x;
            emergencyPressed = state.b;
            homePressed = state.y;
            pausePressed = state.start;
            resetPressed = state.back;
            
            zoomInput = 0;
            // Right side (RB/RT): Zoom In (Decrease Radius)
            if (state.rb || state.rightTrigger > 0.1) zoomInput -= 1.0;
            // Left side (LB/LT): Zoom Out (Increase Radius)
            if (state.lb || state.leftTrigger > 0.1) zoomInput += 1.0;
            // Clamp value
            zoomInput = Math.max(-1.0, Math.min(1.0, zoomInput));
        } else {
            verticalInput = yawInput = forwardInput = lateralInput = zoomInput = 0.0;
            armPressed = homePressed = landPressed = emergencyPressed = false;
            pausePressed = resetPressed = false;
        }
    }

    /**
     * Applies a symmetric deadzone and rescales the remaining analog range.
     *
     * @param value raw axis value
     * @param deadzone ignored center range
     * @return normalized post-deadzone value
     */
    private double applyDeadzone(double value, double deadzone) {
        if (Math.abs(value) < deadzone) {
            return 0.0;
        }
        // Normalize post-deadzone value and scale by 1.2x to ensure 
        // the analog stick can reach 1.0 (max speed) despite the circular physical restriction
        return Math.signum(value) * Math.min(1.0, ((Math.abs(value) - deadzone) / (1.0 - deadzone)) * 1.2);
    }

    /**
     * Returns the normalized altitude axis value.
     *
     * @return vertical input in the range {@code [-1, 1]}
     */
    public double getVerticalInput() { return verticalInput; }

    /**
     * Returns the normalized yaw axis value.
     *
     * @return yaw input in the range {@code [-1, 1]}
     */
    public double getYawInput() { return yawInput; }

    /**
     * Returns the normalized forward axis value.
     *
     * @return forward input in the range {@code [-1, 1]}
     */
    public double getForwardInput() { return forwardInput; }

    /**
     * Returns the normalized lateral axis value.
     *
     * @return lateral input in the range {@code [-1, 1]}
     */
    public double getLateralInput() { return lateralInput; }

    /**
     * Returns whether the arm/takeoff button is pressed.
     *
     * @return {@code true} when the arm button is pressed
     */
    public boolean isArmPressed() { return armPressed; }

    /**
     * Returns whether the return-to-home button is pressed.
     *
     * @return {@code true} when the home button is pressed
     */
    public boolean isHomePressed() { return homePressed; }

    /**
     * Returns whether the land button is pressed.
     *
     * @return {@code true} when the land button is pressed
     */
    public boolean isLandPressed() { return landPressed; }

    /**
     * Returns whether the emergency stop button is pressed.
     *
     * @return {@code true} when the emergency button is pressed
     */
    public boolean isEmergencyPressed() { return emergencyPressed; }

    /**
     * Returns whether pause was pressed since the previous poll.
     *
     * @return {@code true} only on the rising edge of the pause button
     */
    public boolean isPauseJustPressed() { return pausePressed && !pauseWasPressed; }

    /**
     * Returns whether reset was pressed since the previous poll.
     *
     * @return {@code true} only on the rising edge of the reset button
     */
    public boolean isResetJustPressed() { return resetPressed && !resetWasPressed; }

    /**
     * Returns the normalized minimap zoom input.
     *
     * @return zoom input in the range {@code [-1, 1]}
     */
    public double getZoomInput() { return zoomInput; }
    
    /**
     * Returns whether any controller axis or button is active.
     *
     * @return {@code true} when the controller currently provides input
     */
    public boolean hasAnyInput() {
        return Math.abs(verticalInput) > 0
                || Math.abs(yawInput) > 0
                || Math.abs(forwardInput) > 0
                || Math.abs(lateralInput) > 0
                || armPressed || homePressed || landPressed || emergencyPressed
                || pausePressed || resetPressed
                || Math.abs(zoomInput) > 0;
    }
}
