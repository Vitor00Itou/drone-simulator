package fr.enac.drone.model.world;

import fr.enac.drone.model.drone.DroneModel;

/**
 * Detects collisions between the drone and solid world objects. It doesn't move the 
 * drone directly, but only analyzes the current drone position and returns collision 
 * information that can be used by the physics model to resolve the collision.
 */
public class WorldCollisionDetector {

    private final WorldConfiguration worldConfig;

    public WorldCollisionDetector(WorldConfiguration worldConfig) {
        this.worldConfig = worldConfig;
    }

    /**
     * Searches for the first collision between the drone and any solid object.
     */
    public WorldCollision findCollision(DroneModel drone) {
        for (WorldObject object : worldConfig.getObjects()) {

            // Plane objects represent the ground surface
            if ("plane".equals(object.getType())) {
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

            if ("cylinder".equals(object.getType())) {
                horizontalCollision =
                        checkCylinderCollision(drone, object);
            } else if ("box".equals(object.getType())) {
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

        double groundTop =
                ground.getPosY() - ground.getSizeY() / 2.0;

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

        double objectTop =
                object.getPosY() - object.getSizeY() / 2.0;
        double objectBottom =
                object.getPosY() + object.getSizeY() / 2.0;

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
    */
    private boolean isHorizontallyOverObject(
            DroneModel drone,
            WorldObject object
    ) {
        if ("cylinder".equals(object.getType())) {
            double dx = drone.getX() - object.getPosX();
            double dz = drone.getZ() - object.getPosZ();

            double droneRadius = drone.getDroneRadius();
            double objectRadius = object.getSizeX();

            double maxDistance = droneRadius + objectRadius;

            return dx * dx + dz * dz <= maxDistance * maxDistance;
        }

        if ("box".equals(object.getType())) {
            return isHorizontallyOverBoxFootprint(drone, object);
        }

        return false;
    }

    private boolean isHorizontallyOverBoxFootprint(
            DroneModel drone,
            WorldObject object
    ) {
        double droneRadius = drone.getDroneRadius();

        double boxHalfWidth = object.getSizeX() / 2.0;
        double boxHalfDepth = object.getSizeZ() / 2.0;

        return drone.getX() >= object.getPosX() - boxHalfWidth - droneRadius
                && drone.getX() <= object.getPosX() + boxHalfWidth + droneRadius
                && drone.getZ() >= object.getPosZ() - boxHalfDepth - droneRadius
                && drone.getZ() <= object.getPosZ() + boxHalfDepth + droneRadius;
    }

    /**
     * Checks whether the drone and the object overlap on the vertical axis.
     */
    private boolean hasVerticalOverlap(DroneModel drone, WorldObject object) {
        double droneHalfHeight = drone.getDroneHeight() / 2.0;


        double droneMinY = drone.getY() - droneHalfHeight;
        double droneMaxY = drone.getY() + droneHalfHeight;

        double objectMinY =
                object.getPosY() - object.getSizeY() / 2.0;
        double objectMaxY =
                object.getPosY() + object.getSizeY() / 2.0;

        return droneMaxY >= objectMinY
                && droneMinY <= objectMaxY;
    }

    /**
    * Checks horizontal collision between the drone and a cylindrical object.
    */
    private WorldCollision checkCylinderCollision(DroneModel drone, WorldObject object) {
        double dx = drone.getX() - object.getPosX();
        double dz = drone.getZ() - object.getPosZ();

        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);

        double droneRadius = drone.getDroneRadius();

        double cylinderRadius = object.getSizeX();
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
    */
    private WorldCollision checkBoxCollision(
            DroneModel drone,
            WorldObject object
    ) {
        double droneRadius = drone.getDroneRadius();

        double boxHalfWidth = object.getSizeX() / 2.0;
        double boxHalfDepth = object.getSizeZ() / 2.0;

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
