package fr.enac.drone.input;

import javafx.scene.input.KeyCode;

import java.util.Locale;

/**
 * Keyboard mappings supported by the flight controls.
 */
public enum KeyboardLayout {
    QWERTY(KeyCode.A, KeyCode.W),
    AZERTY(KeyCode.Q, KeyCode.Z);

    private final KeyCode yawLeftKey;
    private final KeyCode verticalUpKey;

    KeyboardLayout(KeyCode yawLeftKey, KeyCode verticalUpKey) {
        this.yawLeftKey = yawLeftKey;
        this.verticalUpKey = verticalUpKey;
    }

    public KeyCode getYawLeftKey() {
        return yawLeftKey;
    }

    public KeyCode getVerticalUpKey() {
        return verticalUpKey;
    }

    public String getYawLeftLabel() {
        return yawLeftKey.getName();
    }

    public String getVerticalUpLabel() {
        return verticalUpKey.getName();
    }

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
