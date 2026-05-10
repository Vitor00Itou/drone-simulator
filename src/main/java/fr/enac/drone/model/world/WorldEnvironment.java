package fr.enac.drone.model.world;

import javafx.scene.paint.Color;

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
        validateColor(skyColor);
        this.skyColor = skyColor;
    }

    public String getGroundColor() {
        return groundColor;
    }

    public void setGroundColor(String groundColor) {
        if (groundColor == null || groundColor.trim().isEmpty()) {
            throw new IllegalArgumentException("Ground color cannot be null or empty");
        }
        validateColor(groundColor);
        this.groundColor = groundColor;
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

    private void validateColor(String color) {
        try {
            Color.web(color);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid color format: " + color, e);
        }
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
