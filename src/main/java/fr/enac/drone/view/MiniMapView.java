package fr.enac.drone.view;

import fr.enac.drone.model.drone.DroneModel;
import fr.enac.drone.model.world.WorldConfiguration;
import fr.enac.drone.model.world.WorldObject;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.Paint;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Top-down map of the simulation world, showing the drone, obstacles,
 * flight trail and an optional navigation target.
 */
public class MiniMapView extends StackPane {
    private static final double SIZE = 180.0;
    private static final double MAP_INSET = 12.0;
    private static final int GRID_LINE_COUNT = 4;
    private static final double DRONE_MARKER_SIZE = 6.0;
    private static final double DEFAULT_WORLD_SIZE = 5000.0;
    private static final double GROUND_TEXTURE_TILE_SIZE = 500.0;
    private static final Color FALLBACK_MATERIAL_COLOR = Color.web("#B0B0B0");

    private static final double TRAIL_MIN_DISTANCE = 1.0;
    private static final int MAX_TRAIL_POINTS = 2000;

    private final Canvas canvas = new Canvas(SIZE, SIZE);
    private final SceneMaterialFactory materialFactory = new SceneMaterialFactory();
    private final List<double[]> trailPoints = new ArrayList<>();
    private double lastTrailX, lastTrailZ;
    private boolean trailInitialized = false;

    private double currentWorldSize = DEFAULT_WORLD_SIZE;
    private double currentScale = 1.0;
    private double currentCenter = SIZE / 2.0;

    private double[] targetWorld = null;
    private Consumer<double[]> onTargetClicked;

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

        getChildren().add(canvas);
        setOnMouseClicked(this::handleMapClick);
    }

    public void setOnTargetClicked(Consumer<double[]> callback) {
        this.onTargetClicked = callback;
    }

    public void setTarget(double worldX, double worldZ) {
        this.targetWorld = new double[]{worldX, worldZ};
    }

    public void clearTarget() {
        this.targetWorld = null;
    }

    public List<double[]> getTrail() {
        return new ArrayList<>(trailPoints);
    }

    private void handleMapClick(MouseEvent event) {
        if (onTargetClicked == null) return;
        double mouseX = event.getX();
        double mouseY = event.getY();
        double worldX = (mouseX - currentCenter) / currentScale;
        double worldZ = (currentCenter - mouseY) / currentScale;
        double[] target = new double[]{worldX, worldZ};
        setTarget(worldX, worldZ);
        onTargetClicked.accept(target);
    }

    /**
     * Repaints the minimap from the current model state.
     */
    public void render(DroneModel model, WorldConfiguration worldConfig) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double mapSize = SIZE - (MAP_INSET * 2);
        double center = SIZE / 2.0;

        currentWorldSize = getWorldSize(worldConfig);
        currentScale = mapSize / currentWorldSize;
        currentCenter = center;

        gc.clearRect(0, 0, SIZE, SIZE);

        updateTrail(model);
        drawMapSurface(gc, worldConfig, currentScale);
        drawGrid(gc, mapSize);
        drawTrail(gc, center, currentScale);
        drawHomeMarker(gc, center);
        drawWorldObjects(gc, worldConfig, center, currentScale);
        drawTarget(gc, center, currentScale);
        drawDroneMarker(gc, model, center, currentScale);
    }

    private void updateTrail(DroneModel model) {
        double cx = model.getX();
        double cz = model.getZ();

        if (!trailInitialized) {
            trailPoints.add(new double[]{cx, cz});
            lastTrailX = cx;
            lastTrailZ = cz;
            trailInitialized = true;
            return;
        }

        double dx = cx - lastTrailX;
        double dz = cz - lastTrailZ;
        if (dx * dx + dz * dz >= TRAIL_MIN_DISTANCE * TRAIL_MIN_DISTANCE) {
            if (trailPoints.size() >= MAX_TRAIL_POINTS) {
                trailPoints.remove(0);
            }
            trailPoints.add(new double[]{cx, cz});
            lastTrailX = cx;
            lastTrailZ = cz;
        }
    }

    private void drawTrail(GraphicsContext gc, double center, double scale) {
        if (trailPoints.size() < 2) return;

        gc.setStroke(Color.rgb(255, 255, 100, 0.55));
        gc.setLineWidth(1.5);
        gc.beginPath();
        for (int i = 0; i < trailPoints.size(); i++) {
            double[] pt = trailPoints.get(i);
            double sx = toScreenX(pt[0], center, scale);
            double sy = toScreenY(pt[1], center, scale);
            if (i == 0) gc.moveTo(sx, sy);
            else gc.lineTo(sx, sy);
        }
        gc.stroke();
    }

    private void drawTarget(GraphicsContext gc, double center, double scale) {
        if (targetWorld == null) return;

        double sx = toScreenX(targetWorld[0], center, scale);
        double sy = toScreenY(targetWorld[1], center, scale);

        if (!isInsideMap(sx, sy)) return;

        double size = 6.0;
        gc.setStroke(Color.YELLOW);
        gc.setLineWidth(2.0);
        gc.strokeLine(sx - size, sy, sx + size, sy);
        gc.strokeLine(sx, sy - size, sx, sy + size);
    }

    private void drawMapSurface(GraphicsContext gc, WorldConfiguration worldConfig, double scale) {
        WorldObject groundPlane = getGroundPlane(worldConfig);
        Color groundColor = getGroundColor(worldConfig, groundPlane);

        gc.setFill(getGroundPaint(groundPlane, groundColor, scale));
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

            if ("cylinder".equals(object.getType())) {
                double radius = Math.max(2.2, object.getSizeX() * scale);
                double diameter = radius * 2.0;
                Paint objectPaint = getObjectPaint(object, x - radius, y - radius, diameter, diameter);
                Color strokeColor = getObjectStrokeColor(object);

                gc.setFill(objectPaint);
                gc.setStroke(strokeColor);
                gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
                gc.strokeOval(x - radius, y - radius, radius * 2, radius * 2);
            } else {
                double width = Math.max(3.0, object.getSizeX() * scale);
                double depth = Math.max(3.0, object.getSizeZ() * scale);
                Paint objectPaint = getObjectPaint(object, x - width / 2.0, y - depth / 2.0, width, depth);
                Color strokeColor = getObjectStrokeColor(object);

                gc.setFill(objectPaint);
                gc.setStroke(strokeColor);
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

    private WorldObject getGroundPlane(WorldConfiguration worldConfig) {
        for (WorldObject object : worldConfig.getObjects()) {
            if (isGroundPlane(object)) {
                return object;
            }
        }

        return null;
    }

    private Paint getGroundPaint(WorldObject groundPlane, Color fallbackColor, double scale) {
        if (groundPlane == null) {
            return fallbackColor.deriveColor(0, 0.85, 0.72, 0.78);
        }

        Optional<Image> texture = materialFactory.getTextureImage(groundPlane);
        if (texture.isEmpty()) {
            return fallbackColor.deriveColor(0, 0.85, 0.72, 0.78);
        }

        double patternSize = Math.max(8.0, GROUND_TEXTURE_TILE_SIZE * scale);
        return new ImagePattern(texture.get(), MAP_INSET, MAP_INSET, patternSize, patternSize, false);
    }

    private Paint getObjectPaint(WorldObject object, double x, double y, double width, double height) {
        Optional<Image> texture = materialFactory.getTextureImage(object);
        if (texture.isPresent()) {
            return new ImagePattern(texture.get(), x, y, width, height, false);
        }

        return parseColor(object.getColor());
    }

    private Color getObjectStrokeColor(WorldObject object) {
        Optional<Image> texture = materialFactory.getTextureImage(object);
        if (texture.isPresent()) {
            return Color.rgb(245, 245, 245, 0.7);
        }

        return parseColor(object.getColor()).brighter().deriveColor(0, 1, 1, 0.64);
    }

    private Color getGroundColor(WorldConfiguration worldConfig, WorldObject groundPlane) {
        if (groundPlane != null) {
            return parseColor(groundPlane.getColor());
        }

        return parseColor(worldConfig.getEnvironment().getGroundColor());
    }

    private Color parseColor(String value) {
        try {
            return Color.web(value);
        } catch (IllegalArgumentException exception) {
            return FALLBACK_MATERIAL_COLOR;
        }
    }

    private boolean isInsideMap(double x, double y) {
        return x >= MAP_INSET && x <= SIZE - MAP_INSET && y >= MAP_INSET && y <= SIZE - MAP_INSET;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
