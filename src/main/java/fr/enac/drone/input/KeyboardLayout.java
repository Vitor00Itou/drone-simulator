package fr.enac.drone.input;

import javafx.scene.input.KeyCode;

import java.util.Locale;

/**
 * Keyboard mappings supported by the flight controls.
 */
public enum KeyboardLayout {
    /** Standard QWERTY controls using A/W for yaw-left and climb. */
    QWERTY(KeyCode.A, KeyCode.W),
    /** AZERTY controls using Q/Z for yaw-left and climb. */
    AZERTY(KeyCode.Q, KeyCode.Z);

    private final KeyCode yawLeftKey;
    private final KeyCode verticalUpKey;

    /**
     * Creates a keyboard layout definition.
     *
     * @param yawLeftKey key used to yaw left
     * @param verticalUpKey key used to climb
     */
    KeyboardLayout(KeyCode yawLeftKey, KeyCode verticalUpKey) {
        this.yawLeftKey = yawLeftKey;
        this.verticalUpKey = verticalUpKey;
    }

    /**
     * Returns the key used to yaw left.
     *
     * @return yaw-left key code
     */
    public KeyCode getYawLeftKey() {
        return yawLeftKey;
    }

    /**
     * Returns the key used to climb.
     *
     * @return climb key code
     */
    public KeyCode getVerticalUpKey() {
        return verticalUpKey;
    }

    /**
     * Returns a display label for the yaw-left key.
     *
     * @return human-readable yaw-left key label
     */
    public String getYawLeftLabel() {
        return yawLeftKey.getName();
    }

    /**
     * Returns a display label for the climb key.
     *
     * @return human-readable climb key label
     */
    public String getVerticalUpLabel() {
        return verticalUpKey.getName();
    }

    /**
     * Parses a keyboard layout name.
     *
     * @param value layout name to parse
     * @return matching layout, or {@link #QWERTY} when the value is blank
     * @throws IllegalArgumentException if the layout name is not supported
     */
    public static KeyboardLayout fromName(String value) {
        if (value == null || value.isBlank()) {
            return QWERTY;
        }

        String normalizedValue = value.trim().toUpperCase(Locale.ROOT);
        for (KeyboardLayout layout : values()) {
            if (layout.name().equals(normalizedValue)) {
                return layout;
            }
        }

        throw new IllegalArgumentException("Unsupported keyboard layout: " + value);
    }
}
