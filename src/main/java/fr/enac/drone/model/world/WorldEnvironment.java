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

    /**
     * Creates the default environment values.
     */
    public WorldEnvironment() {
        this.skyColor = "#87CEEB"; // Light sky blue
        this.groundColor = "#228B22"; // Forest green
        this.gravity = 9.81;
        this.showGrid = true;
        this.showTower = true;
    }

    /**
     * Returns the sky color used by the 3D scene.
     *
     * @return RGB hex value or JavaFX color name
     */
    public String getSkyColor() {
        return skyColor;
    }

    /**
     * Sets the sky color used by the 3D scene.
     *
     * @param skyColor RGB hex value or JavaFX color name
     */
    public void setSkyColor(String skyColor) {
        if (skyColor == null || skyColor.trim().isEmpty()) {
            throw new IllegalArgumentException("Sky color cannot be null or empty");
        }
        this.skyColor = skyColor.trim();
    }

    /**
     * Returns the ground color used by default ground materials.
     *
     * @return RGB hex value or JavaFX color name
     */
    public String getGroundColor() {
        return groundColor;
    }

    /**
     * Sets the ground color used by default ground materials.
     *
     * @param groundColor RGB hex value or JavaFX color name
     */
    public void setGroundColor(String groundColor) {
        if (groundColor == null || groundColor.trim().isEmpty()) {
            throw new IllegalArgumentException("Ground color cannot be null or empty");
        }
        this.groundColor = groundColor.trim();
    }

    /**
     * Returns the gravity value configured for the world.
     *
     * @return gravity in meters per second squared
     */
    public double getGravity() {
        return gravity;
    }

    /**
     * Sets the gravity value configured for the world.
     *
     * @param gravity non-negative gravity in meters per second squared
     */
    public void setGravity(double gravity) {
        if (gravity < 0) {
            throw new IllegalArgumentException("Gravity cannot be negative");
        }
        this.gravity = gravity;
    }

    /**
     * Returns whether the minimap should draw a ground grid.
     *
     * @return {@code true} when the grid is visible
     */
    public boolean isShowGrid() {
        return showGrid;
    }

    /**
     * Sets whether the minimap should draw a ground grid.
     *
     * @param showGrid {@code true} to show the grid
     */
    public void setShowGrid(boolean showGrid) {
        this.showGrid = showGrid;
    }

    /**
     * Returns whether the reference tower should be shown by clients that support it.
     *
     * @return {@code true} when the tower is visible
     */
    public boolean isShowTower() {
        return showTower;
    }

    /**
     * Sets whether the reference tower should be shown by clients that support it.
     *
     * @param showTower {@code true} to show the tower
     */
    public void setShowTower(boolean showTower) {
        this.showTower = showTower;
    }

    /**
     * Returns a debug summary of the environment.
     *
     * @return environment colors and gravity
     */
    @Override
    public String toString() {
        return "WorldEnvironment{" +
                "skyColor='" + skyColor + '\'' +
                ", groundColor='" + groundColor + '\'' +
                ", gravity=" + gravity +
                '}';
    }
}
