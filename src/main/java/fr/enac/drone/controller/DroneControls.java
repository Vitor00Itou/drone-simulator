package fr.enac.drone.controller;

/**
 * Immutable record containing the flight control inputs calculated by the autopilot.
 * Each value represents an axis input normalized between -1.0 and 1.0.
 *
 * @param yaw   > 0 rotates right, < 0 rotates left
 * @param pitch > 0 moves forward, < 0 moves backward
 * @param roll  > 0 moves right (strafe), < 0 moves left (strafe)
 */
public record DroneControls(double yaw, double pitch, double roll) {}
