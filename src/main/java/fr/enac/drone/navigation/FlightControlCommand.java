package fr.enac.drone.navigation;

/**
 * Normalized control command produced by navigation assistance.
 */
public record FlightControlCommand(double yawInput, double forwardInput, double lateralInput) {

    public static final FlightControlCommand NEUTRAL =
            new FlightControlCommand(0.0, 0.0, 0.0);
}
