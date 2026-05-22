package fr.enac.drone.model.drone;

import fr.enac.drone.model.world.WorldCollision;

import fr.enac.drone.utils.MathUtils;

/**
 * Manages the physical state, coordinates, and movement logic of the drone.
 * The drone starts on the ground and must be armed (key O) before it can fly.
 * Battery drain depends on altitude, acceleration, and yaw activity.
 * On disarm (key F), motors cut and the drone falls under gravity until it hits the ground.
 */
public class DroneModel {

    private static final double METERS_PER_UNIT = 1.0;

    // Ground level 
    private static final double GROUND_Y = 0;

    // Drone physical dimensions used by rendering and collision detection
    private static final double DRONE_HEIGHT = 0.10;
    private static final double DRONE_RADIUS = 0.60;


    private double x = 0;
    private double y = GROUND_Y - DRONE_HEIGHT / 2.0;
    private double z = 0;

    private double yaw = 0;
    private double yawVelocity = 0.0;

    private final double homeX = x;
    private final double homeZ = z;

    // Velocity vectors (m/s)
    private double velocityX = 0.0;
    private double velocityY = 0.0;
    private double velocityZ = 0.0;

    // Armed state
    private boolean armed = false;

    // Falling state
    private boolean falling = false;

    // Collision response constants
    private static final double COLLISION_RESTITUTION = 0.35;
    private static final double COLLISION_FRICTION = 0.75;
    private static final double COLLISION_SKIN = 1.0;

    // Physics constants
    private static final double ACCELERATION_HORIZONTAL = 8.0;
    private static final double ACCELERATION_VERTICAL   = 8.0;

    private static final double DRAG_COEFFICIENT = 0.85;
    private static final double YAW_DRAG         = 0.0005;

    private static final double GRAVITY      = 9.81;
    private static final double HOVER_THRUST = 9.81;

    private static final double YAW_RATE         = 40.0;
    private static final double YAW_ACCELERATION = 120.0;

    private double yawSensitivity =
            DroneControlSettings.YAW_SENSITIVITY.getDefaultValue();
    private double maxHorizontalSpeed =
            DroneControlSettings.MAX_HORIZONTAL_SPEED.getDefaultValue();
    private double maxVerticalSpeed =
            DroneControlSettings.MAX_VERTICAL_SPEED.getDefaultValue();

    // ── Battery ─────────────────────────────────────────────────────────

    private static final double MAX_FLIGHT_TIME_SECONDS = 600.0;

    private double remainingFlightTimeSeconds =
            MAX_FLIGHT_TIME_SECONDS;

    private static final double DRAIN_GROUND_IDLE   = 0.05;
    private static final double DRAIN_HOVER_BASE    = 1.0;
    private static final double DRAIN_PER_METER_ALT = 0.002;
    private static final double DRAIN_ACCEL_H       = 0.04;
    private static final double DRAIN_THROTTLE_V    = 0.5;
    private static final double DRAIN_YAW           = 0.002;

    // ── Autopilot support ─────────────────────────────────────────────

    private double targetYawVelocity = 0.0;
    private boolean useTargetYaw = false;

    // ───────────────────────────────────────────────────────────────────

    /**
     * Arms the drone.
     */
    public void arm() {
        armed = true;
        falling = false;
    }

    /**
     * Disarms the drone.
     */
    public void disarm() {
        armed = false;
        falling = true;
        velocityX = 0;
        velocityZ = 0;
        yawVelocity = 0;
    }

    public boolean isArmed() {
        return armed;
    }

    public boolean isFalling() {
        return falling;
    }

    // ───────────────────────────────────────────────────────────────────

    public void updatePhysics(
            double pitchInput,
            double rollInput,
            double throttleInput,
            double deltaTime
    ) {
        // Falling physics
        if (falling) {
            velocityY += GRAVITY * deltaTime;
            this.y += velocityY * deltaTime;
            return;
        }

        // Block movement if disarmed
        if (!armed) {
            return;
        }

        // Normalize input vector
        double inputLength =
                Math.sqrt(
                        pitchInput * pitchInput
                                + rollInput * rollInput
                );
        if (inputLength > 1.0) {
            pitchInput /= inputLength;
            rollInput /= inputLength;
            inputLength = 1.0;
        }

        // Convert movement to world space
        double radYaw = Math.toRadians(this.yaw);

        double moveX =
                (pitchInput * Math.sin(radYaw))
                        + (rollInput * Math.cos(radYaw));
        double moveZ =
                (pitchInput * Math.cos(radYaw))
                        - (rollInput * Math.sin(radYaw));

        double targetVelocityX =
                moveX * maxHorizontalSpeed;
        double targetVelocityZ =
                moveZ * maxHorizontalSpeed;

        double accelX =
                (targetVelocityX - velocityX)
                        * ACCELERATION_HORIZONTAL
                        * deltaTime;
        double accelZ =
                (targetVelocityZ - velocityZ)
                        * ACCELERATION_HORIZONTAL
                        * deltaTime;

        velocityX += accelX;
        velocityZ += accelZ;

        velocityX *= Math.pow(DRAG_COEFFICIENT, deltaTime);
        velocityZ *= Math.pow(DRAG_COEFFICIENT, deltaTime);

        // Vertical movement
        double targetVelocityY =
                throttleInput * maxVerticalSpeed;

        double accelY =
                (targetVelocityY - velocityY)
                        * ACCELERATION_VERTICAL
                        * deltaTime;

        velocityY += accelY;
        velocityY += (HOVER_THRUST - GRAVITY) * deltaTime;
        velocityY *= Math.pow(DRAG_COEFFICIENT, deltaTime);

        // Apply movement
        this.x += velocityX * deltaTime;
        this.z += velocityZ * deltaTime;
        this.y += velocityY * deltaTime;

        // ── Battery drain ─────────────────────────────────────────────
        double altitudeMeters =
                Math.max(
                        0.0,
                        -(y - GROUND_Y) * METERS_PER_UNIT
                );

        boolean onGround =
                (this.y >= GROUND_Y - 0.5);

        double drainRate;

        if (onGround
                && throttleInput <= 0
                && inputLength == 0) {
            drainRate = DRAIN_GROUND_IDLE;
        } else {
            drainRate = DRAIN_HOVER_BASE;
            drainRate += altitudeMeters * DRAIN_PER_METER_ALT;

            double hAccelMag =
                    Math.sqrt(accelX * accelX + accelZ * accelZ)
                            / deltaTime;
            drainRate += hAccelMag * DRAIN_ACCEL_H;
            drainRate += Math.abs(throttleInput) * DRAIN_THROTTLE_V;
            drainRate += Math.abs(yawVelocity) * DRAIN_YAW;
        }

        remainingFlightTimeSeconds -=
                drainRate * deltaTime;

        if (remainingFlightTimeSeconds <= 0.0) {
            remainingFlightTimeSeconds = 0.0;
            disarm();
        }
    }

    public void yawLeft(double deltaTime) {
        if (!armed) return;

        double targetYawVelocity = -YAW_RATE * yawSensitivity;
        yawVelocity +=
                (targetYawVelocity - yawVelocity)
                        * YAW_ACCELERATION
                        * deltaTime;
        yaw += yawVelocity * deltaTime;
    }

    public void yawRight(double deltaTime) {
        if (!armed) return;

        double targetYawVelocity = YAW_RATE * yawSensitivity;
        yawVelocity +=
                (targetYawVelocity - yawVelocity)
                        * YAW_ACCELERATION
                        * deltaTime;
        yaw += yawVelocity * deltaTime;
    }

    public void updateYaw(double deltaTime) {
        if (!armed) return;

        if (useTargetYaw) {
            yawVelocity += (targetYawVelocity - yawVelocity) * YAW_ACCELERATION * deltaTime;
        } else {
            yawVelocity *= Math.pow(YAW_DRAG, deltaTime);
        }
        yaw += yawVelocity * deltaTime;
    }

    // Autopilot API
    public void setTargetYawVelocity(double rateDegPerSec) {
        this.targetYawVelocity = rateDegPerSec;
        this.useTargetYaw = true;
    }

    public void clearTargetYaw() {
        this.useTargetYaw = false;
    }

    // ── Getters ───────────────────────────────────────────────────────

    public double getDroneRadius() { return DRONE_RADIUS; }
    public double getDroneHeight() { return DRONE_HEIGHT; }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public double getYaw() { return yaw; }
    public double getYawSensitivity() { return yawSensitivity; }
    public double getMaxHorizontalSpeed() { return maxHorizontalSpeed; }
    public double getMaxVerticalSpeed() { return maxVerticalSpeed; }

    public void setYawSensitivity(double yawSensitivity) {
        this.yawSensitivity =
                DroneControlSettings.YAW_SENSITIVITY.clamp(yawSensitivity);
    }

    public void setMaxHorizontalSpeed(double maxHorizontalSpeed) {
        this.maxHorizontalSpeed =
                DroneControlSettings.MAX_HORIZONTAL_SPEED.clamp(maxHorizontalSpeed);
    }

    public void setMaxVerticalSpeed(double maxVerticalSpeed) {
        this.maxVerticalSpeed =
                DroneControlSettings.MAX_VERTICAL_SPEED.clamp(maxVerticalSpeed);
    }

    public double getBatteryPercentage() {
        return (remainingFlightTimeSeconds
                / MAX_FLIGHT_TIME_SECONDS)
                * 100.0;
    }

    /**
     * Save current drone state.
     */
    public DroneState saveState() {
        return new DroneState(
                x, y, z, yaw,
                velocityX, velocityY, velocityZ,
                yawVelocity
        );
    }

    /**
     * Set drone spawn position.
     */
    public void setSpawnPosition(
            double x, double y, double z, double yaw
    ) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;

        // Reset velocities
        this.velocityX = 0;
        this.velocityY = 0;
        this.velocityZ = 0;
        this.yawVelocity = 0;

        // Reset battery
        remainingFlightTimeSeconds = MAX_FLIGHT_TIME_SECONDS;

        // Reset autopilot state
        clearTargetYaw();
    }

    /**
     * Generate telemetry snapshot.
     */
    public DroneTelemetry getTelemetry() {
        double altitudeMeters =
                Math.max(
                        0.0,
                        -(y - GROUND_Y) * METERS_PER_UNIT
                );
        double headingDegrees = MathUtils.normalizeHeading(yaw);
        double distanceMeters =
                MathUtils.distance2D(homeX, homeZ, x, z)
                        * METERS_PER_UNIT;
        double horizontalSpeedMs =
                Math.sqrt(
                        velocityX * velocityX
                                + velocityZ * velocityZ
                );
        double verticalSpeedMs = -velocityY;

        return new DroneTelemetry(
                altitudeMeters,
                horizontalSpeedMs,
                verticalSpeedMs,
                headingDegrees,
                distanceMeters,
                getBatteryPercentage(),
                armed
        );
    }

    /**
    * Resolves either a ground or horizontal collision using collision data.
    */
    public void resolveCollision(WorldCollision collision) {
        if (collision.isVerticalCollision()) {
                resolveVerticalCollision(collision);
                return;
        }

        resolveHorizontalCollision(
                collision.getNormalX(),
                collision.getNormalZ(),
                collision.getPenetration()
        );
    }

    /**
    * Pushes the drone out of a collided object and reflects its horizontal velocity.
    * This preserves inertia while creating a bounce effect.
    */
    public void resolveHorizontalCollision(double normalX, double normalZ, double penetration) {
        double length = Math.sqrt(normalX * normalX + normalZ * normalZ);

        if (length < 0.0001) {
                return;
        }

        normalX /= length;
        normalZ /= length;

        // Move the drone slightly outside the obstacle to avoid sticking
        double correction = penetration + COLLISION_SKIN;
        this.x += normalX * correction;
        this.z += normalZ * correction;

        // Decompose velocity into normal and tangential components
        double normalVelocity = velocityX * normalX + velocityZ * normalZ;

        double tangentVelocityX = velocityX - normalVelocity * normalX;
        double tangentVelocityZ = velocityZ - normalVelocity * normalZ;

        // Reflect only if the drone is moving into the obstacle
        if (normalVelocity < 0) {
                double bouncedNormalVelocity = -normalVelocity * COLLISION_RESTITUTION;

                velocityX = tangentVelocityX * COLLISION_FRICTION + bouncedNormalVelocity * normalX;
                velocityZ = tangentVelocityZ * COLLISION_FRICTION + bouncedNormalVelocity * normalZ;
        } else {
                velocityX *= COLLISION_FRICTION;
                velocityZ *= COLLISION_FRICTION;
        }
    }

    /**
    * Resolves vertical collisions without affecting horizontal inertia.
    */
    private void resolveVerticalCollision(WorldCollision collision) {
        double correction = collision.getPenetration() + COLLISION_SKIN;

        this.y += collision.getNormalY() * correction;

        // Stop downward inertia when landing on a surface
        if (collision.getNormalY() < 0 && velocityY > 0) {
                velocityY = 0;
                falling = false;
        }

        // Stop upward inertia when hitting the bottom of an object
        if (collision.getNormalY() > 0 && velocityY < 0) {
                velocityY = 0;
        }
    }
}
