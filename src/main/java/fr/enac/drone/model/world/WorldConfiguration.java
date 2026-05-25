package fr.enac.drone.model.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import fr.enac.drone.model.drone.DroneSpawn;

/**
 * Represents the persisted world configuration, including objects, environment, and spawn point.
 * This is separate from the drone's physics state.
 */
public class WorldConfiguration {
    private String worldName;
    private DroneSpawn droneSpawn;
    private List<WorldObject> objects;
    private WorldEnvironment environment;

    /**
     * Creates the default world configuration.
     */
    public WorldConfiguration() {
        this.worldName = "Default World";
        this.droneSpawn = new DroneSpawn();
        this.objects = createDefaultObjects();
        this.environment = new WorldEnvironment();
        updateSpawnBase();
    }

    /**
     * Creates a default world with a custom display name.
     *
     * @param worldName non-empty world name
     */
    public WorldConfiguration(String worldName) {
        setWorldName(worldName);
        this.droneSpawn = new DroneSpawn();
        this.objects = createDefaultObjects();
        this.environment = new WorldEnvironment();
        updateSpawnBase();
    }

    /**
     * Returns the world display name.
     *
     * @return world name
     */
    public String getWorldName() {
        return worldName;
    }

    /**
     * Sets the world display name.
     *
     * @param worldName non-empty world name
     * @throws IllegalArgumentException if the name is blank
     */
    public void setWorldName(String worldName) {
        if (worldName == null || worldName.trim().isEmpty()) {
            throw new IllegalArgumentException("World name cannot be null or empty");
        }
        this.worldName = worldName;
    }

    /**
     * Returns the drone spawn configuration.
     *
     * @return spawn position and heading
     */
    public DroneSpawn getDroneSpawn() {
        return droneSpawn;
    }

    /**
     * Sets the drone spawn configuration.
     *
     * @param droneSpawn non-null spawn configuration
     */
    public void setDroneSpawn(DroneSpawn droneSpawn) {
        this.droneSpawn = Objects.requireNonNull(droneSpawn, "DroneSpawn cannot be null");
    }

    /**
     * Returns an immutable view of world objects.
     *
     * @return configured world objects
     */
    public List<WorldObject> getObjects() {
        return Collections.unmodifiableList(objects);
    }

    /**
     * Replaces all world objects.
     *
     * @param objects new object list
     */
    public void setObjects(List<WorldObject> objects) {
        this.objects = new ArrayList<>(Objects.requireNonNull(objects, "Objects list cannot be null"));
    }

    /**
     * Adds an object to the world configuration.
     *
     * @param obj object to add
     */
    public void addObject(WorldObject obj) {
        Objects.requireNonNull(obj, "Object cannot be null");
        this.objects.add(obj);
    }

    /**
     * Returns environmental rendering and physics settings.
     *
     * @return world environment settings
     */
    public WorldEnvironment getEnvironment() {
        return environment;
    }

    /**
     * Replaces environmental settings.
     *
     * @param environment non-null environment settings
     */
    public void setEnvironment(WorldEnvironment environment) {
        this.environment = Objects.requireNonNull(environment, "Environment cannot be null");
    }

    /**
     * Rebuilds the visible spawn platform so it matches the configured spawn point.
     */
    public void updateSpawnBase() {
        if (this.droneSpawn == null || this.objects == null) return;

        this.objects.removeIf(obj -> "Spawn Base".equals(obj.getName()));

        double spawnY = this.droneSpawn.getPosY();
        double droneBottomY = spawnY + 0.05;
        double baseHeight = Math.max(0.1, -droneBottomY);
        double baseY = droneBottomY + baseHeight / 2.0;
        double baseRadius = 20.0;

        WorldObject spawnBase = new WorldObject(
                "Spawn Base", WorldObjectType.CYLINDER, this.droneSpawn.getPosX(), baseY, this.droneSpawn.getPosZ(),
                baseRadius, baseHeight, baseRadius, "#A9A9A9"
        );
        this.objects.add(spawnBase);
    }

    /**
     * Replaces generated obstacles while keeping the ground plane and spawn base.
     *
     * @param obstacleCount number of random cylindrical obstacles to generate
     */
    public void generateRandomObstacles(int obstacleCount) {
        List<WorldObject> newObjects = new ArrayList<>();

        // Keep the ground plane and spawn base, but remove other objects
        for (WorldObject object : objects) {
            if (object.isPlane() || "Spawn Base".equals(object.getName())) {
                newObjects.add(object);
            }
        }

        Random random = new Random();

        double worldLimit = 2200;
        double minHeight = 80;
        double maxHeight = 250;
        double minRadius = 15;
        double maxRadius = 27.5;

        for (int i = 0; i < obstacleCount; i++) {
            double x;
            double z;

            do {
                x = -worldLimit + random.nextDouble() * (2 * worldLimit);
                z = -worldLimit + random.nextDouble() * (2 * worldLimit);
            } while (Math.sqrt(x * x + z * z) < 250);

            double height = minHeight + random.nextDouble() * (maxHeight - minHeight);
            double radius = minRadius + random.nextDouble() * (maxRadius - minRadius);

            double y = 0 - height / 2;

            newObjects.add(new WorldObject(
                    "Obstacle " + (i + 1),
                    WorldObjectType.CYLINDER,
                    x,
                    y,
                    z,
                    radius,
                    height,
                    radius,
                    "#FFA500"
            ));
        }

        setObjects(newObjects);
    }

    /**
     * Creates the initial ground plane and reference tower.
     *
     * @return default world object list
     */
    private List<WorldObject> createDefaultObjects() {
        List<WorldObject> defaultObjects = new ArrayList<>();
        
        // Ground plane (always present)
        WorldObject ground = new WorldObject(
            "Ground", WorldObjectType.PLANE, 0, 0, 0, 5000, 1, 5000, "#228B22"
        );
        defaultObjects.add(ground);
        
        // Reference tower
        WorldObject referenceTower = new WorldObject(
            "Reference Tower", WorldObjectType.CYLINDER, 150, -70, 500, 30, 200, 30, "#FFA500"
        );
        defaultObjects.add(referenceTower);
        
        
        return defaultObjects;
    }

    /**
     * Returns a debug summary of the world configuration.
     *
     * @return world name, object count, and environment
     */
    @Override
    public String toString() {
        return "WorldConfiguration{" +
                "worldName='" + worldName + '\'' +
                ", objects=" + objects.size() +
                ", environment=" + environment +
                '}';
    }
}
