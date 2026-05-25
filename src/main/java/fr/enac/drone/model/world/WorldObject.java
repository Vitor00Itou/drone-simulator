package fr.enac.drone.model.world;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

/**
 * Represents a single object inside the world configuration.
 */
public class WorldObject {
    private String name;
    private WorldObjectType type;
    private double posX;
    private double posY;
    private double posZ;
    private double sizeX;
    private double sizeY;
    private double sizeZ;
    private String color; // RGB hex or color name
    private String texture; // Optional texture file under src/main/resources/textures

    public WorldObject() {}

    public WorldObject(String name, String type, double posX, double posY, double posZ,
                       double sizeX, double sizeY, double sizeZ, String color) {
        this(name, WorldObjectType.fromSerializedName(type), posX, posY, posZ, sizeX, sizeY, sizeZ, color);
    }

    public WorldObject(String name, WorldObjectType type, double posX, double posY, double posZ,
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

    public WorldObjectType getType() {
        return type;
    }

    public void setType(WorldObjectType type) {
        this.type = Objects.requireNonNull(type, "Object type cannot be null");
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
        this.color = color.trim();
    }

    public String getTexture() {
        return texture;
    }

    public void setTexture(String texture) {
        this.texture = normalizeTexture(texture);
    }

    private String normalizeTexture(String texture) {
        if (texture == null || texture.trim().isEmpty()) {
            return null;
        }

        String normalizedTexture = texture.trim();
        if (normalizedTexture.startsWith("/textures/")) {
            normalizedTexture = normalizedTexture.substring("/textures/".length());
        } else if (normalizedTexture.startsWith("textures/")) {
            normalizedTexture = normalizedTexture.substring("textures/".length());
        } else if (normalizedTexture.startsWith("/")) {
            throw new IllegalArgumentException("Texture must be loaded from /textures resources: " + texture);
        }

        if (normalizedTexture.contains("\\")
            || normalizedTexture.contains("..")
            || normalizedTexture.contains("//")
            || normalizedTexture.startsWith("/")
            || normalizedTexture.endsWith("/")) {
            throw new IllegalArgumentException("Invalid texture resource path: " + texture);
        }

        return normalizedTexture;
    }

    @JsonIgnore
    public boolean isBox() {
        return type == WorldObjectType.BOX;
    }

    @JsonIgnore
    public boolean isCylinder() {
        return type == WorldObjectType.CYLINDER;
    }

    @JsonIgnore
    public boolean isPlane() {
        return type == WorldObjectType.PLANE;
    }

    @JsonIgnore
    public boolean isSolidObstacle() {
        return !isPlane();
    }

    @JsonIgnore
    public double getTopY() {
        return posY - sizeY / 2.0;
    }

    @JsonIgnore
    public double getBottomY() {
        return posY + sizeY / 2.0;
    }

    @JsonIgnore
    public double getHalfWidth() {
        return sizeX / 2.0;
    }

    @JsonIgnore
    public double getHalfDepth() {
        return sizeZ / 2.0;
    }

    @JsonIgnore
    public double getRadius() {
        if (!isCylinder()) {
            throw new IllegalStateException("Only cylindrical objects expose a radius");
        }
        return sizeX;
    }

    @JsonIgnore
    public double getFootprintHalfWidth() {
        return isCylinder() ? getRadius() : getHalfWidth();
    }

    @JsonIgnore
    public double getFootprintHalfDepth() {
        return isCylinder() ? getRadius() : getHalfDepth();
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
