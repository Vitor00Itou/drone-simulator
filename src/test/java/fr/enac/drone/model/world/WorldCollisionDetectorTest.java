package fr.enac.drone.model.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import fr.enac.drone.model.drone.DroneModel;

class WorldCollisionDetectorTest {

    private static final double EPSILON = 1e-9;

    @Test
    void detectsGroundCollisionWhenDroneBottomPenetratesPlane() {
        WorldCollisionDetector detector =
                new WorldCollisionDetector(worldWith(groundPlane()));

        WorldCollision collision =
                detector.findCollision(new TestDrone(0.0, 0.0, 0.0));

        assertNotNull(collision);
        assertTrue(collision.isVerticalCollision());
        assertEquals(-1.0, collision.getNormalY(), EPSILON);
        assertTrue(collision.getPenetration() > 0.0);
    }

    @Test
    void detectsHorizontalCollisionWithBoxSide() {
        WorldObject box = new WorldObject(
                "Box",
                "box",
                0.0,
                -10.0,
                0.0,
                10.0,
                20.0,
                10.0,
                "#777777"
        );
        WorldCollisionDetector detector =
                new WorldCollisionDetector(worldWith(box));

        WorldCollision collision =
                detector.findCollision(new TestDrone(5.2, -10.0, 0.0));

        assertNotNull(collision);
        assertFalse(collision.isVerticalCollision());
        assertEquals(1.0, collision.getNormalX(), EPSILON);
        assertEquals(0.0, collision.getNormalZ(), EPSILON);
        assertEquals(0.4, collision.getPenetration(), EPSILON);
    }

    @Test
    void reportsNearestSurfaceBelowDrone() {
        WorldObject platform = new WorldObject(
                "Platform",
                "box",
                0.0,
                -10.0,
                0.0,
                20.0,
                20.0,
                20.0,
                "#888888"
        );
        WorldCollisionDetector detector =
                new WorldCollisionDetector(worldWith(groundPlane(), platform));

        double groundY = detector.getGroundHeightBelow(new TestDrone(0.0, -30.0, 0.0));

        assertEquals(-20.0, groundY, EPSILON);
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

    private static final class TestDrone extends DroneModel {
        private final double x;
        private final double y;
        private final double z;

        private TestDrone(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public double getX() {
            return x;
        }

        @Override
        public double getY() {
            return y;
        }

        @Override
        public double getZ() {
            return z;
        }
    }
}
