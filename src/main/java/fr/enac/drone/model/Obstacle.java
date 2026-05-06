package fr.enac.drone.model;

/**
 * Static cylindrical object placed in the simulation world.
 */
public class Obstacle {
    private static final double GROUND_Y = 30.0;

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
        this.y = GROUND_Y - height / 2.0;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public double getRadius() { return radius; }
    public double getHeight() { return height; }
}
