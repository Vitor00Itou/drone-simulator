package fr.enac.drone.utils;

/**
 * Utility class for common mathematical operations.
 */
public class MathUtils {
    
    /**
     * Normalizes an angle to be strictly between 0 and 359 degrees.
     */
    public static double normalizeHeading(double angleDegrees) {
        double normalized = angleDegrees % 360;
        return normalized < 0 ? normalized + 360 : normalized;
    }

    /**
     * Calculates the 2D Euclidean distance between two points (Pythagorean theorem).
     */
    public static double distance2D(double x1, double z1, double x2, double z2) {
        double dx = x2 - x1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
