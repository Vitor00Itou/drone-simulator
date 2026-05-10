package fr.enac.drone.view;

import fr.enac.drone.model.drone.DroneTelemetry;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;

/**
 * Two-dimensional FPV overlay displayed above the 3D scene.
 */
public class FpvHudView extends BorderPane {
    private final TelemetryPanel telemetryPanel;

    public FpvHudView() {
        telemetryPanel = new TelemetryPanel();
        CommandHelpPanel commandHelpPanel = new CommandHelpPanel();

        setMouseTransparent(true);
        setPickOnBounds(false);

        setTop(telemetryPanel);
        BorderPane.setAlignment(telemetryPanel, Pos.TOP_CENTER);
        BorderPane.setMargin(telemetryPanel, new Insets(24, 0, 0, 0));

        setBottom(commandHelpPanel);
        BorderPane.setAlignment(commandHelpPanel, Pos.BOTTOM_LEFT);
        BorderPane.setMargin(commandHelpPanel, new Insets(0, 0, 24, 24));
    }

    public void update(DroneTelemetry telemetry) {
        telemetryPanel.update(telemetry);
    }
}
