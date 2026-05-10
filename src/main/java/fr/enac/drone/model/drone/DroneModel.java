package fr.enac.drone.model.drone;

import fr.enac.drone.utils.MathUtils;

public class DroneModel {
    private static final double METERS_PER_UNIT = 1.0;

    private double x = 0;
    private double y = -30;
    private double z = 0;
    private double yaw = 0;
    private double yawVelocity = 0.0;

    // Pour l'autopilote : consigne de vitesse angulaire
    private double targetYawVelocity = 0.0;
    private boolean useTargetYaw = false;

    private final double homeX = x;
    private final double homeZ = z;

    private double velocityX = 0.0;
    private double velocityY = 0.0;
    private double velocityZ = 0.0;

    private static final double ACCELERATION_HORIZONTAL = 8.0;
    private static final double ACCELERATION_VERTICAL = 8.0;
    private static final double DRAG_COEFFICIENT = 0.85;
    private static final double YAW_DRAG = 0.0005;
    private static final double GRAVITY = 9.81;
    private static final double HOVER_THRUST = 9.81;
    private static final double MAX_HORIZONTAL_SPEED = 25.0;
    private static final double MAX_VERTICAL_SPEED = 8.0;
    private static final double YAW_RATE = 40.0;
    private static final double YAW_ACCELERATION = 120.0;

    public void updatePhysics(double pitchInput, double rollInput, double throttleInput, double deltaTime) {
        double inputLength = Math.sqrt(pitchInput * pitchInput + rollInput * rollInput);
        if (inputLength > 1.0) {
            pitchInput /= inputLength;
            rollInput /= inputLength;
            inputLength = 1.0;
        }

        double radYaw = Math.toRadians(this.yaw);
        double moveX = (pitchInput * Math.sin(radYaw)) + (rollInput * Math.cos(radYaw));
        double moveZ = (pitchInput * Math.cos(radYaw)) - (rollInput * Math.sin(radYaw));

        double targetVelocityX = moveX * MAX_HORIZONTAL_SPEED;
        double targetVelocityZ = moveZ * MAX_HORIZONTAL_SPEED;

        double accelX = (targetVelocityX - velocityX) * ACCELERATION_HORIZONTAL * deltaTime;
        double accelZ = (targetVelocityZ - velocityZ) * ACCELERATION_HORIZONTAL * deltaTime;
        velocityX += accelX;
        velocityZ += accelZ;
        velocityX *= Math.pow(DRAG_COEFFICIENT, deltaTime);
        velocityZ *= Math.pow(DRAG_COEFFICIENT, deltaTime);

        double targetVelocityY = throttleInput * MAX_VERTICAL_SPEED;
        double accelY = (targetVelocityY - velocityY) * ACCELERATION_VERTICAL * deltaTime;
        velocityY += accelY;
        velocityY += (HOVER_THRUST - GRAVITY) * deltaTime;
        velocityY *= Math.pow(DRAG_COEFFICIENT, deltaTime);

        this.x += velocityX * deltaTime;
        this.z += velocityZ * deltaTime;
        this.y += velocityY * deltaTime;
    }

    public void yawLeft(double deltaTime) {
        double target = -YAW_RATE;
        yawVelocity += (target - yawVelocity) * YAW_ACCELERATION * deltaTime;
        yaw += yawVelocity * deltaTime;
    }

    public void yawRight(double deltaTime) {
        double target = YAW_RATE;
        yawVelocity += (target - yawVelocity) * YAW_ACCELERATION * deltaTime;
        yaw += yawVelocity * deltaTime;
    }

    public void updateYaw(double deltaTime) {
        if (useTargetYaw) {
            // Accélération vers la consigne de vitesse angulaire (autopilote)
            yawVelocity += (targetYawVelocity - yawVelocity) * YAW_ACCELERATION * deltaTime;
        } else {
            // Pas de consigne : frottement pur
            yawVelocity *= Math.pow(YAW_DRAG, deltaTime);
        }
        yaw += yawVelocity * deltaTime;
    }

    // ---------- API pour l'autopilote ----------
    public void setTargetYawVelocity(double rateDegPerSec) {
        this.targetYawVelocity = rateDegPerSec;
        this.useTargetYaw = true;
    }

    public void clearTargetYaw() {
        this.useTargetYaw = false;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public double getYaw() { return yaw; }

    public DroneTelemetry getTelemetry() {
        double altitudeMeters = -y * METERS_PER_UNIT;
        double headingDegrees = MathUtils.normalizeHeading(yaw);
        double distanceMeters = MathUtils.distance2D(homeX, homeZ, x, z) * METERS_PER_UNIT;
        double horizontalSpeedMs = Math.sqrt(velocityX * velocityX + velocityZ * velocityZ);
        double verticalSpeedMs = -velocityY;
        return new DroneTelemetry(altitudeMeters, horizontalSpeedMs, verticalSpeedMs, headingDegrees, distanceMeters);
    }

    public DroneState saveState() {
        return new DroneState(x, y, z, yaw, velocityX, velocityY, velocityZ, yawVelocity);
    }

    public void setSpawnPosition(double x, double y, double z, double yaw) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.velocityX = 0;
        this.velocityY = 0;
        this.velocityZ = 0;
        this.yawVelocity = 0;
        clearTargetYaw();   // sécurité
    }
}