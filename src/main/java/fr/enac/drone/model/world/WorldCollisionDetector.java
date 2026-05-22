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

            // Check if the drone is landing on top of this object before side collision checks
            WorldCollision topCollision =
                    checkTopCollision(drone, object);

            if (topCollision != null) {
                return topCollision;
            }

            if (!hasVerticalOverlap(drone, object)) {
                continue;
            }

            if ("cylinder".equals(object.getType())) {
                WorldCollision collision =
                        checkCylinderCollision(drone, object);

                if (collision != null) {
                    return collision;
                }
            }

            if ("box".equals(object.getType())) {
                WorldCollision collision =
                        checkBoxCollision(drone, object);

                if (collision != null) {
                    return collision;
                }
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
    * Detects whether the drone is landing on top of a solid world object.
    * This prevents top contact from being handled as a side collision.
    */
    private WorldCollision checkTopCollision(
            DroneModel drone,
            WorldObject object
    ) {
        double droneBottom =
                drone.getY() + drone.getDroneHeight() / 2.0;

        double objectTop =
                object.getPosY() - object.getSizeY() / 2.0;

        double penetration =
                droneBottom - objectTop;

        if (penetration <= 0) {
            return null;
        }

        if (!isHorizontallyOverObject(drone, object)) {
            return null;
        }

        // Y axis points downward in JavaFX, so negative Y pushes the drone upward
        return new WorldCollision(
                0.0,
                -1.0,
                0.0,
                penetration
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

            double droneRadius =
                    Math.max(
                            drone.getDroneWidth(),
                            drone.getDroneDepth()
                    ) / 2.0;

            double objectRadius = object.getSizeX();

            double maxDistance = droneRadius + objectRadius;

            return dx * dx + dz * dz <= maxDistance * maxDistance;
        }

        if ("box".equals(object.getType())) {
            double droneHalfWidth = drone.getDroneWidth() / 2.0;
            double droneHalfDepth = drone.getDroneDepth() / 2.0;

            double boxHalfWidth = object.getSizeX() / 2.0;
            double boxHalfDepth = object.getSizeZ() / 2.0;

            return drone.getX() >= object.getPosX() - boxHalfWidth - droneHalfWidth
                    && drone.getX() <= object.getPosX() + boxHalfWidth + droneHalfWidth
                    && drone.getZ() >= object.getPosZ() - boxHalfDepth - droneHalfDepth
                    && drone.getZ() <= object.getPosZ() + boxHalfDepth + droneHalfDepth;
        }

        return false;
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

        double droneRadius =
                Math.max(drone.getDroneWidth(), drone.getDroneDepth()) / 2.0;

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
        double droneHalfWidth = drone.getDroneWidth() / 2.0;
        double droneHalfDepth = drone.getDroneDepth() / 2.0;

        double boxHalfWidth = object.getSizeX() / 2.0;
        double boxHalfDepth = object.getSizeZ() / 2.0;

        double dx = drone.getX() - object.getPosX();
        double dz = drone.getZ() - object.getPosZ();

        double overlapX = droneHalfWidth + boxHalfWidth - Math.abs(dx);
        double overlapZ = droneHalfDepth + boxHalfDepth - Math.abs(dz);

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
}