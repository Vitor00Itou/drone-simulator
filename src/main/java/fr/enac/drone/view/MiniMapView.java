package fr.enac.drone.view;

import fr.enac.drone.model.drone.DroneModel;
import fr.enac.drone.model.world.WorldConfiguration;
import fr.enac.drone.model.world.WorldObject;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class MiniMapView extends StackPane {
    private static final double SIZE = 180.0;
    private static final double MAP_INSET = 12.0;
    private static final int GRID_LINE_COUNT = 4;
    private static final double DRONE_MARKER_SIZE = 6.0;
    private static final double DEFAULT_WORLD_SIZE = 5000.0;

    private static final double TRAIL_MIN_DISTANCE = 1.0;
    private static final int MAX_TRAIL_POINTS = 2000;

    private final Canvas canvas = new Canvas(SIZE, SIZE);
    private final List<double[]> trailPoints = new ArrayList<>();
    private double lastTrailX, lastTrailZ;
    private boolean trailInitialized = false;

    // Pour conversion inverse (clic -> monde)
    private double currentWorldSize = DEFAULT_WORLD_SIZE;
    private double currentScale = 1.0;
    private double currentCenter = SIZE / 2.0;

    // Cible actuelle (null si pas de cible)
    private double[] targetWorld = null;

    private Consumer<double[]> onTargetClicked; // callback avec {x, z}

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
        // Pas de setMouseTransparent, on veut capturer les clics

        getChildren().add(canvas);

        // Gestion du clic
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

    private void handleMapClick(MouseEvent event) {
        if (onTargetClicked == null) return;
        double mouseX = event.getX();
        double mouseY = event.getY();
        // Conversion écran -> monde (XZ)
        double worldX = (mouseX - currentCenter) / currentScale;
        double worldZ = (currentCenter - mouseY) / currentScale;
        double[] target = new double[]{worldX, worldZ};
        setTarget(worldX, worldZ);
        onTargetClicked.accept(target);
    }

    public void render(DroneModel model, WorldConfiguration worldConfig) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double mapSize = SIZE - (MAP_INSET * 2);
        double center = SIZE / 2.0;

        currentWorldSize = getWorldSize(worldConfig);
        currentScale = mapSize / currentWorldSize;
        currentCenter = center;

        gc.clearRect(0, 0, SIZE, SIZE);

        updateTrail(model);
        drawMapSurface(gc, worldConfig);
        drawGrid(gc, mapSize);
        drawTrail(gc, center, currentScale);
        drawHomeMarker(gc, center);
        drawWorldObjects(gc, worldConfig, center, currentScale);
        drawTarget(gc, center, currentScale);          // croix jaune
        drawDroneMarker(gc, model, center, currentScale);
    }

    // ---------- Trace historique ----------
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

    // ---------- Croix cible (jaune) ----------
    private void drawTarget(GraphicsContext gc, double center, double scale) {
        if (targetWorld == null) return;

        double sx = toScreenX(targetWorld[0], center, scale);
        double sy = toScreenY(targetWorld[1], center, scale);

        // Vérifier si dans la zone visible
        if (!isInsideMap(sx, sy)) return;

        double size = 6.0;
        gc.setStroke(Color.YELLOW);
        gc.setLineWidth(2.0);
        // Ligne horizontale
        gc.strokeLine(sx - size, sy, sx + size, sy);
        // Ligne verticale
        gc.strokeLine(sx, sy - size, sx, sy + size);
    }

    // ---------- Dessins du fond, grille, obstacles, etc. (inchangés) ----------
    private void drawMapSurface(GraphicsContext gc, WorldConfiguration worldConfig) {
        Color ground = getGroundColor(worldConfig);
        gc.setFill(ground.deriveColor(0, 0.85, 0.72, 0.78));
        gc.fillRoundRect(MAP_INSET, MAP_INSET, SIZE - MAP_INSET * 2, SIZE - MAP_INSET * 2, 8, 8);

        gc.setStroke(ground.brighter().deriveColor(0, 0.75, 1.0, 0.34));
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
        for (WorldObject obj : worldConfig.getObjects()) {
            if (isGroundPlane(obj)) continue;

            double x = toScreenX(obj.getPosX(), center, scale);
            double y = toScreenY(obj.getPosZ(), center, scale);
            if (!isInsideMap(x, y)) continue;

            Color objColor = parseColor(obj.getColor());
            gc.setFill(objColor.deriveColor(0, 1, 1, 0.82));
            gc.setStroke(objColor.brighter().deriveColor(0, 1, 1, 0.64));

            if ("cylinder".equals(obj.getType())) {
                double radius = Math.max(2.2, obj.getSizeX() * scale);
                gc.fillOval(x - radius, y - radius, radius * 2, radius * 2);
                gc.strokeOval(x - radius, y - radius, radius * 2, radius * 2);
            } else {
                double w = Math.max(3.0, obj.getSizeX() * scale);
                double d = Math.max(3.0, obj.getSizeZ() * scale);
                gc.fillRect(x - w / 2, y - d / 2, w, d);
                gc.strokeRect(x - w / 2, y - d / 2, w, d);
            }
        }
    }

    private void drawDroneMarker(GraphicsContext gc, DroneModel model, double center, double scale) {
        double rawX = toScreenX(model.getX(), center, scale);
        double rawY = toScreenY(model.getZ(), center, scale);
        double x = clamp(rawX, MAP_INSET, SIZE - MAP_INSET);
        double y = clamp(rawY, MAP_INSET, SIZE - MAP_INSET);
        boolean outside = rawX != x || rawY != y;

        double yawRad = Math.toRadians(model.getYaw());
        double fwdX = Math.sin(yawRad);
        double fwdY = -Math.cos(yawRad);
        double rightX = Math.cos(yawRad);
        double rightY = Math.sin(yawRad);

        double tipX = x + fwdX * DRONE_MARKER_SIZE;
        double tipY = y + fwdY * DRONE_MARKER_SIZE;
        double tailX = x - fwdX * DRONE_MARKER_SIZE * 0.75;
        double tailY = y - fwdY * DRONE_MARKER_SIZE * 0.75;
        double leftX = tailX - rightX * DRONE_MARKER_SIZE * 0.65;
        double leftY = tailY - rightY * DRONE_MARKER_SIZE * 0.65;
        double rightTipX = tailX + rightX * DRONE_MARKER_SIZE * 0.65;
        double rightTipY = tailY + rightY * DRONE_MARKER_SIZE * 0.65;

        gc.setFill(outside ? Color.rgb(255, 209, 102) : Color.rgb(255, 84, 84));
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.3);
        gc.fillPolygon(new double[]{tipX, leftX, rightTipX}, new double[]{tipY, leftY, rightTipY}, 3);
        gc.strokePolygon(new double[]{tipX, leftX, rightTipX}, new double[]{tipY, leftY, rightTipY}, 3);
    }

    private double toScreenX(double worldX, double center, double scale) { return center + worldX * scale; }
    private double toScreenY(double worldZ, double center, double scale) { return center - worldZ * scale; }

    private double getWorldSize(WorldConfiguration worldConfig) {
        double size = DEFAULT_WORLD_SIZE;
        for (WorldObject obj : worldConfig.getObjects()) {
            if (isGroundPlane(obj)) size = Math.max(size, Math.max(obj.getSizeX(), obj.getSizeZ()));
            double hx = Math.abs(obj.getPosX()) + obj.getSizeX() / 2.0;
            double hz = Math.abs(obj.getPosZ()) + obj.getSizeZ() / 2.0;
            size = Math.max(size, 2.0 * Math.max(hx, hz));
        }
        return size;
    }

    private boolean isGroundPlane(WorldObject obj) { return "plane".equals(obj.getType()); }

    private Color getGroundColor(WorldConfiguration worldConfig) {
        for (WorldObject obj : worldConfig.getObjects())
            if (isGroundPlane(obj)) return parseColor(obj.getColor());
        return parseColor(worldConfig.getEnvironment().getGroundColor());
    }

    private Color parseColor(String value) {
        try { return Color.web(value); }
        catch (IllegalArgumentException e) { return Color.rgb(255, 176, 76); }
    }

    private boolean isInsideMap(double x, double y) {
        return x >= MAP_INSET && x <= SIZE - MAP_INSET && y >= MAP_INSET && y <= SIZE - MAP_INSET;
    }

    private double clamp(double v, double min, double max) { return Math.max(min, Math.min(max, v)); }
}