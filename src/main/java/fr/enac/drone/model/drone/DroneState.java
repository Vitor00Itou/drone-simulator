package fr.enac.drone.model.drone;

/**
 * Serializable snapshot of the drone position, heading, and velocity.
 */
public class DroneState {
    /** Drone X coordinate in world space. */
    public double droneX;
    /** Drone Y coordinate in world space. */
    public double droneY;
    /** Drone Z coordinate in world space. */
    public double droneZ;
    /** Drone heading in degrees. */
    public double droneYaw;
    
    /** Horizontal X velocity in meters per second. */
    public double velocityX;
    /** Vertical velocity in meters per second. */
    public double velocityY;
    /** Horizontal Z velocity in meters per second. */
    public double velocityZ;
    /** Yaw velocity in degrees per second. */
    public double yawVelocity;

    /**
     * Creates the default state used by JSON deserialization.
     */
    public DroneState() {
        this.droneX = 0;
        this.droneY = -30;
        this.droneZ = 0;
        this.droneYaw = 0;
        this.velocityX = 0;
        this.velocityY = 0;
        this.velocityZ = 0;
        this.yawVelocity = 0;
    }

    /**
     * Creates a full state snapshot.
     *
     * @param droneX drone X coordinate
     * @param droneY drone Y coordinate
     * @param droneZ drone Z coordinate
     * @param droneYaw drone heading in degrees
     * @param velocityX horizontal X velocity in meters per second
     * @param velocityY vertical velocity in meters per second
     * @param velocityZ horizontal Z velocity in meters per second
     * @param yawVelocity yaw velocity in degrees per second
     */
    public DroneState(double droneX, double droneY, double droneZ, double droneYaw,
                      double velocityX, double velocityY, double velocityZ, double yawVelocity) {
        this.droneX = droneX;
        this.droneY = droneY;
        this.droneZ = droneZ;
        this.droneYaw = droneYaw;
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.yawVelocity = yawVelocity;
    }

    /**
     * Returns a debug representation of the state snapshot.
     *
     * @return state fields as a string
     */
    @Override
    public String toString() {
        return "DroneState{" +
                "droneX=" + droneX +
                ", droneY=" + droneY +
                ", droneZ=" + droneZ +
                ", droneYaw=" + droneYaw +
                ", velocityX=" + velocityX +
                ", velocityY=" + velocityY +
                ", velocityZ=" + velocityZ +
                ", yawVelocity=" + yawVelocity +
                '}';
    }
}
