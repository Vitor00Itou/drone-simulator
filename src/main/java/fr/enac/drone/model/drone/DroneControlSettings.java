package fr.enac.drone.model.drone;

import fr.enac.drone.model.Range;

/**
 * Defines the supported ranges for user-adjustable flight control settings.
 */
public final class DroneControlSettings {

    /** Allowed multiplier for yaw input sensitivity. */
    public static final Range YAW_SENSITIVITY =
            new Range(0.25, 2.0, 1.0);

    /** Allowed maximum horizontal flight speed in meters per second. */
    public static final Range MAX_HORIZONTAL_SPEED =
            new Range(5.0, 100.0, 25.0);

    /** Allowed maximum vertical flight speed in meters per second. */
    public static final Range MAX_VERTICAL_SPEED =
            new Range(2.0, 40.0, 8.0);

    /**
     * Prevents instantiation of this constants holder.
     */
    private DroneControlSettings() {
    }
}
