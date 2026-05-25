package fr.enac.drone.utils;

/**
 * Provides common mathematical helpers used by simulation and navigation code.
 */
public class MathUtils {

    /**
     * Creates a math helper instance.
     */
    public MathUtils() {
    }
    
    /**
     * Normalizes a heading angle to the range {@code [0, 360)} degrees.
     *
     * @param angleDegrees angle in degrees, positive or negative
     * @return equivalent heading in the range {@code [0, 360)}
     */
    public static double normalizeHeading(double angleDegrees) {
        double normalized = angleDegrees % 360;
        return normalized < 0 ? normalized + 360 : normalized;
    }

    /**
     * Calculates the Euclidean distance between two points on the world X/Z plane.
     *
     * @param x1 first point X coordinate
     * @param z1 first point Z coordinate
     * @param x2 second point X coordinate
     * @param z2 second point Z coordinate
     * @return distance between the two points
     */
    public static double distance2D(double x1, double z1, double x2, double z2) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
