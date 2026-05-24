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
    private double throttle = 0;
    private double yaw = 0;
    private double pitch = 0;
    private double roll = 0;
    private boolean armPressed = false;
    private boolean disarmPressed = false;
    private boolean homePressed = false;

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

        if (state.isConnected) {
            // Mode 2 - Add 10% (0.1) deadzone for loose axes
            throttle = applyDeadzone(state.leftStickY, 0.1);
            yaw      = applyDeadzone(state.leftStickX, 0.1);
            pitch    = applyDeadzone(-state.rightStickY, 0.1); // Inverted for correct Forward/Backward mapping
            roll     = applyDeadzone(state.rightStickX, 0.1);
            
            armPressed = state.a || state.start;
            disarmPressed = state.b || state.back;
            homePressed = state.guide;
        } else {
            throttle = yaw = pitch = roll = 0.0;
            armPressed = disarmPressed = homePressed = false;
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

    public double getThrottle() { return throttle; }
    public double getYaw() { return yaw; }
    public double getPitch() { return pitch; }
    public double getRoll() { return roll; }
    public boolean isArmPressed() { return armPressed; }
    public boolean isDisarmPressed() { return disarmPressed; }
    public boolean isHomePressed() { return homePressed; }
}