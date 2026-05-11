package fr.enac.drone.model.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import fr.enac.drone.model.drone.DroneSpawn;

/**
 * Represents the configuration and objects in the world that can be saved/loaded.
 * This is separate from the drone's physics state.
 */
public class WorldConfiguration {
    private String worldName;
    private DroneSpawn droneSpawn;
    private List<WorldObject> objects;
    private WorldEnvironment environment;

    public WorldConfiguration() {
        this.worldName = "Default World";
        this.droneSpawn = new DroneSpawn();
        this.objects = createDefaultObjects();
        this.environment = new WorldEnvironment();
    }

    public WorldConfiguration(String worldName) {
        setWorldName(worldName);
        this.droneSpawn = new DroneSpawn();
        this.objects = createDefaultObjects();
        this.environment = new WorldEnvironment();
    }

    // Getters and Setters with validation
    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        if (worldName == null || worldName.trim().isEmpty()) {
            throw new IllegalArgumentException("World name cannot be null or empty");
        }
        this.worldName = worldName;
    }

    public DroneSpawn getDroneSpawn() {
        return droneSpawn;
    }

    public void setDroneSpawn(DroneSpawn droneSpawn) {
        this.droneSpawn = Objects.requireNonNull(droneSpawn, "DroneSpawn cannot be null");
    }

    public List<WorldObject> getObjects() {
        return Collections.unmodifiableList(objects);
    }

    public void setObjects(List<WorldObject> objects) {
        this.objects = new ArrayList<>(Objects.requireNonNull(objects, "Objects list cannot be null"));
    }

    public void addObject(WorldObject obj) {
        Objects.requireNonNull(obj, "Object cannot be null");
        this.objects.add(obj);
    }

    public WorldEnvironment getEnvironment() {
        return environment;
    }

    public void setEnvironment(WorldEnvironment environment) {
        this.environment = Objects.requireNonNull(environment, "Environment cannot be null");
    }

    public void generateRandomObstacles(int obstacleCount) {
        List<WorldObject> newObjects = new ArrayList<>();

        // Keep the ground plane
        for (WorldObject object : objects) {
            if ("plane".equals(object.getType())) {
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
                    "cylinder",
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

    private List<WorldObject> createDefaultObjects() {
        List<WorldObject> defaultObjects = new ArrayList<>();
        
        // Ground plane (always present)
        WorldObject ground = new WorldObject(
            "Ground", "plane", 0, 0, 0, 5000, 1, 5000, "#228B22"
        );
        defaultObjects.add(ground);
        
        // Reference tower
        WorldObject referenceTower = new WorldObject(
            "Reference Tower", "cylinder", 150, -70, 500, 30, 200, 30, "#FFA500"
        );
        defaultObjects.add(referenceTower);
        
        
        return defaultObjects;
    }

    @Override
    public String toString() {
        return "WorldConfiguration{" +
                "worldName='" + worldName + '\'' +
                ", objects=" + objects.size() +
                ", environment=" + environment +
                '}';
    }
}
