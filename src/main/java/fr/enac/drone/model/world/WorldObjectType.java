package fr.enac.drone.model.world;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Supported persisted shape types for world objects.
 */
public enum WorldObjectType {
    /** Rectangular obstacle or tile. */
    BOX("box"),
    /** Cylindrical obstacle or platform. */
    CYLINDER("cylinder"),
    /** Ground plane. */
    PLANE("plane");

    private final String serializedName;

    /**
     * Creates a shape type with its persisted name.
     *
     * @param serializedName JSON representation
     */
    WorldObjectType(String serializedName) {
        this.serializedName = serializedName;
    }

    /**
     * Returns the JSON representation of this type.
     *
     * @return serialized type name
     */
    @JsonValue
    public String getSerializedName() {
        return serializedName;
    }

    /**
     * Parses a persisted shape type.
     *
     * @param value serialized type name
     * @return matching object type
     * @throws IllegalArgumentException if the value is blank or unsupported
     */
    @JsonCreator
    public static WorldObjectType fromSerializedName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Object type cannot be null or empty");
        }

        String normalizedValue = value.trim().toLowerCase(Locale.ROOT);
        for (WorldObjectType type : values()) {
            if (type.serializedName.equals(normalizedValue)) {
                return type;
            }
        }

        throw new IllegalArgumentException("Invalid object type: " + value);
    }
}
