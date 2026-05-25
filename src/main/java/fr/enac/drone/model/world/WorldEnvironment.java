package fr.enac.drone.model.world;

/**
 * Represents environmental settings for the world configuration.
 */
public class WorldEnvironment {
    private String skyColor;
    private String groundColor;
    private double gravity;
    private boolean showGrid;
    private boolean showTower;

    public WorldEnvironment() {
        this.skyColor = "#87CEEB"; // Light sky blue
        this.groundColor = "#228B22"; // Forest green
        this.gravity = 9.81;
        this.showGrid = true;
        this.showTower = true;
    }

    public String getSkyColor() {
        return skyColor;
    }

    public void setSkyColor(String skyColor) {
        if (skyColor == null || skyColor.trim().isEmpty()) {
            throw new IllegalArgumentException("Sky color cannot be null or empty");
        }
        this.skyColor = skyColor.trim();
    }

    public String getGroundColor() {
        return groundColor;
    }

    public void setGroundColor(String groundColor) {
        if (groundColor == null || groundColor.trim().isEmpty()) {
            throw new IllegalArgumentException("Ground color cannot be null or empty");
        }
        this.groundColor = groundColor.trim();
    }

    public double getGravity() {
        return gravity;
    }

    public void setGravity(double gravity) {
        if (gravity < 0) {
            throw new IllegalArgumentException("Gravity cannot be negative");
        }
        this.gravity = gravity;
    }

    public boolean isShowGrid() {
        return showGrid;
    }

    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }

    public boolean isShowTower() {
        return showTower;
    }

    public void setShowTower(boolean showTower) {
        this.showTower = showTower;
    }

    @Override
    public String toString() {
        return "WorldEnvironment{" +
                "skyColor='" + skyColor + '\'' +
                ", groundColor='" + groundColor + '\'' +
                ", gravity=" + gravity +
                '}';
    }
}
