package fr.enac.drone.model.drone;

/**
 * Represents the current operational state of the drone.
 */
public enum FlightState {
    DISARMED,   // Motors off, sitting on the ground
    TAKING_OFF, // Ascending automatically to a safe hover altitude
    FLYING,     // Fully controlled by the user or autopilot
    RETURNING_HOME, // Ascending to a safe altitude and flying back directly to the spawn position
    LANDING,    // Descending automatically until ground contact
    FALLING     // Emergency stop, motors cut, falling under gravity
}
