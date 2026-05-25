package fr.enac.drone.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Displays one telemetry label/value pair in the FPV telemetry bar.
 */
public class TelemetryItem extends VBox {
    private final Label valueLabel;

    /**
     * Creates an item with a fixed label and an initially empty value.
     *
     * @param label short telemetry label
     */
    public TelemetryItem(String label) {
        Label titleLabel = new Label(label);
        titleLabel.setTextFill(Color.rgb(232, 230, 241));
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 16));

        valueLabel = new Label();
        valueLabel.setTextFill(Color.rgb(232, 230, 241));
        valueLabel.setFont(Font.font("System", 18));

        setAlignment(Pos.CENTER);
        setSpacing(10);
        setMinWidth(110);
        getChildren().addAll(titleLabel, valueLabel);
    }

    /**
     * Replaces the displayed telemetry value.
     *
     * @param value formatted value text
     */
    public void setValue(String value) {
        valueLabel.setText(value);
    }
}
