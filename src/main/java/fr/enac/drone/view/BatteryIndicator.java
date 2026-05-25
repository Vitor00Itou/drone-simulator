package fr.enac.drone.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Compact HUD widget showing the current battery percentage as a colored bar.
 */
public class BatteryIndicator extends VBox {
    private final Rectangle batteryBar;
    private final Rectangle batteryBg;
    private final Label percentLabel;

    /**
     * Creates the battery indicator with its static label and progress bar.
     */
    public BatteryIndicator() {
        Label title = new Label("BAT");
        title.setFont(Font.font("Monospaced", FontWeight.BOLD, 13));
        title.setTextFill(Color.WHITE);
        title.setAlignment(Pos.CENTER);

        // Fixed background (70x12).
        batteryBg = new Rectangle(70, 12);
        batteryBg.setArcWidth(6);
        batteryBg.setArcHeight(6);
        batteryBg.setFill(Color.web("#333333"));

        // Colored bar anchored on the left; its width changes with the battery level.
        batteryBar = new Rectangle(70, 12);
        batteryBar.setArcWidth(6);
        batteryBar.setArcHeight(6);
        batteryBar.setFill(Color.LIME);

        // Pane used to position the bar over the background.
        Pane barPane = new Pane();
        barPane.getChildren().addAll(batteryBg, batteryBar);
        // Place the bar in the top-left corner of the pane.
        batteryBar.setX(0);
        batteryBar.setY(0);
        // The background fills the pane.
        batteryBg.setX(0);
        batteryBg.setY(0);

        // Centered percentage over the bar.
        percentLabel = new Label("100%");
        percentLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 9));
        percentLabel.setTextFill(Color.WHITE);
        percentLabel.setAlignment(Pos.CENTER);

        // StackPane overlays the bar pane and centered text.
        StackPane stack = new StackPane(barPane, percentLabel);
        stack.setAlignment(Pos.CENTER);

        setAlignment(Pos.CENTER);
        setSpacing(4);
        setStyle("-fx-padding: 0 12 0 12;");
        getChildren().addAll(title, stack);
    }

    /**
     * Updates the displayed battery level.
     *
     * @param percent battery level from 0 to 100 percent
     */
    public void update(double percent) {
        double clamped = Math.min(100.0, Math.max(0.0, percent));
        double width = 70 * (clamped / 100.0);
        batteryBar.setWidth(width);
        percentLabel.setText(String.format("%.0f%%", clamped));

        if (clamped > 50) {
            batteryBar.setFill(Color.LIME);
        } else if (clamped > 20) {
            batteryBar.setFill(Color.ORANGE);
        } else {
            batteryBar.setFill(Color.RED);
        }
    }
}
