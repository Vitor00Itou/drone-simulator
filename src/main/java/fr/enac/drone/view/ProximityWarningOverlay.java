package fr.enac.drone.view;

import fr.enac.drone.model.drone.DroneModel;
import fr.enac.drone.model.world.WorldConfiguration;
import fr.enac.drone.model.world.WorldObject;
import javafx.scene.effect.DropShadow;
import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;

import java.util.Arrays;

/**
 * A professional 8-sector proximity radar overlay.
 * Remains invisible until an object is detected within 25 meters.
 */
public class ProximityWarningOverlay extends StackPane {
    private final Arc[] arcs = new Arc[8];
    private final DropShadow[] glows = new DropShadow[8];
    private final double[] phases = new double[8];
    private long lastTime = System.currentTimeMillis();

    /**
     * Creates the radar sectors and center crosshair used by the proximity HUD.
     */
    public ProximityWarningOverlay() {
        setMouseTransparent(true);
        
        // Simple, clean central crosshair to anchor the HUD
        Rectangle vLine = new Rectangle(2, 12, Color.rgb(255, 255, 255, 0.25));
        Rectangle hLine = new Rectangle(12, 2, Color.rgb(255, 255, 255, 0.25));
        
        Group radarGroup = new Group();

        // Sectors: 0=Front, 1=Front-Right, 2=Right, 3=Back-Right, 4=Back, 5=Back-Left, 6=Left, 7=Front-Left
        // Note: In JavaFX Arc, 0 degrees is at 3 o'clock (Right), and positive angle goes counter-clockwise.
        double[] centerAngles = {90, 45, 0, 315, 270, 225, 180, 135};
        
        for (int i = 0; i < 8; i++) {
            arcs[i] = new Arc();
            arcs[i].setCenterX(0);
            arcs[i].setCenterY(0);
            arcs[i].setRadiusX(420); // Ellipse wider horizontally
            arcs[i].setRadiusY(280); // Ellipse shorter vertically
            arcs[i].setStartAngle(centerAngles[i] - 18);
            arcs[i].setLength(36); // 36 degree length leaves a clean 9 degree gap between sectors
            arcs[i].setType(ArcType.OPEN);
            arcs[i].setFill(Color.TRANSPARENT);
            arcs[i].setStrokeWidth(6);
            arcs[i].setStrokeLineCap(StrokeLineCap.ROUND);
            arcs[i].setOpacity(0.0);
            
            glows[i] = new DropShadow();
            glows[i].setRadius(12);
            glows[i].setSpread(0.4);
            arcs[i].setEffect(glows[i]);
            
            radarGroup.getChildren().add(arcs[i]);
        }
        
        getChildren().addAll(radarGroup, vLine, hLine);
    }

    /**
     * Hides all proximity warning sectors.
     */
    public void clear() {
        for (Arc arc : arcs) {
            arc.setOpacity(0);
        }
    }
    
    /**
     * Updates sector colors and opacity based on nearby obstacles.
     *
     * @param drone drone model used as the radar origin
     * @param worldConfig world configuration containing obstacles
     */
    public void update(DroneModel drone, WorldConfiguration worldConfig) {
        long now = System.currentTimeMillis();
        double dt = (now - lastTime) / 1000.0;
        lastTime = now;

        double[] distances = new double[8];
        Arrays.fill(distances, Double.MAX_VALUE);

        double droneX = drone.getX();
        double droneY = drone.getY();
        double droneZ = drone.getZ();
        
        for (WorldObject obj : worldConfig.getObjects()) {
            if (obj.isPlane()) continue; // Ignore ground for horizontal proximity

            // Check vertical overlap with a small 0.1m tolerance margin
            // This prevents false alarms when the drone is simply resting on top of a tower/base
            double objTop = obj.getTopY();
            double objBottom = obj.getBottomY();
            if (droneY + drone.getDroneHeight() / 2.0 <= objTop + 0.1 || droneY - drone.getDroneHeight() / 2.0 >= objBottom - 0.1) {
                continue; // We are safely flying over or completely under the object
            }

            double closestX = obj.getPosX();
            double closestZ = obj.getPosZ();

            if (obj.isBox()) {
                double halfW = obj.getHalfWidth();
                double halfD = obj.getHalfDepth();
                closestX = Math.max(obj.getPosX() - halfW, Math.min(droneX, obj.getPosX() + halfW));
                closestZ = Math.max(obj.getPosZ() - halfD, Math.min(droneZ, obj.getPosZ() + halfD));
            } else if (obj.isCylinder()) {
                double dx = droneX - obj.getPosX();
                double dz = droneZ - obj.getPosZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 0) {
                    closestX = obj.getPosX() + (dx / dist) * Math.min(dist, obj.getRadius());
                    closestZ = obj.getPosZ() + (dz / dist) * Math.min(dist, obj.getRadius());
                }
            }

            double dx = closestX - droneX;
            double dz = closestZ - droneZ;
            double dist = Math.max(0, Math.sqrt(dx * dx + dz * dz) - drone.getDroneRadius());

            // Map position to one of the 8 visual sectors relative to drone's yaw
            double angleDeg = Math.toDegrees(Math.atan2(dx, dz));
            double relativeAngle = angleDeg - drone.getYaw();
            while (relativeAngle <= -180) relativeAngle += 360;
            while (relativeAngle > 180) relativeAngle -= 360;

            int sector = (int) Math.round(relativeAngle / 45.0);
            if (sector < 0) sector += 8;
            if (sector == 8) sector = 0;

            distances[sector] = Math.min(distances[sector], dist);
        }

        // Update visual indicators based on distance
        for (int i = 0; i < 8; i++) {
            double dist = distances[i];
            if (dist > 25.0) {
                arcs[i].setOpacity(0.0);
            } else {
                double fraction = 1.0 - (dist / 25.0); // 0.0 (Far) to 1.0 (Collision)
                double hue = Math.max(0, 120 * (1.0 - Math.pow(fraction, 1.5))); // 120=Green, 60=Yellow, 0=Red
                Color color = Color.hsb(hue, 0.9, 1.0);
                
                arcs[i].setStroke(color);
                glows[i].setColor(color);
                
                // Dynamic frequency: slow (1-2 Hz) in green/yellow, spiking up to 13 Hz in orange/red
                double currentFrequency = 1.0 + 12.0 * Math.pow(fraction, 4.0);
                phases[i] += currentFrequency * dt * Math.PI * 2;
                
                // Pulse opacity oscillates between 0.3 and 1.0
                double pulse = 0.65 + 0.35 * Math.sin(phases[i]);
                
                double baseOpacity = Math.min(1.0, fraction * 1.5);
                arcs[i].setOpacity(baseOpacity * pulse);
            }
        }
    }
}
