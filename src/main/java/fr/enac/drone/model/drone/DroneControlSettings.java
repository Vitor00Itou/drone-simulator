package fr.enac.drone.model.drone;

import fr.enac.drone.model.Range;

/**
 * Centralizes user-adjustable flight control ranges.
 */
public final class DroneControlSettings {

    public static final Range YAW_SENSITIVITY =
            new Range(0.25, 2.0, 1.0);

    public static final Range MAX_HORIZONTAL_SPEED =
            new Range(5.0, 100.0, 25.0);

    public static final Range MAX_VERTICAL_SPEED =
            new Range(2.0, 40.0, 8.0);

    private DroneControlSettings() {
    }
}
