package fr.enac.drone.model;


/**
 * Represents a static obstacle in the simulation world.
 * Each obstacle is modeled as a vertical cylinder defined by its position,
 * radius, and height.
 */
public class Obstacle {
    private final double x;
    private final double y;
    private final double z;
    private final double radius;
    private final double height;

    public Obstacle(double x, double z, double radius, double height) {
        this.x = x;
        this.z = z;
        this.radius = radius;
        this.height = height;

        this.y = 30 - height / 2;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public double getRadius() { return radius; }
    public double getHeight() { return height; }
}