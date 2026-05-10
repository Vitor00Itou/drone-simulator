package fr.enac.drone.model.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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

    private List<WorldObject> createDefaultObjects() {
        List<WorldObject> defaultObjects = new ArrayList<>();
        
        // Ground plane (always present)
        defaultObjects.add(new WorldObject(
            "Ground", "plane", 0, 0, 0, 5000, 1, 5000, "#228B22"
        ));
        
        // Reference tower
        defaultObjects.add(new WorldObject(
            "Reference Tower", "cylinder", 150, -70, 500, 30, 200, 30, "#FFA500"
        ));
        
        
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
