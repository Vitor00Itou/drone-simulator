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

public class BatteryIndicator extends VBox {
    private final Rectangle batteryBar;
    private final Rectangle batteryBg;
    private final Label percentLabel;

    public BatteryIndicator() {
        Label title = new Label("BAT");
        title.setFont(Font.font("Monospaced", FontWeight.BOLD, 13));
        title.setTextFill(Color.WHITE);
        title.setAlignment(Pos.CENTER);

        // Fond fixe (70x12)
        batteryBg = new Rectangle(70, 12);
        batteryBg.setArcWidth(6);
        batteryBg.setArcHeight(6);
        batteryBg.setFill(Color.web("#333333"));

        // Barre colorée – sa largeur variera, elle est ancrée à gauche (x=0)
        batteryBar = new Rectangle(70, 12);
        batteryBar.setArcWidth(6);
        batteryBar.setArcHeight(6);
        batteryBar.setFill(Color.LIME);

        // Panneau pour positionner la barre par-dessus le fond
        Pane barPane = new Pane();
        barPane.getChildren().addAll(batteryBg, batteryBar);
        // On place la barre en haut à gauche du Pane
        batteryBar.setX(0);
        batteryBar.setY(0);
        // Le fond occupe tout le Pane
        batteryBg.setX(0);
        batteryBg.setY(0);

        // Pourcentage centré (par-dessus le tout)
        percentLabel = new Label("100%");
        percentLabel.setFont(Font.font("Monospaced", FontWeight.BOLD, 9));
        percentLabel.setTextFill(Color.WHITE);
        percentLabel.setAlignment(Pos.CENTER);

        // StackPane pour superposer le panneau des barres et le texte centré
        StackPane stack = new StackPane(barPane, percentLabel);
        stack.setAlignment(Pos.CENTER);

        setAlignment(Pos.CENTER);
        setSpacing(4);
        setStyle("-fx-padding: 0 12 0 12;");
        getChildren().addAll(title, stack);
    }

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