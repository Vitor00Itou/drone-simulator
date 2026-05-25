package fr.enac.drone.model;

/**
 * Immutable X/Z world coordinate used by navigation and HUD overlays.
 *
 * @param x world X coordinate
 * @param z world Z coordinate
 */
public record WorldPosition2D(double x, double z) {
}
