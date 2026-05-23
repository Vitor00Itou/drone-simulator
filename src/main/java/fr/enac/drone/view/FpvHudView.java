package fr.enac.drone.view;

import fr.enac.drone.model.SimulationState;
import fr.enac.drone.model.drone.DroneModel;
import fr.enac.drone.model.drone.DroneTelemetry;
import fr.enac.drone.model.world.WorldConfiguration;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.function.Consumer;

/**
 * Head-up display (HUD) overlay for the drone simulation.
 * Combines telemetry panel, command help panel, state indicator, proximity overlay, and minimap.
 */
public class FpvHudView extends StackPane {

    private final TelemetryPanel telemetryPanel;
    private final MiniMapView miniMapView;
    private final ProximityWarningOverlay proximityOverlay;
    private final Label simulationStateLabel;

    public FpvHudView() {
        telemetryPanel = new TelemetryPanel();
        CommandHelpPanel commandHelpPanel = new CommandHelpPanel();
        miniMapView = new MiniMapView();
        proximityOverlay = new ProximityWarningOverlay();
        simulationStateLabel = createSimulationStateLabel();

        // Make panels transparent to mouse clicks so clicks pass through to the minimap
        telemetryPanel.setMouseTransparent(true);
        commandHelpPanel.setMouseTransparent(true);
        proximityOverlay.setMouseTransparent(true);
        simulationStateLabel.setMouseTransparent(true);

        setPickOnBounds(false);

        BorderPane borderPane = new BorderPane();
        borderPane.setPickOnBounds(false);

        // Top: telemetry panel
        StackPane topOverlay = new StackPane(telemetryPanel, simulationStateLabel);
        topOverlay.setPickOnBounds(false);
        topOverlay.setMouseTransparent(true);
        topOverlay.setPadding(new Insets(24, 24, 0, 24));
        StackPane.setAlignment(telemetryPanel, Pos.TOP_CENTER);
        StackPane.setAlignment(simulationStateLabel, Pos.TOP_LEFT);
        borderPane.setTop(topOverlay);

        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);

        // Bottom: command help panel on the left, minimap on the right
        HBox bottomOverlay = new HBox(18, commandHelpPanel, bottomSpacer, miniMapView);
        bottomOverlay.setAlignment(Pos.BOTTOM_CENTER);
        bottomOverlay.setPadding(new Insets(0, 24, 24, 24));

        borderPane.setBottom(bottomOverlay);

        getChildren().addAll(proximityOverlay, borderPane);

        updateSimulationState(SimulationState.READY);
    }

    private Label createSimulationStateLabel() {
        Label label = new Label();
        label.setFont(Font.font("System", FontWeight.BOLD, 12));
        label.setPadding(new Insets(7, 10, 7, 10));
        label.setMinHeight(28);
        label.setMinWidth(Region.USE_PREF_SIZE);
        return label;
    }

    /**
     * Updates all HUD components with the latest drone state.
     *
     * @param model       the drone model containing current state
     * @param worldConfig the world configuration (obstacles, ground, etc.)
     */
    public void update(
            DroneModel model,
            WorldConfiguration worldConfig,
            SimulationState simulationState
    ) {
        DroneTelemetry telemetry = model.getTelemetry();
        telemetryPanel.update(telemetry);
        updateSimulationState(simulationState);
        miniMapView.render(model, worldConfig, simulationState);

        if (simulationState == SimulationState.RUNNING) {
            proximityOverlay.update(model, worldConfig);
        } else {
            proximityOverlay.clear();
        }
    }

    public void updateSimulationState(SimulationState simulationState) {
        simulationStateLabel.setText("Simulation: " + simulationState.name());
        simulationStateLabel.setStyle(getSimulationStateStyle(simulationState));
    }

    private String getSimulationStateStyle(SimulationState simulationState) {
        String accentColor = switch (simulationState) {
            case READY -> "#b8c7d9";
            case RUNNING -> "#86d99a";
            case PAUSED -> "#ffd166";
        };

        return "-fx-background-color: rgba(29, 26, 48, 0.88);"
                + "-fx-background-radius: 5;"
                + "-fx-border-radius: 5;"
                + "-fx-border-color: " + accentColor + ";"
                + "-fx-border-width: 1;"
                + "-fx-text-fill: " + accentColor + ";";
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
     * Adjusts the minimap zoom level dynamically.
     *
     * @param delta the amount of zoom to add/remove
     */
    public void adjustMinimapZoom(double delta) {
        miniMapView.adjustZoom(delta);
    }

    public double getMinimapZoom() {
        return miniMapView.getZoom();
    }

    public void setMinimapZoom(double zoom) {
        miniMapView.setZoom(zoom);
    }

    /**
     * Returns a copy of the recorded flight trail points.
     * Each element is a double[] {x, z} in world coordinates.
     *
     * @return list of trail points
     */
    public List<double[]> getTrail() {
        return miniMapView.getTrail();
    }

    /**
     * Clears the flight trail history.
     */
    public void clearTrail() {
        miniMapView.clearTrail();
        proximityOverlay.clear();
    }
}
