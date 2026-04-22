package fr.enac.drone.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Displays the commands currently available in the simulator.
 */
public class CommandHelpPanel extends VBox {
    public CommandHelpPanel() {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(8);
        setPadding(new Insets(12, 14, 14, 14));
        setMaxWidth(USE_PREF_SIZE);
        setBackground(new Background(new BackgroundFill(
                Color.rgb(67, 66, 80, 0.9),
                new CornerRadii(5),
                Insets.EMPTY
        )));
        setEffect(new DropShadow(12, Color.rgb(0, 0, 0, 0.28)));

        getChildren().addAll(
                createCommandRow("W / S", "Altitude"),
                createCommandRow("A / D", "Cap"),
                createCommandRow("Up / Down", "Avancer / Reculer"),
                createCommandRow("Left / Right", "Deplacement lateral")
        );
    }

    private HBox createCommandRow(String keys, String description) {
        Label keyLabel = new Label(keys);
        keyLabel.setTextFill(Color.rgb(232, 230, 241));
        keyLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        keyLabel.setAlignment(Pos.CENTER);
        keyLabel.setMinWidth(72);
        keyLabel.setPadding(new Insets(5, 10, 5, 10));
        keyLabel.setBackground(new Background(new BackgroundFill(
                Color.rgb(29, 26, 48, 0.95),
                new CornerRadii(14),
                Insets.EMPTY
        )));

        Label descriptionLabel = new Label(description);
        descriptionLabel.setTextFill(Color.rgb(232, 230, 241));
        descriptionLabel.setFont(Font.font("System", 14));

        HBox row = new HBox(10, keyLabel, descriptionLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
