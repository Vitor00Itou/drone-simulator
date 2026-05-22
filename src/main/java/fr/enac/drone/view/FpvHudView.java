package fr.enac.drone.view;

import fr.enac.drone.model.drone.DroneModel;
import fr.enac.drone.model.drone.DroneTelemetry;
import fr.enac.drone.model.world.WorldConfiguration;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Head-up display (HUD) overlay for the drone simulation.
 * Combines telemetry panel, command help panel, and minimap.
 */
public class FpvHudView extends BorderPane {
    
    private final TelemetryPanel telemetryPanel;
    private final MiniMapView miniMapView;
    
    // Flight trail history (X, Z coordinates)
    private final List<double[]> trail = new ArrayList<>();

    public FpvHudView() {
        telemetryPanel = new TelemetryPanel();
        CommandHelpPanel commandHelpPanel = new CommandHelpPanel();
        miniMapView = new MiniMapView();

        // Make panels transparent to mouse clicks so clicks pass through to the minimap
        telemetryPanel.setMouseTransparent(true);
        commandHelpPanel.setMouseTransparent(true);

        setPickOnBounds(false);

        // Top: telemetry panel
        setTop(telemetryPanel);
        BorderPane.setAlignment(telemetryPanel, Pos.TOP_CENTER);
        BorderPane.setMargin(telemetryPanel, new Insets(24, 0, 0, 0));

        // Bottom: command help panel on the left, minimap on the right
        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);

        HBox bottomOverlay = new HBox(18, commandHelpPanel, bottomSpacer, miniMapView);
        bottomOverlay.setAlignment(Pos.BOTTOM_CENTER);
        bottomOverlay.setPadding(new Insets(0, 24, 24, 24));

        setBottom(bottomOverlay);
    }

    /**
     * Updates all HUD components with the latest drone state.
     *
     * @param model       the drone model containing current state
     * @param worldConfig the world configuration (obstacles, ground, etc.)
     */
    public void update(DroneModel model, WorldConfiguration worldConfig) {
        DroneTelemetry telemetry = model.getTelemetry();
        telemetryPanel.update(telemetry);
        miniMapView.render(model, worldConfig);

        // Record current position for flight trail
        double x = model.getX();
        double z = model.getZ();
        trail.add(new double[]{x, z});
        
        // Limit trail size to prevent memory issues
        if (trail.size() > 1000) {
            trail.remove(0);
        }
    }

    /**
     * Registers a callback to be invoked when the user clicks on the minimap.
     *
     * @param callback receives the world coordinates {x, z} of the clicked point
     */
    public void setOnMinimapTargetClicked(Consumer<double[]> callback) {
        miniMapView.setOnTargetClicked(callback);
    }

    /**
     * Clears the current navigation target cross from the minimap.
     */
    public void clearMinimapTarget() {
        miniMapView.clearTarget();
    }

    /**
     * Returns a copy of the recorded flight trail points.
     * Each element is a double[] {x, z} in world coordinates.
     *
     * @return list of trail points
     */
    public List<double[]> getTrail() {
        return trail;
    }
}
