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
    private final TelemetryItem speedItem = new TelemetryItem("SPD");
    private final TelemetryItem headingItem = new TelemetryItem("CAP");
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
        getChildren().addAll(altitudeItem, speedItem, headingItem, distanceItem);
    }

    public void update(DroneTelemetry telemetry) {
        altitudeItem.setValue(String.format(Locale.US, "%.1f m", telemetry.altitudeMeters()));
        speedItem.setValue(String.format(Locale.US, "%.1f km/h", telemetry.speedKmh()));
        headingItem.setValue(String.format(Locale.US, "%.0f\u00B0", telemetry.headingDegrees()));
        distanceItem.setValue(String.format(Locale.US, "%.1f m", telemetry.distanceMeters()));
    }
}
