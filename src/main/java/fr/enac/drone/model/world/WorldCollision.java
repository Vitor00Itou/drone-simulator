package fr.enac.drone.model.world;

/**
 * Represents a collision response direction and penetration depth.
 */
public class WorldCollision {
    private final double normalX;
    private final double normalY;
    private final double normalZ;
    private final double penetration;

    public WorldCollision(double normalX, double normalY, double normalZ, double penetration) {
        this.normalX = normalX;
        this.normalY = normalY;
        this.normalZ = normalZ;
        this.penetration = penetration;
    }

    public double getNormalX() {
        return normalX;
    }

    public double getNormalY() { 
        return normalY; 
    }

    public double getNormalZ() {
        return normalZ;
    }

    public double getPenetration() {
        return penetration;
    }

    public boolean isVerticalCollision() {
        return Math.abs(normalY) > 0.0001;
    }
}