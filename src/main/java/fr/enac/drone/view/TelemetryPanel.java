package fr.enac.drone.view;

import java.util.Locale;

import fr.enac.drone.model.drone.DroneTelemetry;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Top FPV telemetry bar including battery indicator and armed status.
 */
public class TelemetryPanel extends HBox {

    private final TelemetryItem  altitudeItem       = new TelemetryItem("ALT");
    private final TelemetryItem  horizontalSpeedItem = new TelemetryItem("HSPD");
    private final TelemetryItem  verticalSpeedItem   = new TelemetryItem("VSPD");
    private final TelemetryItem  headingItem         = new TelemetryItem("HDG");
    private final TelemetryItem  distanceItem        = new TelemetryItem("DIST");
    private final BatteryIndicator batteryIndicator  = new BatteryIndicator();

    /** Small coloured pill that shows ARMED / DISARMED */
    private final Label armedLabel = new Label("DISARMED");

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

        armedLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        armedLabel.setPadding(new Insets(4, 10, 4, 10));
        armedLabel.setBackground(new Background(new BackgroundFill(
                Color.rgb(200, 0, 0, 0.85),
                new CornerRadii(8),
                Insets.EMPTY
        )));
        armedLabel.setTextFill(Color.WHITE);

        getChildren().addAll(armedLabel, altitudeItem, horizontalSpeedItem,
                verticalSpeedItem, headingItem, distanceItem, batteryIndicator);
    }

    public void update(DroneTelemetry telemetry) {
        altitudeItem.setValue(String.format(Locale.US, "%.1f m",   telemetry.altitudeMeters()));
        horizontalSpeedItem.setValue(String.format(Locale.US, "%.1f m/s", telemetry.horizontalSpeedMs()));
        verticalSpeedItem.setValue(String.format(Locale.US, "%.1f m/s",   telemetry.verticalSpeedMs()));
        headingItem.setValue(String.format(Locale.US, "%.0f\u00B0",       telemetry.headingDegrees()));
        distanceItem.setValue(String.format(Locale.US, "%.1f m",          telemetry.distanceMeters()));
        batteryIndicator.update(telemetry.batteryPercent());

        // Armed status pill
        if (telemetry.armed()) {
            armedLabel.setText("ARMED");
            armedLabel.setBackground(new Background(new BackgroundFill(
                    Color.rgb(0, 180, 0, 0.85), new CornerRadii(8), Insets.EMPTY)));
        } else {
            armedLabel.setText("DISARMED");
            armedLabel.setBackground(new Background(new BackgroundFill(
                    Color.rgb(200, 0, 0, 0.85), new CornerRadii(8), Insets.EMPTY)));
        }
    }
}