package fr.enac.drone.view;

import fr.enac.drone.model.drone.DroneControlSettings;
import fr.enac.drone.model.Range;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.FontWeight;
import javafx.scene.shape.SVGPath;
import javafx.scene.text.Font;

import java.util.Locale;
import java.util.function.DoubleConsumer;

/**
 * Small expandable control settings menu for flight inputs.
 */
public class ControlSettingsPanel extends VBox {

    private final VBox settingsMenu;
    private final Label yawSensitivityValueLabel;
    private final Label maxHorizontalSpeedValueLabel;
    private final Label maxVerticalSpeedValueLabel;

    public ControlSettingsPanel(
            double initialYawSensitivity,
            DoubleConsumer onYawSensitivityChanged,
            double initialMaxHorizontalSpeed,
            DoubleConsumer onMaxHorizontalSpeedChanged,
            double initialMaxVerticalSpeed,
            DoubleConsumer onMaxVerticalSpeedChanged
    ) {
        setAlignment(Pos.BOTTOM_LEFT);
        setSpacing(6);
        setMaxWidth(USE_PREF_SIZE);
        setPickOnBounds(false);

        Button toggleButton = createToggleButton();

        yawSensitivityValueLabel = new Label();
        maxHorizontalSpeedValueLabel = new Label();
        maxVerticalSpeedValueLabel = new Label();

        Slider yawSensitivitySlider = createSlider(
                DroneControlSettings.YAW_SENSITIVITY,
                initialYawSensitivity,
                0.05,
                0.25
        );

        updateYawSensitivityLabel(yawSensitivitySlider.getValue());

        yawSensitivitySlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            double value = newValue.doubleValue();
            updateYawSensitivityLabel(value);
            onYawSensitivityChanged.accept(value);
        });

        Slider maxHorizontalSpeedSlider = createSlider(
                DroneControlSettings.MAX_HORIZONTAL_SPEED,
                initialMaxHorizontalSpeed,
                1.0,
                5.0
        );

        updateSpeedLabel(
                maxHorizontalSpeedValueLabel,
                maxHorizontalSpeedSlider.getValue()
        );

        maxHorizontalSpeedSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            double value = newValue.doubleValue();
            updateSpeedLabel(maxHorizontalSpeedValueLabel, value);
            onMaxHorizontalSpeedChanged.accept(value);
        });

        Slider maxVerticalSpeedSlider = createSlider(
                DroneControlSettings.MAX_VERTICAL_SPEED,
                initialMaxVerticalSpeed,
                1.0,
                5.0
        );

        updateSpeedLabel(
                maxVerticalSpeedValueLabel,
                maxVerticalSpeedSlider.getValue()
        );

        maxVerticalSpeedSlider.valueProperty().addListener((obs, oldValue, newValue) -> {
            double value = newValue.doubleValue();
            updateSpeedLabel(maxVerticalSpeedValueLabel, value);
            onMaxVerticalSpeedChanged.accept(value);
        });

        VBox yawControl = createControlGroup(
                "Yaw sensitivity",
                yawSensitivityValueLabel,
                yawSensitivitySlider
        );
        VBox maxHorizontalSpeedControl = createControlGroup(
                "Horizontal speed",
                maxHorizontalSpeedValueLabel,
                maxHorizontalSpeedSlider
        );
        VBox maxVerticalSpeedControl = createControlGroup(
                "Vertical speed",
                maxVerticalSpeedValueLabel,
                maxVerticalSpeedSlider
        );

        settingsMenu = new VBox(
                10,
                yawControl,
                maxHorizontalSpeedControl,
                maxVerticalSpeedControl
        );
        settingsMenu.setPadding(new Insets(10, 12, 12, 12));
        settingsMenu.setMaxWidth(USE_PREF_SIZE);
        settingsMenu.setBackground(new Background(new BackgroundFill(
                Color.rgb(67, 66, 80, 0.94),
                new CornerRadii(5),
                Insets.EMPTY
        )));
        settingsMenu.setEffect(new DropShadow(12, Color.rgb(0, 0, 0, 0.28)));
        settingsMenu.setVisible(false);
        settingsMenu.setManaged(false);

        toggleButton.setOnAction(e -> {
            boolean expanded = !settingsMenu.isVisible();
            settingsMenu.setVisible(expanded);
            settingsMenu.setManaged(expanded);
        });

        getChildren().addAll(settingsMenu, toggleButton);
    }

    private Slider createSlider(
            Range range,
            double initialValue,
            double blockIncrement,
            double majorTickUnit
    ) {
        Slider slider = new Slider(
                range.getMin(),
                range.getMax(),
                range.clamp(initialValue)
        );
        slider.setPrefWidth(170);
        slider.setBlockIncrement(blockIncrement);
        slider.setMajorTickUnit(majorTickUnit);
        slider.setMinorTickCount(0);
        slider.setShowTickMarks(true);
        return slider;
    }

    private VBox createControlGroup(
            String labelText,
            Label valueLabel,
            Slider slider
    ) {
        Label label = new Label(labelText);
        label.setTextFill(Color.rgb(232, 230, 241));
        label.setFont(Font.font("System", FontWeight.BOLD, 12));

        HBox header = new HBox(10, label, valueLabel);
        header.setAlignment(Pos.CENTER_LEFT);

        return new VBox(5, header, slider);
    }

    private Button createToggleButton() {
        Button button = new Button();
        button.setTooltip(new Tooltip("Control settings"));
        button.setFocusTraversable(false);
        button.setMinSize(30, 30);
        button.setPrefSize(30, 30);
        button.setMaxSize(30, 30);
        button.setGraphic(createSettingsIcon());
        button.setBackground(new Background(new BackgroundFill(
                Color.rgb(67, 66, 80, 0.9),
                new CornerRadii(5),
                Insets.EMPTY
        )));
        button.setEffect(new DropShadow(8, Color.rgb(0, 0, 0, 0.24)));
        return button;
    }

    private SVGPath createSettingsIcon() {
        SVGPath icon = new SVGPath();
        icon.setContent(
                "M19.43 12.98c.04-.32.07-.65.07-.98s-.02-.66-.07-.98"
                        + "l2.11-1.65c.19-.15.24-.42.12-.64l-2-3.46"
                        + "c-.12-.22-.37-.31-.6-.22l-2.49 1c-.52-.4-1.08-.73-1.69-.98"
                        + "l-.38-2.65C14.46 2.18 14.25 2 14 2h-4c-.25 0-.46.18-.5.42"
                        + "l-.38 2.65c-.61.25-1.17.59-1.69.98l-2.49-1"
                        + "c-.23-.08-.48 0-.6.22l-2 3.46c-.13.22-.07.49.12.64"
                        + "l2.11 1.65c-.04.32-.08.65-.08.98s.03.66.08.98"
                        + "l-2.11 1.65c-.19.15-.24.42-.12.64l2 3.46"
                        + "c.12.22.37.31.6.22l2.49-1c.52.4 1.08.73 1.69.98"
                        + "l.38 2.65c.04.24.25.42.5.42h4c.25 0 .46-.18.5-.42"
                        + "l.38-2.65c.61-.25 1.17-.59 1.69-.98l2.49 1"
                        + "c.23.08.48 0 .6-.22l2-3.46c.12-.22.07-.49-.12-.64"
                        + "l-2.11-1.65zM12 15.5A3.5 3.5 0 1 1 12 8a3.5 3.5 0 0 1 0 7.5z"
        );
        icon.setScaleX(0.68);
        icon.setScaleY(0.68);
        icon.setFill(Color.rgb(232, 230, 241));
        return icon;
    }

    private void updateYawSensitivityLabel(double value) {
        yawSensitivityValueLabel.setText(
                String.format(Locale.ROOT, "%.0f%%", value * 100.0)
        );
        yawSensitivityValueLabel.setTextFill(Color.rgb(232, 230, 241));
        yawSensitivityValueLabel.setFont(Font.font("System", 12));
    }

    private void updateSpeedLabel(Label label, double value) {
        label.setText(
                String.format(Locale.ROOT, "%.0f m/s", value)
        );
        label.setTextFill(Color.rgb(232, 230, 241));
        label.setFont(Font.font("System", 12));
    }
}
