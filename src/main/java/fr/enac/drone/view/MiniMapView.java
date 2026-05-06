package fr.enac.drone.view;

import fr.enac.drone.model.DroneModel;
import fr.enac.drone.model.Obstacle;
import fr.enac.drone.model.World;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/**
 * Top-down map of the simulation world, showing the drone and obstacles.
 */
public class MiniMapView extends StackPane {
    private static final double SIZE = 180.0;
    private static final double MAP_INSET = 12.0;
    private static final int GRID_LINE_COUNT = 4;
    private static final double DRONE_MARKER_SIZE = 6.0;

    private final Canvas canvas = new Canvas(SIZE, SIZE);

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
    public void render(DroneModel model) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        World world = model.getWorld();
        double mapSize = SIZE - (MAP_INSET * 2);
        double center = SIZE / 2.0;
        double scale = mapSize / world.getSize();

        gc.clearRect(0, 0, SIZE, SIZE);
        drawMapSurface(gc);
        drawGrid(gc, mapSize);
        drawHomeMarker(gc, center);
        drawObstacles(gc, world, center, scale);
        drawDroneMarker(gc, model, center, scale);
    }

    private void drawMapSurface(GraphicsContext gc) {
        gc.setFill(Color.rgb(24, 27, 32, 0.78));
        gc.fillRoundRect(MAP_INSET, MAP_INSET, SIZE - MAP_INSET * 2, SIZE - MAP_INSET * 2, 8, 8);

        gc.setStroke(Color.rgb(232, 230, 241, 0.32));
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

    private void drawObstacles(GraphicsContext gc, World world, double center, double scale) {
        gc.setFill(Color.rgb(255, 176, 76, 0.82));
        gc.setStroke(Color.rgb(255, 231, 173, 0.62));
        gc.setLineWidth(0.8);

        for (Obstacle obstacle : world.getObstacles()) {
            double x = toScreenX(obstacle.getX(), center, scale);
            double y = toScreenY(obstacle.getZ(), center, scale);

            if (!isInsideMap(x, y)) {
                continue;
            }

            double radius = Math.max(2.2, obstacle.getRadius() * scale);
            gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
            gc.strokeOval(x - radius, y - radius, radius * 2, radius * 2);
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

    private boolean isInsideMap(double x, double y) {
        return x >= MAP_INSET && x <= SIZE - MAP_INSET && y >= MAP_INSET && y <= SIZE - MAP_INSET;
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
