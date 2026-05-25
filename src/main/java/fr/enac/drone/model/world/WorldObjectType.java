package fr.enac.drone.model.world;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

/**
 * Supported persisted shape types for world objects.
 */
public enum WorldObjectType {
    BOX("box"),
    CYLINDER("cylinder"),
    PLANE("plane");

    private final String serializedName;

    WorldObjectType(String serializedName) {
        this.serializedName = serializedName;
    }

    @JsonValue
    public String getSerializedName() {
        return serializedName;
    }

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
