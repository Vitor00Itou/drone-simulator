package fr.enac.drone.model.world;

/**
 * Obstacle density used when generating a random world.
 */
public enum WorldGenerationMode {
    /** Sparse generated world. */
    FEW_OBSTACLES(35),
    /** Balanced generated world. */
    MEDIUM_OBSTACLES(45),
    /** Dense generated world. */
    MANY_OBSTACLES(55);

    private final int obstacleCount;

    /**
     * Creates a generation mode.
     *
     * @param obstacleCount number of obstacles produced by this mode
     */
    WorldGenerationMode(int obstacleCount) {
        this.obstacleCount = obstacleCount;
    }

    /**
     * Returns the number of obstacles generated for this mode.
     *
     * @return obstacle count
     */
    public int getObstacleCount() {
        return obstacleCount;
    }
}
