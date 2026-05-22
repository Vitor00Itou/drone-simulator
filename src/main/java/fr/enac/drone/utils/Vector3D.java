package fr.enac.drone.utils;

/**
 * Simple 3D vector class for position and movement calculations.
 * Provides basic vector operations like addition, subtraction, and distance calculations.
 */
public class Vector3D {
    
    /** X coordinate in world space */
    public double x;
    
    /** Y coordinate (altitude) in world space */
    public double y;
    
    /** Z coordinate in world space */
    public double z;
    
    /**
     * Constructs a new 3D vector with the specified coordinates.
     * 
     * @param x the X coordinate
     * @param y the Y coordinate (altitude)
     * @param z the Z coordinate
     */
    public Vector3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    /**
     * Adds another vector to this vector component-wise.
     * 
     * @param v the vector to add
     * @return a new vector representing the sum
     */
    public Vector3D add(Vector3D v) {
        return new Vector3D(x + v.x, y + v.y, z + v.z);
    }
    
    /**
     * Subtracts another vector from this vector component-wise.
     * 
     * @param v the vector to subtract
     * @return a new vector representing the difference
     */
    public Vector3D subtract(Vector3D v) {
        return new Vector3D(x - v.x, y - v.y, z - v.z);
    }
    
    /**
     * Calculates the 3D Euclidean distance to another vector.
     * 
     * @param v the target vector
     * @return the 3D distance between the two points
     */
    public double distanceTo(Vector3D v) {
        double dx = x - v.x;
        double dy = y - v.y;
        double dz = z - v.z;
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }
    
    /**
     * Calculates the 2D Euclidean distance (ignoring Y/altitude) to another vector.
     * Useful for horizontal navigation calculations.
     * 
     * @param v the target vector
     * @return the 2D horizontal distance between the two points
     */
    public double distance2D(Vector3D v) {
        double dx = x - v.x;
        double dz = z - v.z;
        return Math.sqrt(dx*dx + dz*dz);
    }
    
    /**
     * Returns a string representation of the vector with 2 decimal precision.
     * 
     * @return formatted string like "(x, y, z)"
     */
    @Override
    public String toString() {
        return String.format("(%.2f, %.2f, %.2f)", x, y, z);
    }
}