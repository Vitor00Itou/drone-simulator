package fr.enac.drone.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import fr.enac.drone.model.world.WorldConfiguration;
import fr.enac.drone.model.world.WorldObject;

class PathPlannerTest {

    private static final double CELL_SIZE = 10.0;

    @Test
    void findsPathAcrossOpenWorld() {
        PathPlanner planner = new PathPlanner(worldWith(groundPlane()));

        List<double[]> path = planner.findPath(0.0, 0.0, 30.0, 30.0);

        assertFalse(path.isEmpty());
        assertWaypointNear(path.get(0), 0.0, 0.0);
        assertWaypointNear(path.get(path.size() - 1), 30.0, 30.0);
    }

    @Test
    void returnsEmptyPathWhenStartCellIsBlocked() {
        WorldObject startBlock = new WorldObject(
                "Start Block",
                "box",
                0.0,
                -5.0,
                0.0,
                20.0,
                10.0,
                20.0,
                "#555555"
        );
        PathPlanner planner = new PathPlanner(worldWith(groundPlane(), startBlock));

        List<double[]> path = planner.findPath(0.0, 0.0, 30.0, 30.0);

        assertTrue(path.isEmpty());
    }

    private static WorldConfiguration worldWith(WorldObject... objects) {
        WorldConfiguration config = new WorldConfiguration("Test World");
        config.setObjects(Arrays.asList(objects));
        return config;
    }

    private static WorldObject groundPlane() {
        return new WorldObject(
                "Ground",
                "plane",
                0.0,
                0.0,
                0.0,
                5000.0,
                1.0,
                5000.0,
                "#228B22"
        );
    }

    private static void assertWaypointNear(double[] waypoint, double x, double z) {
        assertTrue(Math.abs(waypoint[0] - x) <= CELL_SIZE);
        assertTrue(Math.abs(waypoint[1] - z) <= CELL_SIZE);
    }
}
