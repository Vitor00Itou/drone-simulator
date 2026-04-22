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

    // Vertical and Rotational movements (Left Analog Stick)
    public void throttleUp() {
        double previousY = y;
        this.y -= 5;
        updateSpeed(x, previousY, z);
    }

    public void throttleDown() {
        double previousY = y;
        this.y += 5;
        updateSpeed(x, previousY, z);
    }

    public void yawLeft() { this.yaw -= 5; }
    public void yawRight() { this.yaw += 5; }

    // Directional movements (Right Analog Stick)
    // Uses trigonometry to align movement with the current heading (yaw)
    public void pitchForward() {
        double previousX = x;
        double previousZ = z;
        this.x += 5 * Math.sin(Math.toRadians(yaw));
        this.z += 5 * Math.cos(Math.toRadians(yaw));
        updateSpeed(previousX, y, previousZ);
    }

    public void pitchBackward() {
        double previousX = x;
        double previousZ = z;
        this.x -= 5 * Math.sin(Math.toRadians(yaw));
        this.z -= 5 * Math.cos(Math.toRadians(yaw));
        updateSpeed(previousX, y, previousZ);
    }

    public void rollLeft() {
        double previousX = x;
        double previousZ = z;
        this.x -= 5 * Math.cos(Math.toRadians(yaw));
        this.z += 5 * Math.sin(Math.toRadians(yaw));
        updateSpeed(previousX, y, previousZ);
    }

    public void rollRight() {
        double previousX = x;
        double previousZ = z;
        this.x += 5 * Math.cos(Math.toRadians(yaw));
        this.z -= 5 * Math.sin(Math.toRadians(yaw));
        updateSpeed(previousX, y, previousZ);
    }

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
