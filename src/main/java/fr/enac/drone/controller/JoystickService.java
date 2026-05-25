package fr.enac.drone.controller;

import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

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

    public JoystickService() {
        controllers = new ControllerManager();
    }

    public void start() {
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(new OutputStream() {
            public void write(int b) {}
        }));
        try {
            controllers.initSDLGamepad();
        } finally {
            System.setErr(originalErr);
        }

        // 2. Load and inject custom mappings
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

    public void stop() {
        controllers.quitSDLGamepad();
        System.out.println("Joystick service stopped.");
    }

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

    private double applyDeadzone(double value, double deadzone) {
        if (Math.abs(value) < deadzone) {
            return 0.0;
        }
        // Normalize post-deadzone value and scale by 1.2x to ensure 
        // the analog stick can reach 1.0 (max speed) despite the circular physical restriction
        return Math.signum(value) * Math.min(1.0, ((Math.abs(value) - deadzone) / (1.0 - deadzone)) * 1.2);
    }

    public double getVerticalInput() { return verticalInput; }
    public double getYawInput() { return yawInput; }
    public double getForwardInput() { return forwardInput; }
    public double getLateralInput() { return lateralInput; }
    public boolean isArmPressed() { return armPressed; }
    public boolean isHomePressed() { return homePressed; }
    public boolean isLandPressed() { return landPressed; }
    public boolean isEmergencyPressed() { return emergencyPressed; }
    public boolean isPauseJustPressed() { return pausePressed && !pauseWasPressed; }
    public boolean isResetJustPressed() { return resetPressed && !resetWasPressed; }
    public double getZoomInput() { return zoomInput; }
    
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
