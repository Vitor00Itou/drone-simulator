package fr.enac.drone.model;

/**
 * Manages the physical state, coordinates, and movement logic of the drone.
 * Uses trigonometry to calculate directional movement relative to the current yaw.
 */
public class DroneModel {
    private double x = 0;
    private double y = -30; // Initial altitude to avoid spawning inside the ground plane
    private double z = 0;
    private double yaw = 0; // Rotation angle in degrees

    // Vertical and Rotational movements (Left Analog Stick)
    public void throttleUp() { this.y -= 5; }
    public void throttleDown() { this.y += 5; }
    public void yawLeft() { this.yaw -= 5; }
    public void yawRight() { this.yaw += 5; }

    // Directional movements (Right Analog Stick)
    // Uses trigonometry to align movement with the current heading (yaw)
    public void pitchForward() {
        this.x += 5 * Math.sin(Math.toRadians(yaw));
        this.z += 5 * Math.cos(Math.toRadians(yaw));
    }

    public void pitchBackward() {
        this.x -= 5 * Math.sin(Math.toRadians(yaw));
        this.z -= 5 * Math.cos(Math.toRadians(yaw));
    }

    public void rollLeft() {
        this.x -= 5 * Math.cos(Math.toRadians(yaw));
        this.z += 5 * Math.sin(Math.toRadians(yaw));
    }

    public void rollRight() {
        this.x += 5 * Math.cos(Math.toRadians(yaw));
        this.z -= 5 * Math.sin(Math.toRadians(yaw));
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public double getYaw() { return yaw; }
}