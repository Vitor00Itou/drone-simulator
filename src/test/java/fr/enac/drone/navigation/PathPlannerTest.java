package fr.enac.drone.navigation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import fr.enac.drone.model.WorldPosition2D;
import fr.enac.drone.model.world.WorldConfiguration;
import fr.enac.drone.model.world.WorldObject;
import fr.enac.drone.model.world.WorldObjectType;

class PathPlannerTest {

    private static final double CELL_SIZE = 4.0;

    @Test
    void findsPathAcrossOpenWorld() {
        PathPlanner planner = new PathPlanner(worldWith(groundPlane()));

        List<WorldPosition2D> path = planner.findPath(0.0, 0.0, 0.0, 30.0, 30.0);

        assertFalse(path.isEmpty());
        assertWaypointNear(path.get(0), 0.0, 0.0);
        assertWaypointNear(path.get(path.size() - 1), 30.0, 30.0);
    }

    @Test
    void routesToNearestFreeCellWhenTargetIsBlocked() {
        WorldObject blockedTarget = new WorldObject(
                "Blocked Target",
                WorldObjectType.BOX,
                30.0,
                -5.0,
                30.0,
                24.0,
                10.0,
                24.0,
                "#555555"
        );
        PathPlanner planner = new PathPlanner(worldWith(groundPlane(), blockedTarget));

        List<WorldPosition2D> path = planner.findPath(-50.0, -5.0, -50.0, 30.0, 30.0);

        assertFalse(path.isEmpty());
        WorldPosition2D lastWaypoint = path.get(path.size() - 1);
        assertTrue(Math.hypot(lastWaypoint.x() - 30.0, lastWaypoint.z() - 30.0) > CELL_SIZE);
    }

    private static WorldConfiguration worldWith(WorldObject... objects) {
        WorldConfiguration config = new WorldConfiguration("Test World");
        config.setObjects(Arrays.asList(objects));
        return config;
    }

    private static WorldObject groundPlane() {
        return new WorldObject(
                "Ground",
                WorldObjectType.PLANE,
                0.0,
                0.0,
                0.0,
                5000.0,
                1.0,
                5000.0,
                "#228B22"
        );
    }

    private static void assertWaypointNear(WorldPosition2D waypoint, double x, double z) {
        assertTrue(Math.abs(waypoint.x() - x) <= CELL_SIZE);
        assertTrue(Math.abs(waypoint.z() - z) <= CELL_SIZE);
    }
}
