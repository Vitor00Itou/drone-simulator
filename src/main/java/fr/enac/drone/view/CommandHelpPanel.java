package fr.enac.drone.view;

import fr.enac.drone.input.InputDevice;
import fr.enac.drone.input.KeyboardLayout;
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

    /**
     * Creates the command help panel with default keyboard bindings.
     */
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

        updateHelpText(InputDevice.KEYBOARD, KeyboardLayout.QWERTY);
    }
    
    /**
     * Rebuilds the displayed command rows for the current input source.
     *
     * @param device active input device
     * @param layout active keyboard layout for keyboard controls
     */
    public void updateHelpText(InputDevice device, KeyboardLayout layout) {
        getChildren().clear();
        
        if (device == InputDevice.JOYSTICK) {
            getChildren().addAll(
                    createCommandRow("Start",         "Start / Pause / Resume"),
                    createCommandRow("Back",          "Reset simulation"),
                    createCommandRow("A",             "Takeoff / Arm"),
                    createCommandRow("X",             "Land"),
                    createCommandRow("B",             "Emergency Stop"),
                    createCommandRow("Y",             "Return to Home"),
                    createCommandRow("Left Stick \u2191\u2193", "Altitude"),
                    createCommandRow("Left Stick \u2190\u2192", "Heading"),
                    createCommandRow("Right Stick \u2191\u2193", "Forward / Backward"),
                    createCommandRow("Right Stick \u2190\u2192", "Lateral Movement"),
                    createCommandRow("LB / RB",       "Zoom Minimap")
            );
        } else {
            getChildren().addAll(
                    createCommandRow("Space / Enter", "Start / Pause / Resume"),
                    createCommandRow("R",             "Reset simulation"),
                    createCommandRow("O",             "Takeoff / Arm"),
                    createCommandRow("L",             "Land"),
                    createCommandRow("F",             "Emergency Stop"),
                    createCommandRow(layout.getVerticalUpLabel() + " / S", "Altitude"),
                    createCommandRow(layout.getYawLeftLabel() + " / D", "Heading"),
                    createCommandRow("Up / Down",     "Forward / Backward"),
                    createCommandRow("Left / Right",  "Lateral Movement"),
                    createCommandRow("H",             "Return to Home")
            );
        }
    }

    /**
     * Creates a single command binding row.
     *
     * @param keys key or button label
     * @param description command description
     * @return configured row node
     */
    private HBox createCommandRow(String keys, String description) {
        Label keyLabel = new Label(keys);
        keyLabel.setTextFill(Color.rgb(232, 230, 241));
        keyLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        keyLabel.setAlignment(Pos.CENTER);
        keyLabel.setMinWidth(90);
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
