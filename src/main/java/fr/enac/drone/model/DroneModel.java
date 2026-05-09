package fr.enac.drone.model;

import fr.enac.drone.utils.MathUtils;

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
    private double yawVelocity = 0.0; // Angular velocity (degrees/s)

    private final double homeX = x;
    private final double homeZ = z;
    
    // Velocity vectors (m/s)
    private double velocityX = 0.0;
    private double velocityY = 0.0; // Positive = up, Negative = down
    private double velocityZ = 0.0;
    
    // Physics constants
    private static final double ACCELERATION_HORIZONTAL = 8.0; // m/s² (smoother acceleration)
    private static final double ACCELERATION_VERTICAL = 8.0;   // m/s² (smoother acceleration)
    private static final double DRAG_COEFFICIENT = 0.85;       // Air resistance (moderate drag)
    private static final double YAW_DRAG = 0.0005;              // Yaw drag (much faster stop rotation)
    private static final double GRAVITY = 9.81;                 // m/s²
    private static final double HOVER_THRUST = 9.81;            // Thrust to counteract gravity
    
    // Speed limits
    private static final double MAX_HORIZONTAL_SPEED = 25.0; // m/s
    private static final double MAX_VERTICAL_SPEED = 8.0;    // m/s
    private static final double YAW_RATE = 40.0;             // degrees/s 
    private static final double YAW_ACCELERATION = 120.0;    // degrees/s²
    /**
     * Updates the physical state of the drone using physics with inertia.
     */
    public void updatePhysics(double pitchInput, double rollInput, double throttleInput, double deltaTime) {
        // Normalize input vector
        double inputLength = Math.sqrt(pitchInput * pitchInput + rollInput * rollInput);
        if (inputLength > 1.0) {
            pitchInput /= inputLength;
            rollInput /= inputLength;
            inputLength = 1.0;
        }

        // Calculate movement direction based on yaw and inputs
        double radYaw = Math.toRadians(this.yaw);
        double moveX = (pitchInput * Math.sin(radYaw)) + (rollInput * Math.cos(radYaw));
        double moveZ = (pitchInput * Math.cos(radYaw)) - (rollInput * Math.sin(radYaw));

        // Calculate target velocities based on input direction
        double targetVelocityX = moveX * MAX_HORIZONTAL_SPEED;
        double targetVelocityZ = moveZ * MAX_HORIZONTAL_SPEED;

        // Apply acceleration towards target velocities (horizontal)
        double accelX = (targetVelocityX - velocityX) * ACCELERATION_HORIZONTAL * deltaTime;
        double accelZ = (targetVelocityZ - velocityZ) * ACCELERATION_HORIZONTAL * deltaTime;
        
        velocityX += accelX;
        velocityZ += accelZ;

        // Apply drag (air resistance)
        velocityX *= Math.pow(DRAG_COEFFICIENT, deltaTime);
        velocityZ *= Math.pow(DRAG_COEFFICIENT, deltaTime);

        // Vertical movement with gravity and thrust
        // Calculate target velocity based on throttle input
        double targetVelocityY = throttleInput * MAX_VERTICAL_SPEED;
        
        // Apply acceleration towards target velocity (like horizontal)
        double accelY = (targetVelocityY - velocityY) * ACCELERATION_VERTICAL * deltaTime;
        velocityY += accelY;
        
        // Apply net gravity (hover thrust cancels gravity when throttle = 0)
        velocityY += (HOVER_THRUST - GRAVITY) * deltaTime;
        
        // Apply drag (air resistance)
        velocityY *= Math.pow(DRAG_COEFFICIENT, deltaTime);

        // Update position based on velocity
        this.x += velocityX * deltaTime;
        this.z += velocityZ * deltaTime;
        this.y += velocityY * deltaTime;

    }

    // Yaw rotation with angular inertia (smooth acceleration and deceleration)
    public void yawLeft(double deltaTime) { 
        double targetYawVelocity = -YAW_RATE;
        yawVelocity += (targetYawVelocity - yawVelocity) * YAW_ACCELERATION * deltaTime;
        yaw += yawVelocity * deltaTime;
    }
    
    public void yawRight(double deltaTime) { 
        double targetYawVelocity = YAW_RATE;
        yawVelocity += (targetYawVelocity - yawVelocity) * YAW_ACCELERATION * deltaTime;
        yaw += yawVelocity * deltaTime;
    }
    
    public void updateYaw(double deltaTime) {
        // When no yaw input, return to zero velocity (stabilize)
        // Apply stronger drag to yaw velocity to stop rotation faster
        yawVelocity *= Math.pow(YAW_DRAG, deltaTime);
        yaw += yawVelocity * deltaTime;
    }

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
        
        // Calculate actual speeds from velocity vectors
        double horizontalSpeedMs = Math.sqrt(velocityX * velocityX + velocityZ * velocityZ);
        double verticalSpeedMs = -velocityY; // Invert sign: positive = up, negative = down

        return new DroneTelemetry(altitudeMeters, horizontalSpeedMs, verticalSpeedMs, headingDegrees, distanceMeters);
    }

    /**
     * Save the current drone state to a WorldState object.
     */
    public fr.enac.drone.model.world.WorldState saveState() {
        return new fr.enac.drone.model.world.WorldState(x, y, z, yaw, velocityX, velocityY, velocityZ, yawVelocity);
    }

    /**
     * Sets the drone's initial spawn position.
     */
    public void setSpawnPosition(double x, double y, double z, double yaw) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        // Reset velocities when spawning
        this.velocityX = 0;
        this.velocityY = 0;
        this.velocityZ = 0;
        this.yawVelocity = 0;
    }
}
