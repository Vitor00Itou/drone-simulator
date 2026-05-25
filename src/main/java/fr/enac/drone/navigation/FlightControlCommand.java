package fr.enac.drone.navigation;

/**
 * Normalized control command produced by navigation assistance.
 *
 * @param yawInput normalized yaw input in the range {@code [-1, 1]}
 * @param forwardInput normalized forward input in the range {@code [-1, 1]}
 * @param lateralInput normalized lateral input in the range {@code [-1, 1]}
 */
public record FlightControlCommand(double yawInput, double forwardInput, double lateralInput) {

    /** Command that leaves all flight axes neutral. */
    public static final FlightControlCommand NEUTRAL =
            new FlightControlCommand(0.0, 0.0, 0.0);
}
