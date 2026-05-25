package fr.enac.drone.model.world;

/**
 * Represents a collision response direction and penetration depth.
 */
public class WorldCollision {
    private final double normalX;
    private final double normalY;
    private final double normalZ;
    private final double penetration;

    /**
     * Creates collision information for a single contact.
     *
     * @param normalX collision normal X component
     * @param normalY collision normal Y component
     * @param normalZ collision normal Z component
     * @param penetration overlap distance to resolve
     */
    public WorldCollision(double normalX, double normalY, double normalZ, double penetration) {
        this.normalX = normalX;
        this.normalY = normalY;
        this.normalZ = normalZ;
        this.penetration = penetration;
    }

    /**
     * Returns the collision normal X component.
     *
     * @return normal X component
     */
    public double getNormalX() {
        return normalX;
    }

    /**
     * Returns the collision normal Y component.
     *
     * @return normal Y component
     */
    public double getNormalY() { 
        return normalY; 
    }

    /**
     * Returns the collision normal Z component.
     *
     * @return normal Z component
     */
    public double getNormalZ() {
        return normalZ;
    }

    /**
     * Returns the penetration depth to correct.
     *
     * @return penetration depth
     */
    public double getPenetration() {
        return penetration;
    }

    /**
     * Returns whether the collision normal is primarily vertical.
     *
     * @return {@code true} when the Y normal component is significant
     */
    public boolean isVerticalCollision() {
        return Math.abs(normalY) > 0.0001;
    }
}
