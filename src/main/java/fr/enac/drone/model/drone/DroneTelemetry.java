package fr.enac.drone.model.drone;

/**
 * Immutable snapshot of the values displayed in the FPV telemetry HUD.
 *
 * @param altitudeMeters altitude above the reference ground in meters
 * @param horizontalSpeedMs horizontal speed in meters per second
 * @param verticalSpeedMs vertical speed in meters per second
 * @param headingDegrees normalized heading in degrees
 * @param distanceMeters horizontal distance from the home position in meters
 * @param batteryPercent battery level from 0 to 100 percent
 * @param flightState current operational state of the drone
 */
public record DroneTelemetry(
    double altitudeMeters,
    double horizontalSpeedMs,
    double verticalSpeedMs,
    double headingDegrees,
    double distanceMeters,
    double batteryPercent,
    FlightState flightState
) {}
