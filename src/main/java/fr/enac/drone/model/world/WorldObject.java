package fr.enac.drone.model.world;

import javafx.scene.paint.Color;

/**
 * Represents a single object inside the world configuration.
 */
public class WorldObject {
    private String name;
    private String type; // "box", "cylinder", "plane"
    private double posX;
    private double posY;
    private double posZ;
    private double sizeX;
    private double sizeY;
    private double sizeZ;
    private String color; // RGB hex or color name

    public WorldObject() {}

    public WorldObject(String name, String type, double posX, double posY, double posZ,
                       double sizeX, double sizeY, double sizeZ, String color) {
        setName(name);
        setType(type);
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
        setSizeX(sizeX);
        setSizeY(sizeY);
        setSizeZ(sizeZ);
        setColor(color);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Object name cannot be null or empty");
        }
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Object type cannot be null or empty");
        }
        String lowerType = type.toLowerCase();
        if (!lowerType.matches("box|cylinder|plane")) {
            throw new IllegalArgumentException("Invalid object type: " + type + ". Must be box, cylinder, or plane");
        }
        this.type = lowerType;
    }

    public double getPosX() {
        return posX;
    }

    public void setPosX(double posX) {
        this.posX = posX;
    }

    public double getPosY() {
        return posY;
    }

    public void setPosY(double posY) {
        this.posY = posY;
    }

    public double getPosZ() {
        return posZ;
    }

    public void setPosZ(double posZ) {
        this.posZ = posZ;
    }

    public double getSizeX() {
        return sizeX;
    }

    public void setSizeX(double sizeX) {
        if (sizeX <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
        this.sizeX = sizeX;
    }

    public double getSizeY() {
        return sizeY;
    }

    public void setSizeY(double sizeY) {
        if (sizeY <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
        this.sizeY = sizeY;
    }

    public double getSizeZ() {
        return sizeZ;
    }

    public void setSizeZ(double sizeZ) {
        if (sizeZ <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
        this.sizeZ = sizeZ;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        if (color == null || color.trim().isEmpty()) {
            throw new IllegalArgumentException("Color cannot be null or empty");
        }
        validateColor(color);
        this.color = color;
    }

    private void validateColor(String color) {
        try {
            Color.web(color);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid color format: " + color, e);
        }
    }

    public String getTypeCaseSensitive() {
        return type;
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
