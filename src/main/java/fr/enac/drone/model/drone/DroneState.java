package fr.enac.drone.model.drone;

/**
 * Represents the current drone state in the world.
 */
public class DroneState {
    public double droneX;
    public double droneY;
    public double droneZ;
    public double droneYaw;
    
    public double velocityX;
    public double velocityY;
    public double velocityZ;
    public double yawVelocity;

    // Default constructor for JSON deserialization
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
