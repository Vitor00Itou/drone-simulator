package fr.enac.drone.model;

/**
 * Represents the different world configurations available in the simulator.
 * Each mode defines the number of obstacles to be generated in the environment.
 */

public enum WorldMode {
    FEW_OBSTACLES(35),
    MEDIUM_OBSTACLES(45),
    MANY_OBSTACLES(55);

    private final int obstacleCount;

    WorldMode(int obstacleCount) {
        this.obstacleCount = obstacleCount;
    }

    public int getObstacleCount() {
        return obstacleCount;
    }
}