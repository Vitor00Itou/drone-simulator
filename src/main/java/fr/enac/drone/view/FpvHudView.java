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

/**
 * Two-dimensional FPV overlay displayed above the 3D scene.
 */
public class FpvHudView extends BorderPane {
    private final TelemetryPanel telemetryPanel;
    private final MiniMapView miniMapView;

    public FpvHudView() {
        telemetryPanel = new TelemetryPanel();
        CommandHelpPanel commandHelpPanel = new CommandHelpPanel();
        miniMapView = new MiniMapView();

        setMouseTransparent(true);
        setPickOnBounds(false);

        setTop(telemetryPanel);
        BorderPane.setAlignment(telemetryPanel, Pos.TOP_CENTER);
        BorderPane.setMargin(telemetryPanel, new Insets(24, 0, 0, 0));

        Region bottomSpacer = new Region();
        HBox.setHgrow(bottomSpacer, Priority.ALWAYS);

        HBox bottomOverlay = new HBox(18, commandHelpPanel, bottomSpacer, miniMapView);
        bottomOverlay.setAlignment(Pos.BOTTOM_CENTER);
        bottomOverlay.setPadding(new Insets(0, 24, 24, 24));

        setBottom(bottomOverlay);
    }

    public void update(DroneModel model, WorldConfiguration worldConfig) {
        DroneTelemetry telemetry = model.getTelemetry();
        telemetryPanel.update(telemetry);
        miniMapView.render(model, worldConfig);
    }
}
