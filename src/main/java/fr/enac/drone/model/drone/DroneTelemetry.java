package fr.enac.drone.model.drone;

/**
 * Immutable snapshot of the values displayed in the FPV telemetry HUD.
 */
public record DroneTelemetry(
        double altitudeMeters,
        double horizontalSpeedMs,
        double verticalSpeedMs,
        double headingDegrees,
        double distanceMeters
) {
}
