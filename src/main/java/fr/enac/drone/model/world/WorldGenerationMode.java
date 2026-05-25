package fr.enac.drone.model.world;

/**
 * Obstacle density used when generating a random world.
 */
public enum WorldGenerationMode {
    FEW_OBSTACLES(35),
    MEDIUM_OBSTACLES(45),
    MANY_OBSTACLES(55);

    private final int obstacleCount;

    WorldGenerationMode(int obstacleCount) {
        this.obstacleCount = obstacleCount;
    }

    public int getObstacleCount() {
        return obstacleCount;
    }
}
