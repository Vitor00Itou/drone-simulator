package fr.enac.drone.model;

import java.util.List;

/**
 * Immutable description of the simulation world visible to renderers.
 */
public class World {
    private static final double DEFAULT_WORLD_SIZE = 5000.0;

    private final double size;
    private final List<Obstacle> obstacles;

    public World(double size, List<Obstacle> obstacles) {
        this.size = size;
        this.obstacles = List.copyOf(obstacles);
    }

    public static World createDefault() {
        return new World(DEFAULT_WORLD_SIZE, List.of(
                new Obstacle(150, 500, 30, 200)
        ));
    }

    public double getSize() {
        return size;
    }

    public List<Obstacle> getObstacles() {
        return obstacles;
    }
}
