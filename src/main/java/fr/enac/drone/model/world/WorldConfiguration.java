package fr.enac.drone.model.world;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the configuration and objects in the world that can be saved/loaded.
 * This is separate from the drone's physics state.
 */
public class WorldConfiguration {
    public String worldName;
    public DroneSpawn droneSpawn;
    public List<WorldObject> objects;
    public WorldEnvironment environment;

    public WorldConfiguration() {
        this.worldName = "Default World";
        this.droneSpawn = new DroneSpawn();
        this.objects = createDefaultObjects();
        this.environment = new WorldEnvironment();
    }

    public WorldConfiguration(String worldName) {
        this.worldName = worldName;
        this.droneSpawn = new DroneSpawn();
        this.objects = createDefaultObjects();
        this.environment = new WorldEnvironment();
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

    /**
     * Represents the drone's spawn position and orientation.
     */
    public static class DroneSpawn {
        public double posX;
        public double posY;
        public double posZ;
        public double yaw;

        public DroneSpawn() {
            this.posX = 0;
            this.posY = -30; // Above ground
            this.posZ = 0;
            this.yaw = 0;
        }

        public DroneSpawn(double posX, double posY, double posZ, double yaw) {
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.yaw = yaw;
        }

        @Override
        public String toString() {
            return "DroneSpawn{" +
                    "pos=(" + posX + ", " + posY + ", " + posZ + ")" +
                    ", yaw=" + yaw +
                    '}';
        }
    }
    public static class WorldObject {
        public String name;
        public String type; // "tower", "box", "cylinder", etc.
        public double posX;
        public double posY;
        public double posZ;
        public double sizeX;
        public double sizeY;
        public double sizeZ;
        public String color; // RGB hex or color name

        public WorldObject() {}

        public WorldObject(String name, String type, double posX, double posY, double posZ,
                          double sizeX, double sizeY, double sizeZ, String color) {
            this.name = name;
            this.type = type;
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.sizeX = sizeX;
            this.sizeY = sizeY;
            this.sizeZ = sizeZ;
            this.color = color;
        }

        @Override
        public String toString() {
            return "WorldObject{" +
                    "name='" + name + '\'' +
                    ", type='" + type + '\'' +
                    ", pos=(" + posX + ", " + posY + ", " + posZ + ")" +
                    '}';
        }
    }

    /**
     * Represents environmental settings.
     */
    public static class WorldEnvironment {
        public String skyColor; // Sky color
        public String groundColor; // Ground color
        public double gravity; // Gravity value
        public boolean showGrid; // Show reference grid
        public boolean showTower; // Show reference tower

        public WorldEnvironment() {
            this.skyColor = "#87CEEB"; // Light sky blue
            this.groundColor = "#228B22"; // Forest green
            this.gravity = 9.81;
            this.showGrid = true;
            this.showTower = true;
        }

        @Override
        public String toString() {
            return "WorldEnvironment{" +
                    "skyColor='" + skyColor + '\'' +
                    ", groundColor='" + groundColor + '\'' +
                    ", gravity=" + gravity +
                    '}';
        }
    }
}
