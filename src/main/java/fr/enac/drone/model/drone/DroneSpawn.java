package fr.enac.drone.model.drone;

/**
 * Represents the drone's spawn position and orientation in the world configuration.
 */
public class DroneSpawn {
    private double posX;
    private double posY;
    private double posZ;
    private double yaw;

    public DroneSpawn() {
        this.posX = 0;
        this.posY = -30; // Above ground
        this.posZ = 0;
        this.yaw = 0;
    }

    public DroneSpawn(double posX, double posY, double posZ, double yaw) {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.yaw = yaw;
    }

    public double getPosX() {
        return posX;
    }

    public void setPosX(double posX) {
        this.posX = posX;
    }

    public double getPosY() {
        return posY;
    }

    public void setPosY(double posY) {
        this.posY = posY;
    }

    public double getPosZ() {
        return posZ;
    }

    public void setPosZ(double posZ) {
        this.posZ = posZ;
    }

    public double getYaw() {
        return yaw;
    }

    public void setYaw(double yaw) {
        this.yaw = yaw;
    }

    @Override
    public String toString() {
        return "DroneSpawn{" +
                "pos=(" + posX + ", " + posY + ", " + posZ + ")" +
                ", yaw=" + yaw +
                '}';
    }
}
