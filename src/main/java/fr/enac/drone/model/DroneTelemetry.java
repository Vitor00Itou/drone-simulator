package fr.enac.drone.model;

/**
 * Immutable snapshot of the values displayed in the FPV telemetry HUD.
 */
public record DroneTelemetry(
        double altitudeMeters,
        double speedKmh,
        double headingDegrees,
        double distanceMeters
) {
}
