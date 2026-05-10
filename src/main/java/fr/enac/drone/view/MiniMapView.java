package fr.enac.drone.view;

import fr.enac.drone.model.drone.DroneModel;
import fr.enac.drone.model.world.WorldConfiguration;
import fr.enac.drone.model.world.WorldObject;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-down map of the simulation world, showing the drone, obstacles, and the flight trail.
 */
public class MiniMapView extends StackPane {
    private static final double SIZE = 180.0;
    private static final double MAP_INSET = 12.0;
    private static final int GRID_LINE_COUNT = 4;
    private static final double DRONE_MARKER_SIZE = 6.0;
    private static final double DEFAULT_WORLD_SIZE = 5000.0;

    // Trail settings
    private static final double TRAIL_MIN_DISTANCE = 1.0;   // world units between recorded points
    private static final int MAX_TRAIL_POINTS = 2000;       // prevent memory issues

    private final Canvas canvas = new Canvas(SIZE, SIZE);

    // Flight path history: each element is [worldX, worldZ]
    private final List<double[]> trailPoints = new ArrayList<>();
    private double lastTrailX, lastTrailZ;
    private boolean trailInitialized = false;

    public MiniMapView() {
        setMinSize(SIZE, SIZE);
        setPrefSize(SIZE, SIZE);
        setMaxSize(SIZE, SIZE);
        setBackground(new Background(new BackgroundFill(
                Color.rgb(67, 66, 80, 0.9),
                new CornerRadii(5),
                Insets.EMPTY
        )));
        setEffect(new DropShadow(12, Color.rgb(0, 0, 0, 0.28)));
        setMouseTransparent(true);

        getChildren().add(canvas);
    }

    /**
     * Repaints the minimap from the current model state.
     */
    public void render(DroneModel model, WorldConfiguration worldConfig) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double mapSize = SIZE - (MAP_INSET * 2);
        double center = SIZE / 2.0;
        double scale = mapSize / getWorldSize(worldConfig);

        gc.clearRect(0, 0, SIZE, SIZE);

        // 1. Update and record the drone's position for the trail
        updateTrail(model);

        // 2. Draw background
        drawMapSurface(gc, worldConfig);
        drawGrid(gc, mapSize);

        // 3. Draw the flight trail (semi-transparent, so it doesn't hide objects)
        drawTrail(gc, center, scale);

        // 4. Draw static world elements
        drawHomeMarker(gc, center);
        drawWorldObjects(gc, worldConfig, center, scale);

        // 5. Draw the drone marker on top of everything
        drawDroneMarker(gc, model, center, scale);
    }

    private void updateTrail(DroneModel model) {
        double currentX = model.getX();
        double currentZ = model.getZ();

        if (!trailInitialized) {
            trailPoints.add(new double[]{currentX, currentZ});
            lastTrailX = currentX;
            lastTrailZ = currentZ;
            trailInitialized = true;
            return;
        }

        double dx = currentX - lastTrailX;
        double dz = currentZ - lastTrailZ;
        if (dx * dx + dz * dz >= TRAIL_MIN_DISTANCE * TRAIL_MIN_DISTANCE) {
            // Limit trail length
            if (trailPoints.size() >= MAX_TRAIL_POINTS) {
                trailPoints.remove(0);
            }
            trailPoints.add(new double[]{currentX, currentZ});
            lastTrailX = currentX;
            lastTrailZ = currentZ;
        }
    }

    private void drawTrail(GraphicsContext gc, double center, double scale) {
        if (trailPoints.size() < 2) {
            return;
        }

        gc.setStroke(Color.rgb(255, 255, 100, 0.55)); // soft yellow, not too opaque
        gc.setLineWidth(1.5);
        gc.beginPath();

        for (int i = 0; i < trailPoints.size(); i++) {
            double[] pt = trailPoints.get(i);
            double sx = toScreenX(pt[0], center, scale);
            double sy = toScreenY(pt[1], center, scale);

            if (i == 0) {
                gc.moveTo(sx, sy);
            } else {
                gc.lineTo(sx, sy);
            }
        }
        gc.stroke();
    }

    // ----- The methods below remain unchanged from the original -----
    private void drawMapSurface(GraphicsContext gc, WorldConfiguration worldConfig) {
        Color groundColor = getGroundColor(worldConfig);
        gc.setFill(groundColor.deriveColor(0, 0.85, 0.72, 0.78));
        gc.fillRoundRect(MAP_INSET, MAP_INSET, SIZE - MAP_INSET * 2, SIZE - MAP_INSET * 2, 8, 8);

        gc.setStroke(groundColor.brighter().deriveColor(0, 0.75, 1.0, 0.34));
        gc.setLineWidth(1.2);
        gc.strokeRoundRect(MAP_INSET, MAP_INSET, SIZE - MAP_INSET * 2, SIZE - MAP_INSET * 2, 8, 8);
    }

    private void drawGrid(GraphicsContext gc, double mapSize) {
        gc.setLineWidth(1.0);

        for (int i = 1; i < GRID_LINE_COUNT; i++) {
            double position = MAP_INSET + (mapSize / GRID_LINE_COUNT) * i;
            double opacity = i == GRID_LINE_COUNT / 2 ? 0.22 : 0.11;

            gc.setStroke(Color.rgb(232, 230, 241, opacity));
            gc.strokeLine(position, MAP_INSET, position, SIZE - MAP_INSET);
            gc.strokeLine(MAP_INSET, position, SIZE - MAP_INSET, position);
        }
    }

    private void drawHomeMarker(GraphicsContext gc, double center) {
        gc.setStroke(Color.rgb(106, 202, 255, 0.9));
        gc.setLineWidth(1.4);
        gc.strokeLine(center - 5, center, center + 5, center);
        gc.strokeLine(center, center - 5, center, center + 5);
        gc.strokeOval(center - 4, center - 4, 8, 8);
    }

    private void drawWorldObjects(GraphicsContext gc, WorldConfiguration worldConfig, double center, double scale) {
        gc.setLineWidth(0.8);

        for (WorldObject object : worldConfig.getObjects()) {
            if (isGroundPlane(object)) {
                continue;
            }

            double x = toScreenX(object.getPosX(), center, scale);
            double y = toScreenY(object.getPosZ(), center, scale);

            if (!isInsideMap(x, y)) {
                continue;
            }

            Color objectColor = parseColor(object.getColor());
            gc.setFill(objectColor.deriveColor(0, 1, 1, 0.82));
            gc.setStroke(objectColor.brighter().deriveColor(0, 1, 1, 0.64));

            if ("cylinder".equals(object.getType())) {
                double radius = Math.max(2.2, object.getSizeX() * scale);
                gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
                gc.strokeOval(x - radius, y - radius, radius * 2, radius * 2);
            } else {
                double width = Math.max(3.0, object.getSizeX() * scale);
                double depth = Math.max(3.0, object.getSizeZ() * scale);
                gc.fillRect(x - width / 2.0, y - depth / 2.0, width, depth);
                gc.strokeRect(x - width / 2.0, y - depth / 2.0, width, depth);
            }
        }
    }

    private void drawDroneMarker(GraphicsContext gc, DroneModel model, double center, double scale) {
        double rawX = toScreenX(model.getX(), center, scale);
        double rawY = toScreenY(model.getZ(), center, scale);
        double x = clamp(rawX, MAP_INSET, SIZE - MAP_INSET);
        double y = clamp(rawY, MAP_INSET, SIZE - MAP_INSET);
        boolean outsideWorld = rawX != x || rawY != y;

        double yawRadians = Math.toRadians(model.getYaw());
        double forwardX = Math.sin(yawRadians);
        double forwardY = -Math.cos(yawRadians);
        double rightX = Math.cos(yawRadians);
        double rightY = Math.sin(yawRadians);

        double tipX = x + forwardX * DRONE_MARKER_SIZE;
        double tipY = y + forwardY * DRONE_MARKER_SIZE;
        double tailX = x - forwardX * DRONE_MARKER_SIZE * 0.75;
        double tailY = y - forwardY * DRONE_MARKER_SIZE * 0.75;
        double leftX = tailX - rightX * DRONE_MARKER_SIZE * 0.65;
        double leftY = tailY - rightY * DRONE_MARKER_SIZE * 0.65;
        double rightTipX = tailX + rightX * DRONE_MARKER_SIZE * 0.65;
        double rightTipY = tailY + rightY * DRONE_MARKER_SIZE * 0.65;

        gc.setFill(outsideWorld ? Color.rgb(255, 209, 102) : Color.rgb(255, 84, 84));
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.3);
        gc.fillPolygon(
                new double[] { tipX, leftX, rightTipX },
                new double[] { tipY, leftY, rightTipY },
                3
        );
        gc.strokePolygon(
                new double[] { tipX, leftX, rightTipX },
                new double[] { tipY, leftY, rightTipY },
                3
        );
    }

    private double toScreenX(double worldX, double center, double scale) {
        return center + worldX * scale;
    }

    private double toScreenY(double worldZ, double center, double scale) {
        return center - worldZ * scale;
    }

    private double getWorldSize(WorldConfiguration worldConfig) {
        double worldSize = DEFAULT_WORLD_SIZE;

        for (WorldObject object : worldConfig.getObjects()) {
            if (isGroundPlane(object)) {
                worldSize = Math.max(worldSize, Math.max(object.getSizeX(), object.getSizeZ()));
            }

            double halfWidth = object.getSizeX() / 2.0;
            double halfDepth = object.getSizeZ() / 2.0;
            double requiredSize = 2.0 * Math.max(
                    Math.abs(object.getPosX()) + halfWidth,
                    Math.abs(object.getPosZ()) + halfDepth
            );
            worldSize = Math.max(worldSize, requiredSize);
        }

        return worldSize;
    }

    private boolean isGroundPlane(WorldObject object) {
        return "plane".equals(object.getType());
    }

    private Color getGroundColor(WorldConfiguration worldConfig) {
        for (WorldObject object : worldConfig.getObjects()) {
            if (isGroundPlane(object)) {
                return parseColor(object.getColor());
            }
        }

        return parseColor(worldConfig.getEnvironment().getGroundColor());
    }

    private Color parseColor(String value) {
        try {
            return Color.web(value);
        } catch (IllegalArgumentException exception) {
            return Color.rgb(255, 176, 76);
        }
    }

    private boolean isInsideMap(double x, double y) {
        return x >= MAP_INSET && x <= SIZE - MAP_INSET && y >= MAP_INSET && y <= SIZE - MAP_INSET;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}