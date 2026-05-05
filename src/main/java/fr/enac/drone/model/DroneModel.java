package fr.enac.drone.model;

import fr.enac.drone.utils.MathUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Manages the physical state, coordinates, and movement logic of the drone.
 * Uses trigonometry to calculate directional movement relative to the current yaw.
 */
public class DroneModel {
    private static final double METERS_PER_UNIT = 1.0;

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

    private final List<Obstacle> obstacles = new ArrayList<>();
    private WorldMode currentWorldMode = WorldMode.FEW_OBSTACLES;

    private static final double WORLD_LIMIT = 2200;
    private static final double MIN_OBSTACLE_HEIGHT = 80;
    private static final double MAX_OBSTACLE_HEIGHT = 250;
    private static final double MIN_OBSTACLE_RADIUS = 15;
    private static final double MAX_OBSTACLE_RADIUS = 27.5; // diâmetro máximo 55

    /**
    * Initializes the model by generating obstacles based on the default world mode.
    */
    public DroneModel() {
        generateObstacles(currentWorldMode);
    }

    /**
    * Updates the world configuration and regenerates obstacles accordingly.
    */
    public void setWorldMode(WorldMode worldMode) {
        this.currentWorldMode = worldMode;
        generateObstacles(worldMode);
    }

    /**
    * Returns the currently active world mode.
    */
    public WorldMode getCurrentWorldMode() {
        return currentWorldMode;
    }

    /**
    * Provides a read-only view of the obstacles in the world.
    */
    public List<Obstacle> getObstacles() {
        return Collections.unmodifiableList(obstacles);
    }

    /**
    * Generates obstacles randomly distributed in the world based on the selected mode.
    * Each obstacle has randomized position, height, and radius within defined limits.
    * Obstacles are prevented from spawning too close to the origin (drone start position).
    */
    private void generateObstacles(WorldMode worldMode) {
        // Clear previously generated obstacles before creating a new set
        obstacles.clear();

        Random random = new Random();

        for (int i = 0; i < worldMode.getObstacleCount(); i++) {
            double x;
            double z;
            do {
                x = -WORLD_LIMIT + random.nextDouble() * (2 * WORLD_LIMIT);
                z = -WORLD_LIMIT + random.nextDouble() * (2 * WORLD_LIMIT);
            } while (Math.sqrt(x * x + z * z) < 250);

            double height = MIN_OBSTACLE_HEIGHT + random.nextDouble() * (MAX_OBSTACLE_HEIGHT - MIN_OBSTACLE_HEIGHT);

            double radius = MIN_OBSTACLE_RADIUS + random.nextDouble() * (MAX_OBSTACLE_RADIUS - MIN_OBSTACLE_RADIUS);

            obstacles.add(new Obstacle(x, z, radius, height));
        }
    }

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
