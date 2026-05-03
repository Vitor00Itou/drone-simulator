package fr.enac.drone.model;

/**
 * Manages the physical state, coordinates, and movement logic of the drone.
 * Uses trigonometry to calculate directional movement relative to the current yaw.
 */
public class DroneModel {
    private static final double METERS_PER_UNIT = 1.0;
    private static final long SPEED_TIMEOUT_NANOS = 500_000_000L;

    private double x = 0;
    private double y = -30; // Initial altitude to avoid spawning inside the ground plane
    private double z = 0;
    private double yaw = 0; // Rotation angle in degrees

    private final double homeX = x;
    private final double homeZ = z;
    private double speedKmh = 0;
    private long lastMovementNanos = 0;

    // Real-world physical constants
    private static final double MAX_HORIZONTAL_SPEED = 25.0; // m/s
    private static final double MAX_VERTICAL_SPEED = 8.0;    // m/s
    private static final double YAW_RATE = 120.0;            // degrees/s

    /**
     * Updates the physical state of the drone using Kinematic physics.
     */
    public void updatePhysics(double pitchInput, double rollInput, double throttleInput, double deltaTime) {
        // Store previous position for speed calculation
        double previousX = x;
        double previousY = y;
        double previousZ = z;

        // Vector normalization for diagonal movement
        double length = Math.sqrt(pitchInput * pitchInput + rollInput * rollInput);
        if (length > 1.0) {
            pitchInput /= length;
            rollInput /= length;
        }

        // 2D Rotation matrix for Yaw direction
        double radYaw = Math.toRadians(this.yaw);
        double moveZ = (pitchInput * Math.cos(radYaw)) - (rollInput * Math.sin(radYaw));
        double moveX = (pitchInput * Math.sin(radYaw)) + (rollInput * Math.cos(radYaw));

        // Apply velocities scaled by the exact time elapsed
        this.x += moveX * MAX_HORIZONTAL_SPEED * deltaTime;
        this.z += moveZ * MAX_HORIZONTAL_SPEED * deltaTime;
        this.y += throttleInput * MAX_VERTICAL_SPEED * deltaTime;

        // Update speed based on actual movement
        updateSpeed(previousX, previousY, previousZ);
    }

    // Yaw rotation also scaled by time
    public void yawLeft(double deltaTime) { this.yaw -= YAW_RATE * deltaTime; }
    public void yawRight(double deltaTime) { this.yaw += YAW_RATE * deltaTime; }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public double getYaw() { return yaw; }

    public DroneTelemetry getTelemetry() {
        double altitudeMeters = Math.max(0, -y * METERS_PER_UNIT);
        double headingDegrees = normalizeHeading(yaw);
        double distanceMeters = distance2D(homeX, homeZ, x, z) * METERS_PER_UNIT;
        double currentSpeedKmh = isSpeedRecent() ? speedKmh : 0;

        return new DroneTelemetry(altitudeMeters, currentSpeedKmh, headingDegrees, distanceMeters);
    }

    private void updateSpeed(double previousX, double previousY, double previousZ) {
        double distanceMeters = distance3D(previousX, previousY, previousZ, x, y, z) * METERS_PER_UNIT;
        long now = System.nanoTime();

        if (lastMovementNanos > 0) {
            double elapsedSeconds = (now - lastMovementNanos) / 1_000_000_000.0;
            speedKmh = elapsedSeconds > 0 ? (distanceMeters / elapsedSeconds) * 3.6 : 0;
        }

        lastMovementNanos = now;
    }

    private boolean isSpeedRecent() {
        return lastMovementNanos > 0 && System.nanoTime() - lastMovementNanos <= SPEED_TIMEOUT_NANOS;
    }

    private static double normalizeHeading(double angleDegrees) {
        double normalized = angleDegrees % 360;
        return normalized < 0 ? normalized + 360 : normalized;
    }

    private static double distance2D(double x1, double z1, double x2, double z2) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static double distance3D(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
