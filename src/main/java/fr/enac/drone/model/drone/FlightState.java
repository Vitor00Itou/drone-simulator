package fr.enac.drone.model.drone;

/**
 * Represents the current operational state of the drone.
 */
public enum FlightState {
    /** Motors are off and the drone is not accepting flight input. */
    DISARMED,
    /** The drone is ascending automatically to a safe hover altitude. */
    TAKING_OFF,
    /** The drone is fully controlled by the pilot or autopilot. */
    FLYING,
    /** The drone is climbing if needed and flying back to the spawn position. */
    RETURNING_HOME,
    /** The drone is descending automatically until ground contact. */
    LANDING,
    /** Motors are cut and the drone is falling under gravity. */
    FALLING
}
