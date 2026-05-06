package fr.enac.drone.model;

import fr.enac.drone.utils.MathUtils;

/**
 * Manages the physical state, coordinates, and movement logic of the drone.
 * Uses trigonometry to calculate directional movement relative to the current yaw.
 */
public class DroneModel {
    private static final double METERS_PER_UNIT = 1.0;

    private final World world = World.createDefault();

    private double x = 0;
    private double y = -30; // Initial altitude to avoid spawning inside the ground plane
    private double z = 0;
    private double yaw = 0; // Rotation angle in degrees

    private final double homeX = x;
    private final double homeZ = z;
    private double currentHorizontalSpeedMs = 0.0; // Current horizontal speed in meters per second
    private double currentVerticalSpeedMs = 0.0; // Current vertical speed in meters per second

    // Real-world physical constants
    private static final double MAX_HORIZONTAL_SPEED = 25.0; // m/s
    private static final double MAX_VERTICAL_SPEED = 8.0;    // m/s
    private static final double YAW_RATE = 120.0;            // degrees/s

    /**
     * Updates the physical state of the drone using Kinematic physics.
     */
    public void updatePhysics(double pitchInput, double rollInput, double throttleInput, double deltaTime) {
        double length = Math.sqrt(pitchInput * pitchInput + rollInput * rollInput);
        
        // Normalize vector if magnitude exceeds 1.0
        if (length > 1.0) {
            pitchInput /= length;
            rollInput /= length;
            length = 1.0;
        }

        // Set the current horizontal speed based on input magnitude (already clamped to max 1.0)
        this.currentHorizontalSpeedMs = length * MAX_HORIZONTAL_SPEED;
        
        // Set the current vertical speed based on throttle input
        this.currentVerticalSpeedMs = throttleInput == 0.0 ? 0.0 : -throttleInput * MAX_VERTICAL_SPEED;

        double radYaw = Math.toRadians(this.yaw);
        double moveZ = (pitchInput * Math.cos(radYaw)) - (rollInput * Math.sin(radYaw));
        double moveX = (pitchInput * Math.sin(radYaw)) + (rollInput * Math.cos(radYaw));

        // Apply spatial displacement
        this.x += moveX * MAX_HORIZONTAL_SPEED * deltaTime;
        this.z += moveZ * MAX_HORIZONTAL_SPEED * deltaTime;
        this.y += throttleInput * MAX_VERTICAL_SPEED * deltaTime;
    }

    // Yaw rotation also scaled by time
    public void yawLeft(double deltaTime) { this.yaw -= YAW_RATE * deltaTime; }
    public void yawRight(double deltaTime) { this.yaw += YAW_RATE * deltaTime; }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public double getYaw() { return yaw; }
    public World getWorld() { return world; }

    /**
     * Generates the current telemetry data for the HUD.
     */
    public DroneTelemetry getTelemetry() {
        // Altitude is just the Y axis inverted and scaled
        double altitudeMeters = -y * METERS_PER_UNIT;
        
        // Heading requires normalizing the raw yaw angle
        double headingDegrees = MathUtils.normalizeHeading(yaw);
        
        // Distance from Home requires calculating the vector between current X,Z and Home X,Z
        double distanceMeters = MathUtils.distance2D(homeX, homeZ, x, z) * METERS_PER_UNIT;
        
        // Speed is already known by the physics engine, no math required!
        double horizontalSpeedMs = this.currentHorizontalSpeedMs;
        double verticalSpeedMs = this.currentVerticalSpeedMs;

        return new DroneTelemetry(altitudeMeters, horizontalSpeedMs, verticalSpeedMs, headingDegrees, distanceMeters);
    }
}
