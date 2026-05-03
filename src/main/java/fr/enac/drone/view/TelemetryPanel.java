package fr.enac.drone.view;

import fr.enac.drone.model.DroneTelemetry;
import java.util.Locale;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

/**
 * Top FPV telemetry bar.
 */
public class TelemetryPanel extends HBox {
    private final TelemetryItem altitudeItem = new TelemetryItem("ALT");
    private final TelemetryItem horizontalSpeedItem = new TelemetryItem("HSPD");
    private final TelemetryItem verticalSpeedItem = new TelemetryItem("VSPD");
    private final TelemetryItem headingItem = new TelemetryItem("HDG");
    private final TelemetryItem distanceItem = new TelemetryItem("DIST");

    public TelemetryPanel() {
        setAlignment(Pos.CENTER);
        setSpacing(34);
        setPadding(new Insets(22, 42, 24, 42));
        setMaxWidth(USE_PREF_SIZE);
        setBackground(new Background(new BackgroundFill(
                Color.rgb(67, 66, 80, 0.9),
                new CornerRadii(14),
                Insets.EMPTY
        )));
        getChildren().addAll(altitudeItem, horizontalSpeedItem, verticalSpeedItem, headingItem, distanceItem);
    }

    public void update(DroneTelemetry telemetry) {
        altitudeItem.setValue(String.format(Locale.US, "%.1f m", telemetry.altitudeMeters()));
        horizontalSpeedItem.setValue(String.format(Locale.US, "%.1f m/s", telemetry.horizontalSpeedMs()));
        verticalSpeedItem.setValue(String.format(Locale.US, "%.1f m/s", telemetry.verticalSpeedMs()));
        headingItem.setValue(String.format(Locale.US, "%.0f\u00B0", telemetry.headingDegrees()));
        distanceItem.setValue(String.format(Locale.US, "%.1f m", telemetry.distanceMeters()));
    }
}
