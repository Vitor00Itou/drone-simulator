package fr.enac.drone.model.drone;

/**
 * Represents the drone's spawn position and orientation in the world configuration.
 */
public class DroneSpawn {
    private double posX;
    private double posY;
    private double posZ;
    private double yaw;

    /**
     * Creates the default spawn above the ground at the world origin.
     */
    public DroneSpawn() {
        this.posX = 0;
        this.posY = -30; // Above ground
        this.posZ = 0;
        this.yaw = 0;
    }

    /**
     * Creates a spawn with explicit world coordinates and heading.
     *
     * @param posX spawn X coordinate
     * @param posY spawn Y coordinate
     * @param posZ spawn Z coordinate
     * @param yaw spawn heading in degrees
     */
    public DroneSpawn(double posX, double posY, double posZ, double yaw) {
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        this.yaw = yaw;
    }

    /**
     * Returns the spawn X coordinate.
     *
     * @return spawn X coordinate
     */
    public double getPosX() {
        return posX;
    }

    /**
     * Sets the spawn X coordinate.
     *
     * @param posX spawn X coordinate
     */
    public void setPosX(double posX) {
        this.posX = posX;
    }

    /**
     * Returns the spawn Y coordinate.
     *
     * @return spawn Y coordinate
     */
    public double getPosY() {
        return posY;
    }

    /**
     * Sets the spawn Y coordinate.
     *
     * @param posY spawn Y coordinate
     */
    public void setPosY(double posY) {
        this.posY = posY;
    }

    /**
     * Returns the spawn Z coordinate.
     *
     * @return spawn Z coordinate
     */
    public double getPosZ() {
        return posZ;
    }

    /**
     * Sets the spawn Z coordinate.
     *
     * @param posZ spawn Z coordinate
     */
    public void setPosZ(double posZ) {
        this.posZ = posZ;
    }

    /**
     * Returns the spawn heading.
     *
     * @return yaw angle in degrees
     */
    public double getYaw() {
        return yaw;
    }

    /**
     * Sets the spawn heading.
     *
     * @param yaw yaw angle in degrees
     */
    public void setYaw(double yaw) {
        this.yaw = yaw;
    }

    /**
     * Returns a debug representation of the spawn.
     *
     * @return spawn coordinates and yaw
     */
    @Override
    public String toString() {
        return "DroneSpawn{" +
                "pos=(" + posX + ", " + posY + ", " + posZ + ")" +
                ", yaw=" + yaw +
                '}';
    }
}
