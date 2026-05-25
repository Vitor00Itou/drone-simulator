package fr.enac.drone.view;

import fr.enac.drone.model.SimulationState;
import fr.enac.drone.model.WorldPosition2D;
import fr.enac.drone.model.drone.DroneModel;
import fr.enac.drone.model.world.WorldConfiguration;
import fr.enac.drone.model.world.WorldObject;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Minimap HUD element displaying a top-down radar view centered on the drone.
 */
public class MiniMapView extends StackPane {

    private static final double MAP_SIZE = 240.0;
    private static final double MAP_FRAME_HEIGHT = MAP_SIZE + 60;
    private static final double TRAIL_MIN_DISTANCE = 1;
    private static final int MAX_TRAIL_POINTS = 2000;

    private final Canvas canvas;
    private final Slider zoomSlider;
    // Flight trail history (X, Z coordinates)
    private final List<WorldPosition2D> trailPoints = new ArrayList<>();

    private Consumer<WorldPosition2D> onTargetClicked;
    private WorldPosition2D targetPos = null;
    private double viewRadius = 1000.0; // Default Zoom limit: 1km around the drone
    private double lastDroneX = 0;
    private double lastDroneZ = 0;
    private double lastDroneYaw = 0;
    private double lastTrailX = 0;
    private double lastTrailZ = 0;
    private boolean trailInitialized = false;

    /**
     * Creates the minimap canvas, circular frame, zoom slider, and mouse handlers.
     */
    public MiniMapView() {
        setMinSize(MAP_SIZE, MAP_FRAME_HEIGHT);
        setPrefSize(MAP_SIZE, MAP_FRAME_HEIGHT);
        setMaxSize(MAP_SIZE, MAP_FRAME_HEIGHT);

        canvas = new Canvas(MAP_SIZE, MAP_SIZE);

        // Circular radar styling
        setStyle("-fx-border-color: rgba(255, 255, 255, 0.35); " +
                 "-fx-border-width: 3; " +
                 "-fx-border-radius: 200; " +
                 "-fx-background-radius: 200; " +
                 "-fx-background-color: rgba(15, 20, 25, 0.85);");

        Circle clip = new Circle(MAP_SIZE / 2, MAP_SIZE / 2, MAP_SIZE / 2);
        canvas.setClip(clip);
        canvas.setOnMouseClicked(this::handleMouseClicked);

        // Zoom Slider on the right side of the minimap
        zoomSlider = new Slider(100, 3000, viewRadius); // Min 100m, Max 3km, Default 1km
        zoomSlider.setOrientation(Orientation.VERTICAL);
        zoomSlider.setMaxHeight(140);
        zoomSlider.setFocusTraversable(false);
        zoomSlider.setStyle("-fx-control-inner-background: rgba(0, 0, 0, 0.97); -fx-accent: #6acaff;");

        StackPane.setAlignment(zoomSlider, Pos.CENTER_RIGHT);
        StackPane.setMargin(zoomSlider, new Insets(0, -25, 0, 0)); // Negative margin pushes it outside
        zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> viewRadius = newVal.doubleValue());

        // Prevent the slider from stealing keyboard focus so flight controls remain active
        zoomSlider.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) canvas.requestFocus();
        });

        // Mouse scroll for natural zooming
        setOnScroll(event -> {
            double zoomFactor = event.getDeltaY() > 0 ? -150 : 150;
            adjustZoom(zoomFactor);
        });

        getChildren().addAll(canvas, zoomSlider);
    }

    /**
     * Registers a callback fired when the user selects a target on the minimap.
     *
     * @param callback receives the clicked world X/Z coordinate
     */
    public void setOnTargetClicked(Consumer<WorldPosition2D> callback) {
        this.onTargetClicked = callback;
    }

    /**
     * Clears the currently displayed navigation target.
     */
    public void clearTarget() {
        this.targetPos = null;
    }

    /**
     * Adjusts the minimap zoom radius.
     *
     * @param delta zoom radius delta in meters
     */
    public void adjustZoom(double delta) {
        zoomSlider.setValue(zoomSlider.getValue() + delta);
    }

    /**
     * Returns the current minimap zoom radius.
     *
     * @return zoom radius in meters
     */
    public double getZoom() {
        return viewRadius;
    }

    /**
     * Sets the minimap zoom radius.
     *
     * @param zoom zoom radius in meters
     */
    public void setZoom(double zoom) {
        zoomSlider.setValue(zoom);
    }

    /**
     * Returns a copy of the recorded flight trail.
     *
     * @return trail points in world coordinates
     */
    public List<WorldPosition2D> getTrail() {
        return new ArrayList<>(trailPoints);
    }

    /**
     * Clears the recorded flight trail.
     */
    public void clearTrail() {
        trailPoints.clear();
        trailInitialized = false;
        lastTrailX = 0;
        lastTrailZ = 0;
    }

    /**
     * Converts a minimap click into a world-space target and notifies the listener.
     *
     * @param event mouse click event on the canvas
     */
    private void handleMouseClicked(MouseEvent event) {
        if (onTargetClicked == null) return;

        double dx = event.getX() - MAP_SIZE / 2.0;
        double dy = event.getY() - MAP_SIZE / 2.0;

        // Compensate the rotation
        double rad = Math.toRadians(lastDroneYaw);
        double cosA = Math.cos(rad);
        double sinA = Math.sin(rad);

        double rotX = dx * cosA - dy * sinA;
        double rotY = dx * sinA + dy * cosA;

        // Invert the scaling (including the -Y flip for +Z) and apply drone offset
        double scale = MAP_SIZE / (viewRadius * 2);
        double worldX = lastDroneX + (rotX / scale);
        double worldZ = lastDroneZ + (rotY / -scale);

        targetPos = new WorldPosition2D(worldX, worldZ);
        onTargetClicked.accept(targetPos);
    }

    /**
     * Renders the minimap for the current model and world state.
     *
     * @param model drone model used as the minimap center
     * @param config world configuration to draw
     * @param simulationState current simulation lifecycle state
     */
    public void render(
            DroneModel model,
            WorldConfiguration config,
            SimulationState simulationState
    ) {
        lastDroneX = model.getX();
        lastDroneZ = model.getZ();
        lastDroneYaw = model.getYaw();

        if (simulationState == SimulationState.RUNNING) {
            // Record current position for flight trail
            updateTrail(model);
        }

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0, 0, MAP_SIZE, MAP_SIZE);

        double groundWidth = 5000.0;
        double groundHeight = 5000.0;
        double groundX = 0.0;
        double groundZ = 0.0;
        Color groundColor = Color.rgb(34, 139, 34); 

        for (WorldObject obj : config.getObjects()) {
            if (obj.isPlane()) {
                groundWidth = obj.getSizeX();
                groundHeight = obj.getSizeZ();
                groundX = obj.getPosX();
                groundZ = obj.getPosZ();
                try {
                    groundColor = Color.web(obj.getColor());
                } catch (IllegalArgumentException ignored) {
                    groundColor = Color.rgb(34, 139, 34);
                }
                break;
            }
        }

        gc.save();
        // Move rendering origin to the center of the canvas
        gc.translate(MAP_SIZE / 2.0, MAP_SIZE / 2.0);

        // Rotate so that the drone's heading points UP (-Y on canvas). Minus fixes inversion.
        gc.rotate(-model.getYaw());

        // Scale to match viewRadius, flip Y axis so World +Z goes UP in the Canvas
        double scale = MAP_SIZE / (viewRadius * 2);
        gc.scale(scale, -scale);
        // Translate the world relative to the drone
        gc.translate(-model.getX(), -model.getZ());

        // 1. Draw Ground Bounds
        gc.setFill(groundColor);
        gc.fillRect(groundX - groundWidth / 2.0, groundZ - groundHeight / 2.0, groundWidth, groundHeight);

        // 2. Draw Grid (gives sense of speed when moving)
        if (config.getEnvironment().isShowGrid()) {
            gc.setStroke(Color.color(1, 1, 1, 0.15));
            gc.setLineWidth(1.0 / scale);
            for (double x = groundX - groundWidth/2; x <= groundX + groundWidth/2; x += 25) {
                gc.strokeLine(x, groundZ - groundHeight/2, x, groundZ + groundHeight/2);
            }
            for (double z = groundZ - groundHeight/2; z <= groundZ + groundHeight/2; z += 25) {
                gc.strokeLine(groundX - groundWidth/2, z, groundX + groundWidth/2, z);
            }
        }

        // 3. Draw Drone Trail
        drawTrail(gc, scale);

        // 4. Draw World Obstacles
        drawWorldObjects(gc, config);

        // 5. Draw Go To Target (Autopilot)
        drawTarget(gc, scale);

        gc.restore();
        drawDroneIcon(gc);
        drawCompass(gc, model);
    }

    /**
     * Adds the drone's current X/Z position to the trail when it has moved enough.
     *
     * @param model drone model supplying the current position
     */
    private void updateTrail(DroneModel model) {
        double cx = model.getX();
        double cz = model.getZ();

        if (!trailInitialized) {
            trailPoints.add(new WorldPosition2D(cx, cz));
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
            trailPoints.add(new WorldPosition2D(cx, cz));
            lastTrailX = cx;
            lastTrailZ = cz;
        }
    }

    /**
     * Draws the recorded flight trail.
     *
     * @param gc canvas graphics context
     * @param scale world-to-canvas scale
     */
    private void drawTrail(GraphicsContext gc, double scale) {
        if (trailPoints.size() <= 1) {
            return;
        }

        gc.setStroke(Color.rgb(255, 255, 255, 0.4));
        gc.setLineWidth(2.0 / scale);
        gc.beginPath();
        boolean first = true;
        for (WorldPosition2D point : trailPoints) {
            if (first) {
                gc.moveTo(point.x(), point.z());
                first = false;
            } else {
                gc.lineTo(point.x(), point.z());
            }
        }
        gc.stroke();
    }

    /**
     * Draws all non-ground world objects on the minimap.
     *
     * @param gc canvas graphics context
     * @param config world configuration to draw
     */
    private void drawWorldObjects(GraphicsContext gc, WorldConfiguration config) {
        for (WorldObject obj : config.getObjects()) {
            if (obj.isPlane()) continue;

            try {
                gc.setFill(Color.web(obj.getColor()));
            } catch (IllegalArgumentException e) {
                gc.setFill(Color.GRAY);
            }

            if (obj.isCylinder()) {
                double r = obj.getRadius();
                gc.fillOval(obj.getPosX() - r, obj.getPosZ() - r, r * 2, r * 2);
            } else {
                double w = obj.getSizeX();
                double d = obj.getSizeZ();
                gc.fillRect(obj.getPosX() - w/2, obj.getPosZ() - d/2, w, d);
            }
        }
    }

    /**
     * Draws the active navigation target cross.
     *
     * @param gc canvas graphics context
     * @param scale world-to-canvas scale
     */
    private void drawTarget(GraphicsContext gc, double scale) {
        if (targetPos == null) {
            return;
        }

        gc.setStroke(Color.RED);
        gc.setLineWidth(3.0 / scale);
        double tx = targetPos.x();
        double tz = targetPos.z();
        double crossSize = 5.0;
        gc.strokeLine(tx - crossSize, tz - crossSize, tx + crossSize, tz + crossSize);
        gc.strokeLine(tx - crossSize, tz + crossSize, tx + crossSize, tz - crossSize);
    }

    /**
     * Draws the fixed center drone icon.
     *
     * @param gc canvas graphics context
     */
    private void drawDroneIcon(GraphicsContext gc) {
        // 6. Draw the Drone Icon (Always fixed at the center, pointing UP)
        gc.setFill(Color.WHITE);
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(1.5);

        double[] xPoints = { MAP_SIZE/2, MAP_SIZE/2 - 7, MAP_SIZE/2 + 7 };
        double[] yPoints = { MAP_SIZE/2 - 10, MAP_SIZE/2 + 7, MAP_SIZE/2 + 7 };
        gc.fillPolygon(xPoints, yPoints, 3);
        gc.strokePolygon(xPoints, yPoints, 3);
    }

    /**
     * Draws cardinal direction labels around the minimap edge.
     *
     * @param gc canvas graphics context
     * @param model drone model supplying heading
     */
    private void drawCompass(GraphicsContext gc, DroneModel model) {
        // 7. Draw the Cardinal Points (N, S, E, W) rotating dynamically
        gc.save();
        gc.translate(MAP_SIZE / 2.0, MAP_SIZE / 2.0);
        gc.rotate(-model.getYaw());
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("System", FontWeight.BOLD, 12));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        double compassR = MAP_SIZE / 2.0 - 12;
        gc.fillText("N", 0, -compassR);
        gc.fillText("S", 0, compassR);
        gc.fillText("E", compassR, 0);
        gc.fillText("W", -compassR, 0);
        gc.restore();
    }
}
