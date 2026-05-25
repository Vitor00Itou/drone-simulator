package fr.enac.drone.model.world;

import fr.enac.drone.model.drone.DroneModel;

/**
 * Detects collisions between the drone and solid world objects. It doesn't move the 
 * drone directly, but only analyzes the current drone position and returns collision 
 * information that can be used by the physics model to resolve the collision.
 */
public class WorldCollisionDetector {

    private final WorldConfiguration worldConfig;

    /**
     * Creates a detector for the active world configuration.
     *
     * @param worldConfig world objects used for collision checks
     */
    public WorldCollisionDetector(WorldConfiguration worldConfig) {
        this.worldConfig = worldConfig;
    }

    /**
     * Estimates the highest surface Y-coordinate directly below the drone.
     * Useful for dynamic landing and ground-effect calculations (simulating a downward sensor).
     *
     * @param drone drone to query against the world
     * @return closest surface Y coordinate below the drone, or {@code 0.0} when none is found
     */
    public double getGroundHeightBelow(DroneModel drone) {
        double droneY = drone.getY();
        double highestGroundY = Double.MAX_VALUE;

        for (WorldObject object : worldConfig.getObjects()) {
            if (object.isPlane()) {
                double planeY = object.getTopY();
                if (planeY >= droneY && planeY < highestGroundY) {
                    highestGroundY = planeY;
                }
                continue;
            }

            if (isHorizontallyOverObject(drone, object)) {
                double topY = object.getTopY();
                if (topY >= droneY && topY < highestGroundY) {
                    highestGroundY = topY;
                }
            }
        }

        return highestGroundY == Double.MAX_VALUE ? 0.0 : highestGroundY;
    }

    /**
     * Searches for the first collision between the drone and any solid object.
     *
     * @param drone drone to test
     * @return collision information, or {@code null} when there is no collision
     */
    public WorldCollision findCollision(DroneModel drone) {
        for (WorldObject object : worldConfig.getObjects()) {

            // Plane objects represent the ground surface
            if (object.isPlane()) {
                WorldCollision groundCollision =
                        checkGroundCollision(drone, object);

                if (groundCollision != null) {
                    return groundCollision;
                }

                continue;
            }

            if (!hasVerticalOverlap(drone, object)) {
                continue;
            }

            WorldCollision verticalCollision =
                    checkVerticalCollision(drone, object);

            if (verticalCollision == null) {
                continue;
            }

            WorldCollision horizontalCollision = null;

            if (object.isCylinder()) {
                horizontalCollision =
                        checkCylinderCollision(drone, object);
            } else if (object.isBox()) {
                horizontalCollision =
                        checkBoxCollision(drone, object);
            }

            if (horizontalCollision == null) {
                continue;
            }

            WorldCollision collision =
                    getShallowestCollision(verticalCollision, horizontalCollision);

            if (collision != null) {
                return collision;
            }
        }

        return null;
    }

    /**
     * Detects whether the drone has penetrated the ground plane.
     * The drone is considered to collide with the ground when its bottom
     * goes below the top surface of the plane.
     *
     * @param drone drone to test
     * @param ground ground plane object
     * @return ground collision information, or {@code null}
     */
    private WorldCollision checkGroundCollision(
            DroneModel drone,
            WorldObject ground
    ) {
        if (!isHorizontallyOverBoxFootprint(drone, ground)) {
            return null;
        }

        double droneBottom =
                drone.getY() + drone.getDroneHeight() / 2.0;

        double groundTop = ground.getTopY();

        if (droneBottom <= groundTop) {
            return null;
        }

        double penetration =
                droneBottom - groundTop;

        // Y axis points downward in JavaFX, so a negative Y normal pushes the drone upward
        return new WorldCollision(
                0.0,
                -1.0,
                0.0,
                penetration
        );
    }

    /**
     * Detects vertical contact with a solid world object.
     * The returned normal points toward the closest vertical exit direction.
     *
     * @param drone drone to test
     * @param object solid object to test
     * @return vertical collision information, or {@code null}
     */
    private WorldCollision checkVerticalCollision(
            DroneModel drone,
            WorldObject object
    ) {
        if (!isHorizontallyOverObject(drone, object)) {
            return null;
        }

        double droneTop =
                drone.getY() - drone.getDroneHeight() / 2.0;
        double droneBottom =
                drone.getY() + drone.getDroneHeight() / 2.0;

        double objectTop = object.getTopY();
        double objectBottom = object.getBottomY();

        double topPenetration =
                droneBottom - objectTop;
        double bottomPenetration =
                objectBottom - droneTop;

        if (topPenetration <= 0 || bottomPenetration <= 0) {
            return null;
        }

        if (topPenetration <= bottomPenetration) {
            // Y axis points downward in JavaFX, so negative Y pushes the drone upward
            return new WorldCollision(
                    0.0,
                    -1.0,
                    0.0,
                    topPenetration
            );
        }

        // Positive Y pushes the drone downward when it hits an object's underside
        return new WorldCollision(
                0.0,
                1.0,
                0.0,
                bottomPenetration
        );
    }

    /**
     * Checks whether the drone footprint overlaps the object's footprint.
     *
     * @param drone drone to test
     * @param object object footprint to test
     * @return {@code true} when the footprints overlap horizontally
     */
    private boolean isHorizontallyOverObject(
            DroneModel drone,
            WorldObject object
    ) {
        if (object.isCylinder()) {
            double dx = drone.getX() - object.getPosX();
            double dz = drone.getZ() - object.getPosZ();

            double droneRadius = drone.getDroneRadius();
            double objectRadius = object.getRadius();

            double maxDistance = droneRadius + objectRadius;

            return dx * dx + dz * dz <= maxDistance * maxDistance;
        }

        if (object.isBox()) {
            return isHorizontallyOverBoxFootprint(drone, object);
        }

        return false;
    }

    /**
     * Checks whether the drone footprint overlaps a box-like footprint.
     *
     * @param drone drone to test
     * @param object box or plane footprint to test
     * @return {@code true} when the expanded box footprint contains the drone center
     */
    private boolean isHorizontallyOverBoxFootprint(
            DroneModel drone,
            WorldObject object
    ) {
        double droneRadius = drone.getDroneRadius();

        double boxHalfWidth = object.getHalfWidth();
        double boxHalfDepth = object.getHalfDepth();

        return drone.getX() >= object.getPosX() - boxHalfWidth - droneRadius
                && drone.getX() <= object.getPosX() + boxHalfWidth + droneRadius
                && drone.getZ() >= object.getPosZ() - boxHalfDepth - droneRadius
                && drone.getZ() <= object.getPosZ() + boxHalfDepth + droneRadius;
    }

    /**
     * Checks whether the drone and the object overlap on the vertical axis.
     *
     * @param drone drone to test
     * @param object object to test
     * @return {@code true} when their vertical spans overlap
     */
    private boolean hasVerticalOverlap(DroneModel drone, WorldObject object) {
        double droneHalfHeight = drone.getDroneHeight() / 2.0;


        double droneMinY = drone.getY() - droneHalfHeight;
        double droneMaxY = drone.getY() + droneHalfHeight;

        double objectMinY = object.getTopY();
        double objectMaxY = object.getBottomY();

        return droneMaxY >= objectMinY
                && droneMinY <= objectMaxY;
    }

    /**
     * Checks horizontal collision between the drone and a cylindrical object.
     *
     * @param drone drone to test
     * @param object cylindrical object to test
     * @return horizontal collision information, or {@code null}
     */
    private WorldCollision checkCylinderCollision(DroneModel drone, WorldObject object) {
        double dx = drone.getX() - object.getPosX();
        double dz = drone.getZ() - object.getPosZ();

        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        double droneRadius = drone.getDroneRadius();

        double cylinderRadius = object.getRadius();
        double horizontalOverlap = droneRadius + cylinderRadius - horizontalDistance;

        if (horizontalOverlap <= 0) {
            return null;
        }

        double normalX;
        double normalZ;

        if (horizontalDistance < 0.0001) {
            normalX = 1.0;
            normalZ = 0.0;
        } else {
            normalX = dx / horizontalDistance;
            normalZ = dz / horizontalDistance;
        }

        return new WorldCollision(normalX, 0.0, normalZ, horizontalOverlap);
    }

    /**
     * Checks horizontal collision between the drone and a box object.
     *
     * @param drone drone to test
     * @param object box object to test
     * @return horizontal collision information, or {@code null}
     */
    private WorldCollision checkBoxCollision(
            DroneModel drone,
            WorldObject object
    ) {
        double droneRadius = drone.getDroneRadius();

        double boxHalfWidth = object.getHalfWidth();
        double boxHalfDepth = object.getHalfDepth();

        double dx = drone.getX() - object.getPosX();
        double dz = drone.getZ() - object.getPosZ();

        double overlapX = droneRadius + boxHalfWidth - Math.abs(dx);
        double overlapZ = droneRadius + boxHalfDepth - Math.abs(dz);

        if (overlapX <= 0 || overlapZ <= 0) {
            return null;
        }

        if (overlapX <= overlapZ) {
            double normalX = dx >= 0 ? 1.0 : -1.0;
            return new WorldCollision(normalX, 0.0, 0.0, overlapX);
        }

        double normalZ = dz >= 0 ? 1.0 : -1.0;
        return new WorldCollision(0.0, 0.0, normalZ, overlapZ);
    }

    /**
     * Selects the collision with the smaller penetration depth.
     *
     * @param first first candidate collision
     * @param second second candidate collision
     * @return shallowest non-null collision, or {@code null}
     */
    private WorldCollision getShallowestCollision(
            WorldCollision first,
            WorldCollision second
    ) {
        if (first == null) {
            return second;
        }

        if (second == null) {
            return first;
        }

        if (first.getPenetration() <= second.getPenetration()) {
            return first;
        }

        return second;
    }
}
