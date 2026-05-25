package fr.enac.drone.model.world;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Objects;

/**
 * Represents a single persisted object inside the world configuration.
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
    /** RGB hex value or JavaFX color name used when no texture is available. */
    private String color;
    /** Optional texture file under {@code src/main/resources/textures}. */
    private String texture;

    /**
     * Creates an empty object for JSON deserialization.
     */
    public WorldObject() {}

    /**
     * Creates an object from a serialized type name.
     *
     * @param name non-empty object name
     * @param type serialized object type
     * @param posX center X coordinate
     * @param posY center Y coordinate
     * @param posZ center Z coordinate
     * @param sizeX width or cylinder radius
     * @param sizeY height
     * @param sizeZ depth or cylinder radius
     * @param color fallback color
     */
    public WorldObject(String name, String type, double posX, double posY, double posZ,
                       double sizeX, double sizeY, double sizeZ, String color) {
        this(name, WorldObjectType.fromSerializedName(type), posX, posY, posZ, sizeX, sizeY, sizeZ, color);
    }

    /**
     * Creates an object with a strongly typed shape.
     *
     * @param name non-empty object name
     * @param type object shape type
     * @param posX center X coordinate
     * @param posY center Y coordinate
     * @param posZ center Z coordinate
     * @param sizeX width or cylinder radius
     * @param sizeY height
     * @param sizeZ depth or cylinder radius
     * @param color fallback color
     */
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

    /**
     * Returns the object name.
     *
     * @return object name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the object name.
     *
     * @param name non-empty object name
     */
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Object name cannot be null or empty");
        }
        this.name = name;
    }

    /**
     * Returns the object shape type.
     *
     * @return shape type
     */
    public WorldObjectType getType() {
        return type;
    }

    /**
     * Sets the object shape type.
     *
     * @param type non-null shape type
     */
    public void setType(WorldObjectType type) {
        this.type = Objects.requireNonNull(type, "Object type cannot be null");
    }

    /**
     * Returns the center X coordinate.
     *
     * @return center X coordinate
     */
    public double getPosX() {
        return posX;
    }

    /**
     * Sets the center X coordinate.
     *
     * @param posX center X coordinate
     */
    public void setPosX(double posX) {
        this.posX = posX;
    }

    /**
     * Returns the center Y coordinate.
     *
     * @return center Y coordinate
     */
    public double getPosY() {
        return posY;
    }

    /**
     * Sets the center Y coordinate.
     *
     * @param posY center Y coordinate
     */
    public void setPosY(double posY) {
        this.posY = posY;
    }

    /**
     * Returns the center Z coordinate.
     *
     * @return center Z coordinate
     */
    public double getPosZ() {
        return posZ;
    }

    /**
     * Sets the center Z coordinate.
     *
     * @param posZ center Z coordinate
     */
    public void setPosZ(double posZ) {
        this.posZ = posZ;
    }

    /**
     * Returns the object width, or cylinder radius for cylindrical objects.
     *
     * @return positive X size
     */
    public double getSizeX() {
        return sizeX;
    }

    /**
     * Sets the object width, or cylinder radius for cylindrical objects.
     *
     * @param sizeX positive X size
     */
    public void setSizeX(double sizeX) {
        if (sizeX <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
        this.sizeX = sizeX;
    }

    /**
     * Returns the object height.
     *
     * @return positive Y size
     */
    public double getSizeY() {
        return sizeY;
    }

    /**
     * Sets the object height.
     *
     * @param sizeY positive Y size
     */
    public void setSizeY(double sizeY) {
        if (sizeY <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
        this.sizeY = sizeY;
    }

    /**
     * Returns the object depth, or cylinder radius for cylindrical objects.
     *
     * @return positive Z size
     */
    public double getSizeZ() {
        return sizeZ;
    }

    /**
     * Sets the object depth, or cylinder radius for cylindrical objects.
     *
     * @param sizeZ positive Z size
     */
    public void setSizeZ(double sizeZ) {
        if (sizeZ <= 0) {
            throw new IllegalArgumentException("Size must be positive");
        }
        this.sizeZ = sizeZ;
    }

    /**
     * Returns the fallback color used for rendering.
     *
     * @return RGB hex value or JavaFX color name
     */
    public String getColor() {
        return color;
    }

    /**
     * Sets the fallback rendering color.
     *
     * @param color RGB hex value or JavaFX color name
     */
    public void setColor(String color) {
        if (color == null || color.trim().isEmpty()) {
            throw new IllegalArgumentException("Color cannot be null or empty");
        }
        this.color = color.trim();
    }

    /**
     * Returns the optional texture resource name.
     *
     * @return texture file name, or {@code null} when no texture is configured
     */
    public String getTexture() {
        return texture;
    }

    /**
     * Sets the optional texture resource name.
     *
     * @param texture resource name under {@code /textures}, or {@code null}
     */
    public void setTexture(String texture) {
        this.texture = normalizeTexture(texture);
    }

    /**
     * Validates and normalizes a texture path to a resource-local file name.
     *
     * @param texture texture path or file name
     * @return normalized texture file name, or {@code null}
     */
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

    /**
     * Returns whether this object is a box.
     *
     * @return {@code true} for box objects
     */
    @JsonIgnore
    public boolean isBox() {
        return type == WorldObjectType.BOX;
    }

    /**
     * Returns whether this object is a cylinder.
     *
     * @return {@code true} for cylindrical objects
     */
    @JsonIgnore
    public boolean isCylinder() {
        return type == WorldObjectType.CYLINDER;
    }

    /**
     * Returns whether this object is a plane.
     *
     * @return {@code true} for ground-plane objects
     */
    @JsonIgnore
    public boolean isPlane() {
        return type == WorldObjectType.PLANE;
    }

    /**
     * Returns whether this object should participate in obstacle collision checks.
     *
     * @return {@code true} for non-plane objects
     */
    @JsonIgnore
    public boolean isSolidObstacle() {
        return !isPlane();
    }

    /**
     * Returns the top Y coordinate.
     *
     * @return top surface Y coordinate
     */
    @JsonIgnore
    public double getTopY() {
        return posY - sizeY / 2.0;
    }

    /**
     * Returns the bottom Y coordinate.
     *
     * @return bottom surface Y coordinate
     */
    @JsonIgnore
    public double getBottomY() {
        return posY + sizeY / 2.0;
    }

    /**
     * Returns half the object width.
     *
     * @return half-width on the X axis
     */
    @JsonIgnore
    public double getHalfWidth() {
        return sizeX / 2.0;
    }

    /**
     * Returns half the object depth.
     *
     * @return half-depth on the Z axis
     */
    @JsonIgnore
    public double getHalfDepth() {
        return sizeZ / 2.0;
    }

    /**
     * Returns the cylinder radius.
     *
     * @return cylinder radius
     * @throws IllegalStateException if this object is not cylindrical
     */
    @JsonIgnore
    public double getRadius() {
        if (!isCylinder()) {
            throw new IllegalStateException("Only cylindrical objects expose a radius");
        }
        return sizeX;
    }

    /**
     * Returns the half-width used for top-down footprint checks.
     *
     * @return cylinder radius or box half-width
     */
    @JsonIgnore
    public double getFootprintHalfWidth() {
        return isCylinder() ? getRadius() : getHalfWidth();
    }

    /**
     * Returns the half-depth used for top-down footprint checks.
     *
     * @return cylinder radius or box half-depth
     */
    @JsonIgnore
    public double getFootprintHalfDepth() {
        return isCylinder() ? getRadius() : getHalfDepth();
    }

    /**
     * Returns a debug summary of the object.
     *
     * @return object name, type, and position
     */
    @Override
    public String toString() {
        return "WorldObject{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", pos=(" + posX + ", " + posY + ", " + posZ + ")" +
                '}';
    }
}
