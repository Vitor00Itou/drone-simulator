package fr.enac.drone.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Manages the simulation world state, including obstacle generation.
 */
public class World {
    private final List<Obstacle> obstacles = new ArrayList<>();
    private WorldMode currentWorldMode = WorldMode.FEW_OBSTACLES;

    private static final double WORLD_LIMIT = 2200;
    private static final double MIN_OBSTACLE_HEIGHT = 80;
    private static final double MAX_OBSTACLE_HEIGHT = 250;
    private static final double MIN_OBSTACLE_RADIUS = 15;
    private static final double MAX_OBSTACLE_RADIUS = 27.5;

    /**
     * Initializes the world using the default world mode.
     */
    public World() {
        generateObstacles(currentWorldMode);
    }

    /**
     * Updates the world mode and regenerates obstacles.
     */
    public void setWorldMode(WorldMode worldMode) {
        this.currentWorldMode = worldMode;
        generateObstacles(worldMode);
    }

    /**
     * Returns the currently active world mode.
     */
    public WorldMode getCurrentWorldMode() {
        return currentWorldMode;
    }

    /**
     * Provides a read-only list of generated obstacles.
     */
    public List<Obstacle> getObstacles() {
        return Collections.unmodifiableList(obstacles);
    }

    /**
     * Generates random obstacles according to the selected world mode.
     */
    private void generateObstacles(WorldMode worldMode) {
        obstacles.clear();

        Random random = new Random();

        for (int i = 0; i < worldMode.getObstacleCount(); i++) {
            double x;
            double z;

            do {
                x = -WORLD_LIMIT + random.nextDouble() * (2 * WORLD_LIMIT);
                z = -WORLD_LIMIT + random.nextDouble() * (2 * WORLD_LIMIT);
            } while (Math.sqrt(x * x + z * z) < 250);

            double height = MIN_OBSTACLE_HEIGHT
                    + random.nextDouble() * (MAX_OBSTACLE_HEIGHT - MIN_OBSTACLE_HEIGHT);

            double radius = MIN_OBSTACLE_RADIUS
                    + random.nextDouble() * (MAX_OBSTACLE_RADIUS - MIN_OBSTACLE_RADIUS);

            obstacles.add(new Obstacle(x, z, radius, height));
        }
    }
}