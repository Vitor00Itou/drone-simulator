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

    // Drone physical dimensions used by rendering and collision detection
    private static final double DRONE_HEIGHT = 0.10;
    private static final double DRONE_RADIUS = 0.60;

    private double x = 0;
    private double y = 0.0;
    private double z = 0;

    private double yaw = 0;
    private double yawVelocity = 0.0;

    private double homeX = 0;
    private double homeZ = 0;

    // Velocity vectors (m/s)
    private double velocityX = 0.0;
    private double velocityY = 0.0;
    private double velocityZ = 0.0;

    // Flight State
    private FlightState state = FlightState.DISARMED;
    private double stateTimer = 0.0;
    private static final double TAKEOFF_ALTITUDE = 2.0;
    private double targetTakeoffY = 0.0;
    private double rthSafeY = 0.0;
    private ReturnToHomePhase rthPhase = ReturnToHomePhase.ASCENDING;

    // Collision response constants
    private static final double COLLISION_RESTITUTION = 0.35;
    private static final double COLLISION_FRICTION = 0.75;
    private static final double COLLISION_SKIN = 0.01;

    // Physics constants
    private static final double ACCELERATION_HORIZONTAL = 8.0;
    private static final double ACCELERATION_VERTICAL   = 8.0;

    private static final double DRAG_COEFFICIENT = 0.85;
    private static final double YAW_DRAG         = 0.0005;

    private static final double GRAVITY      = 9.81;
    private static final double HOVER_THRUST = 9.81;

    private static final double YAW_RATE         = 40.0;
    private static final double YAW_ACCELERATION = 15.0; 
    private double yawSensitivity =
            DroneControlSettings.YAW_SENSITIVITY.getDefaultValue();
    private double maxHorizontalSpeed =
            DroneControlSettings.MAX_HORIZONTAL_SPEED.getDefaultValue();
    private double maxVerticalSpeed =
            DroneControlSettings.MAX_VERTICAL_SPEED.getDefaultValue();

    // Battery model constants.

    private static final double MAX_FLIGHT_TIME_SECONDS = 600.0;

    private double remainingFlightTimeSeconds =
            MAX_FLIGHT_TIME_SECONDS;

    private static final double DRAIN_GROUND_IDLE   = 0.05;
    private static final double DRAIN_HOVER_BASE    = 1.0;
    private static final double DRAIN_PER_METER_ALT = 0.002;
    private static final double DRAIN_ACCEL_H       = 0.04;
    private static final double DRAIN_VERTICAL_INPUT = 0.5;
    private static final double DRAIN_YAW           = 0.002;

    // Autopilot yaw support.

    private double targetYawVelocity = 0.0;
    private boolean useTargetYaw = false;

    /**
     * Creates a drone model with default position, velocity, battery, and flight settings.
     */
    public DroneModel() {
    }

    /**
     * Starts automatic takeoff, or recovers from a falling state when possible.
     */
    public void takeoff() {
        if (state == FlightState.DISARMED) {
            state = FlightState.TAKING_OFF;
            stateTimer = 0.0;
            targetTakeoffY = this.y - (TAKEOFF_ALTITUDE / METERS_PER_UNIT);
        } else if (state == FlightState.FALLING) {
            state = FlightState.FLYING;
            stateTimer = 0.0; // Trigger mid-air spool up timer
        }
    }

    /**
     * Starts the return-to-home sequence at a safe altitude.
     *
     * @param safeY target safe Y coordinate, where smaller values are higher in JavaFX space
     */
    public void returnToHome(double safeY) {
        if (isArmed() && state != FlightState.FALLING) {
            state = FlightState.RETURNING_HOME;
            // Ensure we don't descend if we are already higher than the safe altitude
            this.rthSafeY = Math.min(safeY, this.y); 
            this.rthPhase = ReturnToHomePhase.ASCENDING;
            this.stateTimer = 0.0;
            clearTargetYaw();
        }
    }

    /**
     * Starts automatic landing from any controlled flight state.
     */
    public void land() {
        if (state == FlightState.FLYING || state == FlightState.TAKING_OFF || state == FlightState.RETURNING_HOME) {
            state = FlightState.LANDING;
            stateTimer = 0.0;
        }
    }

    /**
     * Cuts the motors and makes the drone fall immediately.
     */
    public void disarm() {
        state = FlightState.FALLING;
        stateTimer = 0.0;
        velocityX = 0;
        velocityZ = 0;
        yawVelocity = 0;
    }
    
    /**
     * Arms the drone by delegating to takeoff.
     */
    public void arm() {
        takeoff();
    }

    /**
     * Returns whether the drone is in a state that accepts flight control.
     *
     * @return {@code true} when the drone is taking off, flying, returning home, or landing
     */
    public boolean isArmed() {
        return state == FlightState.FLYING || state == FlightState.TAKING_OFF || state == FlightState.LANDING || state == FlightState.RETURNING_HOME;
    }

    /**
     * Returns whether the drone is currently falling after an emergency stop.
     *
     * @return {@code true} when the drone is falling
     */
    public boolean isFalling() {
        return state == FlightState.FALLING;
    }

    /**
     * Returns the current flight state.
     *
     * @return current flight state
     */
    public FlightState getFlightState() {
        return state;
    }

    /**
     * Computes the current motor spool factor used to soften takeoff and mid-air recovery.
     *
     * @return spool factor in the range {@code [0, 1]}
     */
    private double getSpoolFactor() {
        if (!isArmed()) return 0.0;
        if (state == FlightState.TAKING_OFF) {
            return Math.min(1.0, stateTimer / 1.0); // 1 sec spool up on ground
        }
        if (state == FlightState.FLYING && stateTimer < 0.4) {
            return stateTimer / 0.4; // 400ms spool up for mid-air recovery
        }
        return 1.0;
    }

    /**
     * Advances physics using normalized pilot inputs and the current ground estimate.
     *
     * @param forwardInput normalized forward/backward input
     * @param lateralInput normalized left/right input
     * @param verticalInput normalized climb/descent input
     * @param groundYBelow closest known ground or obstacle surface below the drone
     * @param deltaTime elapsed simulation time in seconds
     */
    public void updatePhysics(
            double forwardInput,
            double lateralInput,
            double verticalInput,
            double groundYBelow,
            double deltaTime
    ) {
        // Falling physics
        if (state == FlightState.FALLING) {
            velocityY += GRAVITY * deltaTime;
            this.y += velocityY * deltaTime;
            return;
        }

        // Block movement if disarmed
        if (!isArmed()) {
            return;
        }

        stateTimer += deltaTime;
        double spoolFactor = getSpoolFactor();

        // Auto states override user input for controlled maneuvers
        if (state == FlightState.TAKING_OFF) {
            forwardInput = 0;
            lateralInput = 0;
            
            if (this.y <= targetTakeoffY) {
                state = FlightState.FLYING;
                stateTimer = 0.4; // Bypass mid-air spool up after a normal takeoff
                verticalInput = 0; // Transition to hover
            } else {
                if (stateTimer < 1.0) {
                    // Phase 1: Spool up delay (Wait 1 second before lifting)
                    verticalInput = 0.0;
                } else {
                    // Phase 2: Smoothly ramp up upward vertical input to a max of 50% (-0.5)
                    verticalInput = Math.max(-0.5, -(stateTimer - 1.0) * 0.5);
                }
            }
        } else if (state == FlightState.RETURNING_HOME) {
            if (Math.abs(forwardInput) > 0.01 || Math.abs(lateralInput) > 0.01 || Math.abs(verticalInput) > 0.01) {
                state = FlightState.FLYING; // Cancel RTH on any user input
                clearTargetYaw();
            } else {
                forwardInput = 0;
                lateralInput = 0;
                verticalInput = 0;
                
                double dx = homeX - this.x;
                double dz = homeZ - this.z;
                double dist = Math.sqrt(dx * dx + dz * dz);

                if (dist > 0.5) {
                    double targetYawDeg = Math.toDegrees(Math.atan2(dx, dz));
                    double angleError = targetYawDeg - this.yaw;
                    while (angleError > 180) angleError -= 360;
                    while (angleError <= -180) angleError += 360;
                    double yawCmd = Math.max(-1.0, Math.min(1.0, angleError / 45.0)); // Smooth turn
                    setTargetYawVelocity(yawCmd * getYawRateDegreesPerSecond());
                } else {
                    clearTargetYaw();
                }

                if (rthPhase == ReturnToHomePhase.ASCENDING) {
                    // Phase 0: Ascend directly upward to the safe Y coordinate
                    if (this.y > rthSafeY) {
                        verticalInput = -1.0; // Ascend full speed
                    } else {
                        rthPhase = ReturnToHomePhase.CRUISING; // Start cruising
                    }
                } else if (rthPhase == ReturnToHomePhase.CRUISING) {
                    // Phase 1: Maintain safe altitude while flying directly to (homeX, homeZ)
                    if (this.y > rthSafeY + 1.0) verticalInput = -0.5;
                    else if (this.y < rthSafeY - 1.0) verticalInput = 0.5;

                    if (dist < 1.0) { // Tolerance of 1 meter
                        clearTargetYaw();
                        land(); // Arrived at home position, transition seamlessly to LANDING
                    } else {
                        double dirX = dx / dist;
                        double dirZ = dz / dist;
                        double radYaw = Math.toRadians(this.yaw);
                        
                        // Slow down gracefully if we are within 10 meters of the base
                        double speedFactor = Math.min(1.0, dist / 10.0);
                        
                        // Inverse kinematics to figure out what stick inputs we need to go to dirX/dirZ
                        forwardInput = (dirX * Math.sin(radYaw) + dirZ * Math.cos(radYaw)) * speedFactor;
                        lateralInput = (dirX * Math.cos(radYaw) - dirZ * Math.sin(radYaw)) * speedFactor;
                    }
                }
            }
        } else if (state == FlightState.LANDING) {
            if (Math.abs(forwardInput) > 0.01 || Math.abs(lateralInput) > 0.01 || Math.abs(verticalInput) > 0.01) {
                state = FlightState.FLYING; // Cancel auto-landing on user input
            } else {
                forwardInput = 0;
                lateralInput = 0;

                // Active braking to prevent horizontal drifting during landing
                velocityX -= velocityX * 4.0 * deltaTime;
                velocityZ -= velocityZ * 4.0 * deltaTime;

                // Determine desired descent speed in m/s
                double targetDescentSpeed;
                
                // The drone now acts like it has a downward-facing proximity sensor
                double distToGround = groundYBelow - this.y;
                
                // Calculate a safe braking altitude to begin slowing down (min 15m, scales with speed)
                double brakingDistance = Math.max(15.0, maxVerticalSpeed * 1.5);

                if (distToGround > brakingDistance) {
                    targetDescentSpeed = maxVerticalSpeed; // Fast plunge when high up!
                } else if (distToGround > 1.5) {
                    // Smoothly interpolate from max vertical speed to 0.5 based on remaining distance
                    double fraction = (distToGround - 1.5) / (brakingDistance - 1.5);
                    targetDescentSpeed = 0.5 + ((maxVerticalSpeed - 0.5) * fraction);
                } else {
                    targetDescentSpeed = 0.5; // Very slow and gentle touchdown
                }

                // Calculate the required vertical input to achieve the exact target descent speed
                verticalInput = targetDescentSpeed / maxVerticalSpeed;
            }
        }

        // Normalize input vector
        double inputLength =
                Math.sqrt(
                        forwardInput * forwardInput
                                + lateralInput * lateralInput
                );
        if (inputLength > 1.0) {
            forwardInput /= inputLength;
            lateralInput /= inputLength;
            inputLength = 1.0;
        }

        // Convert movement to world space
        double radYaw = Math.toRadians(this.yaw);

        double moveX =
                (forwardInput * Math.sin(radYaw))
                        + (lateralInput * Math.cos(radYaw));
        double moveZ =
                (forwardInput * Math.cos(radYaw))
                        - (lateralInput * Math.sin(radYaw));

        double targetVelocityX =
                moveX * maxHorizontalSpeed;
        double targetVelocityZ =
                moveZ * maxHorizontalSpeed;

        double accelX =
                (targetVelocityX - velocityX)
                        * ACCELERATION_HORIZONTAL
                        * spoolFactor
                        * deltaTime;
        double accelZ =
                (targetVelocityZ - velocityZ)
                        * ACCELERATION_HORIZONTAL
                        * spoolFactor
                        * deltaTime;

        velocityX += accelX;
        velocityZ += accelZ;

        velocityX *= Math.pow(DRAG_COEFFICIENT, deltaTime);
        velocityZ *= Math.pow(DRAG_COEFFICIENT, deltaTime);

        // Vertical movement
        double targetVelocityY =
                verticalInput * maxVerticalSpeed;

        double accelY =
                (targetVelocityY - velocityY)
                        * ACCELERATION_VERTICAL
                        * spoolFactor
                        * deltaTime;

        velocityY += accelY;
        velocityY += (GRAVITY - HOVER_THRUST * spoolFactor) * deltaTime;
        velocityY *= Math.pow(DRAG_COEFFICIENT, deltaTime);

        // Apply movement
        this.x += velocityX * deltaTime;
        this.z += velocityZ * deltaTime;
        this.y += velocityY * deltaTime;

        // Battery drain.
        double altitudeMeters =
                Math.max(
                        0.0,
                        -y * METERS_PER_UNIT
                );

        double drainRate;

        if (state == FlightState.DISARMED || state == FlightState.FALLING) {
            drainRate = DRAIN_GROUND_IDLE;
        } else {
            drainRate = DRAIN_HOVER_BASE;
            drainRate += altitudeMeters * DRAIN_PER_METER_ALT;

            double hAccelMag =
                    Math.sqrt(accelX * accelX + accelZ * accelZ)
                            / deltaTime;
            drainRate += hAccelMag * DRAIN_ACCEL_H;
            drainRate += Math.abs(verticalInput) * DRAIN_VERTICAL_INPUT;
            drainRate += Math.abs(yawVelocity) * DRAIN_YAW;
        }

        remainingFlightTimeSeconds -=
                drainRate * deltaTime;

        if (remainingFlightTimeSeconds <= 0.0) {
            remainingFlightTimeSeconds = 0.0;
            disarm();
        }
    }

    /**
     * Applies a manual yaw command and cancels automatic yaw modes when needed.
     *
     * @param input normalized yaw input in the range {@code [-1, 1]}
     */
    public void setManualYawInput(double input) {
        if (!isArmed()) return;
        if (state == FlightState.LANDING || state == FlightState.RETURNING_HOME) {
            state = FlightState.FLYING; // Cancel auto modes
            clearTargetYaw();
        }
        setTargetYawVelocity(input * getYawRateDegreesPerSecond());
    }

    /**
     * Advances yaw velocity and heading for the current simulation tick.
     *
     * @param deltaTime elapsed simulation time in seconds
     */
    public void updateYaw(double deltaTime) {
        if (!isArmed()) return;

        if (useTargetYaw) {
            yawVelocity += (targetYawVelocity - yawVelocity) * YAW_ACCELERATION * getSpoolFactor() * deltaTime;
        } else {
            yawVelocity *= Math.pow(YAW_DRAG, deltaTime);
        }
        yaw += yawVelocity * deltaTime;
    }

    /**
     * Sets the target yaw velocity used by autopilot or smoothed manual yaw.
     *
     * @param rateDegPerSec desired yaw rate in degrees per second
     */
    public void setTargetYawVelocity(double rateDegPerSec) {
        this.targetYawVelocity = rateDegPerSec;
        this.useTargetYaw = true;
    }

    /**
     * Disables target yaw velocity and lets yaw inertia decay.
     */
    public void clearTargetYaw() {
        this.useTargetYaw = false;
    }

    /**
     * Restores user-adjustable flight settings to their defaults.
     */
    public void resetFlightSettings() {
        setYawSensitivity(DroneControlSettings.YAW_SENSITIVITY.getDefaultValue());
        setMaxHorizontalSpeed(DroneControlSettings.MAX_HORIZONTAL_SPEED.getDefaultValue());
        setMaxVerticalSpeed(DroneControlSettings.MAX_VERTICAL_SPEED.getDefaultValue());
    }

    /**
     * Returns the maximum yaw rate after applying sensitivity.
     *
     * @return yaw rate in degrees per second
     */
    public double getYawRateDegreesPerSecond() {
        return YAW_RATE * yawSensitivity;
    }

    /**
     * Returns the drone collision radius.
     *
     * @return radius in meters
     */
    public double getDroneRadius() { return DRONE_RADIUS; }

    /**
     * Returns the drone collision height.
     *
     * @return height in meters
     */
    public double getDroneHeight() { return DRONE_HEIGHT; }

    /**
     * Returns the drone X coordinate.
     *
     * @return X coordinate
     */
    public double getX() { return x; }

    /**
     * Returns the drone Y coordinate.
     *
     * @return Y coordinate
     */
    public double getY() { return y; }

    /**
     * Returns the drone Z coordinate.
     *
     * @return Z coordinate
     */
    public double getZ() { return z; }

    /**
     * Returns the drone heading.
     *
     * @return yaw angle in degrees
     */
    public double getYaw() { return yaw; }

    /**
     * Returns the yaw sensitivity multiplier.
     *
     * @return yaw sensitivity multiplier
     */
    public double getYawSensitivity() { return yawSensitivity; }

    /**
     * Returns the maximum horizontal speed.
     *
     * @return maximum horizontal speed in meters per second
     */
    public double getMaxHorizontalSpeed() { return maxHorizontalSpeed; }

    /**
     * Returns the maximum vertical speed.
     *
     * @return maximum vertical speed in meters per second
     */
    public double getMaxVerticalSpeed() { return maxVerticalSpeed; }

    /**
     * Sets yaw sensitivity, clamped to supported control settings.
     *
     * @param yawSensitivity requested yaw sensitivity multiplier
     */
    public void setYawSensitivity(double yawSensitivity) {
        this.yawSensitivity =
                DroneControlSettings.YAW_SENSITIVITY.clamp(yawSensitivity);
    }

    /**
     * Sets maximum horizontal speed, clamped to supported control settings.
     *
     * @param maxHorizontalSpeed requested maximum horizontal speed
     */
    public void setMaxHorizontalSpeed(double maxHorizontalSpeed) {
        this.maxHorizontalSpeed =
                DroneControlSettings.MAX_HORIZONTAL_SPEED.clamp(maxHorizontalSpeed);
    }

    /**
     * Sets maximum vertical speed, clamped to supported control settings.
     *
     * @param maxVerticalSpeed requested maximum vertical speed
     */
    public void setMaxVerticalSpeed(double maxVerticalSpeed) {
        this.maxVerticalSpeed =
                DroneControlSettings.MAX_VERTICAL_SPEED.clamp(maxVerticalSpeed);
    }

    /**
     * Returns remaining battery as a percentage.
     *
     * @return battery level from 0 to 100 percent
     */
    public double getBatteryPercentage() {
        return (remainingFlightTimeSeconds
                / MAX_FLIGHT_TIME_SECONDS)
                * 100.0;
    }

    /**
     * Saves the current drone state.
     *
     * @return serializable state snapshot
     */
    public DroneState saveState() {
        return new DroneState(
                x, y, z, yaw,
                velocityX, velocityY, velocityZ,
                yawVelocity
        );
    }

    /**
     * Sets the drone spawn position and resets flight, velocity, battery, and autopilot state.
     *
     * @param x spawn X coordinate
     * @param y spawn Y coordinate
     * @param z spawn Z coordinate
     * @param yaw spawn heading in degrees
     */
    public void setSpawnPosition(
            double x, double y, double z, double yaw
    ) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.homeX = x;
        this.homeZ = z;

        // Reset velocities
        this.velocityX = 0;
        this.velocityY = 0;
        this.velocityZ = 0;
        this.yawVelocity = 0;

        // Reset battery
        remainingFlightTimeSeconds = MAX_FLIGHT_TIME_SECONDS;

        // Reset autopilot state
        this.state = FlightState.DISARMED;
        this.stateTimer = 0.0;
        this.targetTakeoffY = y;
        this.rthSafeY = y;
        this.rthPhase = ReturnToHomePhase.ASCENDING;
        clearTargetYaw();
    }

    /**
     * Generates a telemetry snapshot for HUD display.
     *
     * @return current telemetry values
     */
    public DroneTelemetry getTelemetry() {
        double altitudeMeters =
                Math.max(
                        0.0,
                        -y * METERS_PER_UNIT
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
                state
        );
    }
    /**
     * Resolves either a vertical or horizontal collision using collision data.
     *
     * @param collision collision information produced by the world detector
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
     *
     * @param normalX collision normal X component
     * @param normalZ collision normal Z component
     * @param penetration overlap distance to correct
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
     *
     * @param collision vertical collision information
     */
    private void resolveVerticalCollision(WorldCollision collision) {
        double correction = collision.getPenetration() + COLLISION_SKIN;

        this.y += collision.getNormalY() * correction;

        // Stop downward inertia when landing on a surface
        if (collision.getNormalY() < 0 && velocityY > 0) {
            velocityY = 0;
            if (state == FlightState.FALLING || state == FlightState.LANDING) {
                state = FlightState.DISARMED;
            }
        }

        // Stop upward inertia when hitting the bottom of an object
        if (collision.getNormalY() > 0 && velocityY < 0) {
            velocityY = 0;
        }
    }
}
