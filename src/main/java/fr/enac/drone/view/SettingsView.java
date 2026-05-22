package fr.enac.drone.view;

import fr.enac.drone.model.WorldMode;
import fr.enac.drone.model.drone.DroneControlSettings;
import fr.enac.drone.model.drone.DroneModel;
import fr.enac.drone.model.world.WorldConfiguration;
import fr.enac.drone.model.world.WorldPersistence;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.util.Duration;

import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * Overlay settings drawer for session-level drone and world configuration.
 */
public class SettingsView extends StackPane {

    private static final double DRAWER_WIDTH = 360.0;
    private static final double DRAWER_RIGHT_MARGIN = 24.0;
    private static final double DRAWER_TOP_MARGIN = 24.0;
    private static final double CLOSED_TRANSLATE_X = DRAWER_WIDTH + DRAWER_RIGHT_MARGIN;
    private static final String PANEL_BACKGROUND = "rgba(67, 66, 80, 0.94)";
    private static final String CONTROL_BACKGROUND = "rgba(29, 26, 48, 0.95)";
    private static final String CONTROL_BACKGROUND_SOFT = "rgba(29, 26, 48, 0.55)";
    private static final String HUD_TEXT = "#e8e6f1";
    private static final String HUD_TEXT_MUTED = "#c8c4d8";
    private static final String HUD_BORDER = "rgba(232, 230, 241, 0.24)";
    private static final String HUD_BORDER_STRONG = "rgba(232, 230, 241, 0.48)";
    private static final String HUD_ACCENT = "#6acaff";
    private static final String WORLD_ACTION_BACKGROUND = "rgba(98, 105, 122, 0.96)";
    private static final String WORLD_ACTION_BORDER = "rgba(232, 230, 241, 0.42)";
    private static final String HUD_WARNING = "#ffd166";
    private static final double YAW_MIN = DroneControlSettings.YAW_SENSITIVITY.getMin();
    private static final double YAW_MAX = DroneControlSettings.YAW_SENSITIVITY.getMax();
    private static final double HORIZONTAL_SPEED_MIN = DroneControlSettings.MAX_HORIZONTAL_SPEED.getMin();
    private static final double HORIZONTAL_SPEED_MAX = DroneControlSettings.MAX_HORIZONTAL_SPEED.getMax();
    private static final double VERTICAL_SPEED_MIN = DroneControlSettings.MAX_VERTICAL_SPEED.getMin();
    private static final double VERTICAL_SPEED_MAX = DroneControlSettings.MAX_VERTICAL_SPEED.getMax();

    public enum Section {
        DRONE,
        WORLD
    }

    private enum WorldSource {
        PREDEFINED,
        RANDOM
    }

    public static class State {
        private Section activeSection = Section.DRONE;
        private WorldSource worldSource = WorldSource.PREDEFINED;
        private String selectedWorldFilename;
        private WorldMode selectedWorldMode = WorldMode.MEDIUM_OBSTACLES;

        public Section getActiveSection() {
            return activeSection;
        }

        private void setActiveSection(Section activeSection) {
            this.activeSection = Objects.requireNonNull(activeSection);
        }

        private WorldSource getWorldSource() {
            return worldSource;
        }

        private void setWorldSource(WorldSource worldSource) {
            this.worldSource = Objects.requireNonNull(worldSource);
        }

        public void setSelectedWorldFilename(String selectedWorldFilename) {
            this.selectedWorldFilename = selectedWorldFilename;
        }

        private String getSelectedWorldFilename() {
            return selectedWorldFilename;
        }

        private WorldMode getSelectedWorldMode() {
            return selectedWorldMode;
        }

        private void setSelectedWorldMode(WorldMode selectedWorldMode) {
            this.selectedWorldMode = Objects.requireNonNull(selectedWorldMode);
        }
    }

    private final DroneModel model;
    private final WorldConfiguration currentConfig;
    private final String currentConfigFilename;
    private final BiConsumer<WorldConfiguration, String> onWorldApplied;
    private final State state;
    private final Region clickOutsideLayer;
    private final VBox drawer;
    private final Button settingsButton;

    private Runnable onOpened;
    private Runnable onClosed;
    private boolean open;
    private TranslateTransition transition;

    public SettingsView(
            DroneModel model,
            WorldConfiguration currentConfig,
            String currentConfigFilename,
            BiConsumer<WorldConfiguration, String> onWorldApplied,
            State state
    ) {
        this.model = Objects.requireNonNull(model, "Model cannot be null");
        this.currentConfig = Objects.requireNonNull(currentConfig, "World configuration cannot be null");
        this.currentConfigFilename = Objects.requireNonNull(currentConfigFilename, "Filename cannot be null");
        this.onWorldApplied = Objects.requireNonNull(onWorldApplied, "Callback cannot be null");
        this.state = Objects.requireNonNull(state, "State cannot be null");

        setPickOnBounds(false);
        setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        clickOutsideLayer = createClickOutsideLayer();
        settingsButton = createSettingsButton();
        drawer = createDrawer();

        getChildren().addAll(clickOutsideLayer, settingsButton, drawer);
        StackPane.setAlignment(settingsButton, Pos.TOP_RIGHT);
        StackPane.setMargin(settingsButton, new Insets(DRAWER_TOP_MARGIN, DRAWER_RIGHT_MARGIN, 0, 0));
        StackPane.setAlignment(drawer, Pos.TOP_RIGHT);
        StackPane.setMargin(drawer, new Insets(DRAWER_TOP_MARGIN, DRAWER_RIGHT_MARGIN, 0, 0));

        closeImmediately();
    }

    public void setOnOpened(Runnable onOpened) {
        this.onOpened = onOpened;
    }

    public void setOnClosed(Runnable onClosed) {
        this.onClosed = onClosed;
    }

    public boolean isOpen() {
        return open || drawer.isVisible();
    }

    public void open() {
        if (open) {
            return;
        }

        stopTransition();
        open = true;
        clickOutsideLayer.setVisible(true);
        clickOutsideLayer.setMouseTransparent(false);
        settingsButton.setVisible(false);
        settingsButton.setMouseTransparent(true);
        drawer.setVisible(true);
        drawer.setMouseTransparent(false);
        drawer.toFront();

        runTransition(0.0, null);

        if (onOpened != null) {
            onOpened.run();
        }
    }

    public void close() {
        if (!open) {
            return;
        }

        stopTransition();
        open = false;
        clickOutsideLayer.setMouseTransparent(true);
        drawer.setMouseTransparent(true);

        runTransition(CLOSED_TRANSLATE_X, () -> {
            drawer.setVisible(false);
            clickOutsideLayer.setVisible(false);
            settingsButton.setVisible(true);
            settingsButton.setMouseTransparent(false);
            settingsButton.toFront();

            if (onClosed != null) {
                onClosed.run();
            }
        });
    }

    private Region createClickOutsideLayer() {
        Region layer = new Region();
        layer.setStyle("-fx-background-color: rgba(0, 0, 0, 0.10);");
        layer.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        layer.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            close();
            event.consume();
        });
        return layer;
    }

    private Button createSettingsButton() {
        Button button = new Button();
        button.setFocusTraversable(false);
        button.setMinSize(42, 42);
        button.setPrefSize(42, 42);
        button.setMaxSize(42, 42);
        button.setGraphic(createSettingsIcon());
        button.setStyle(
                "-fx-background-color: " + PANEL_BACKGROUND + ";"
                        + "-fx-background-radius: 6;"
                        + "-fx-border-color: " + HUD_BORDER + ";"
                        + "-fx-border-radius: 6;"
                        + "-fx-cursor: hand;"
        );
        button.setEffect(new DropShadow(10, Color.rgb(0, 0, 0, 0.32)));
        button.setOnAction(event -> open());
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
        icon.setScaleX(0.78);
        icon.setScaleY(0.78);
        icon.setFill(Color.rgb(232, 230, 241));
        return icon;
    }

    private VBox createDrawer() {
        VBox panel = new VBox(16);
        panel.setPrefWidth(DRAWER_WIDTH);
        panel.setMinWidth(DRAWER_WIDTH);
        panel.setMaxWidth(DRAWER_WIDTH);
        panel.setMaxHeight(Region.USE_PREF_SIZE);
        panel.setPadding(new Insets(18, 18, 20, 18));
        panel.setStyle(
                "-fx-background-color: " + PANEL_BACKGROUND + ";"
                        + "-fx-background-radius: 5;"
                        + "-fx-border-color: " + HUD_BORDER + ";"
                        + "-fx-border-radius: 5;"
                        + "-fx-border-width: 0 0 0 1;"
        );
        panel.setEffect(new DropShadow(18, Color.rgb(0, 0, 0, 0.26)));
        panel.addEventHandler(MouseEvent.MOUSE_CLICKED, MouseEvent::consume);

        HBox header = createHeader();
        StackPane contentHost = new StackPane();
        contentHost.setMaxWidth(Double.MAX_VALUE);
        contentHost.setMaxHeight(Region.USE_PREF_SIZE);
        VBox tabs = createSettingsTabs(contentHost);

        panel.getChildren().addAll(header, tabs);
        return panel;
    }

    private VBox createSettingsTabs(StackPane contentHost) {
        VBox droneContent = createDroneContent();
        VBox worldContent = createWorldContent();

        ToggleGroup tabGroup = new ToggleGroup();
        ToggleButton droneButton = createSettingsTabButton("Drone", tabGroup);
        ToggleButton worldButton = createSettingsTabButton("World", tabGroup);

        droneButton.setOnAction(event -> {
            if (droneButton.isSelected()) {
                state.setActiveSection(Section.DRONE);
                showSettingsContent(contentHost, droneContent);
            }
        });
        worldButton.setOnAction(event -> {
            if (worldButton.isSelected()) {
                state.setActiveSection(Section.WORLD);
                showSettingsContent(contentHost, worldContent);
            }
        });
        tabGroup.selectedToggleProperty().addListener((observable, oldToggle, newToggle) -> {
            if (newToggle == null && oldToggle != null) {
                oldToggle.setSelected(true);
            }
        });

        if (state.getActiveSection() == Section.WORLD) {
            worldButton.setSelected(true);
            showSettingsContent(contentHost, worldContent);
        } else {
            droneButton.setSelected(true);
            showSettingsContent(contentHost, droneContent);
        }

        HBox tabButtons = new HBox(6, droneButton, worldButton);
        tabButtons.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(droneButton, Priority.ALWAYS);
        HBox.setHgrow(worldButton, Priority.ALWAYS);

        VBox tabs = new VBox(12, tabButtons, contentHost);
        tabs.setMaxWidth(Double.MAX_VALUE);
        return tabs;
    }

    private ToggleButton createSettingsTabButton(String text, ToggleGroup tabGroup) {
        ToggleButton button = new ToggleButton(text);
        button.setToggleGroup(tabGroup);
        button.setFocusTraversable(false);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle(getSettingsTabStyle(false));
        button.selectedProperty().addListener((observable, oldValue, selected) ->
                button.setStyle(getSettingsTabStyle(selected))
        );
        return button;
    }

    private String getSettingsTabStyle(boolean selected) {
        if (selected) {
            return "-fx-padding: 8 12;"
                    + "-fx-background-radius: 5;"
                    + "-fx-border-radius: 5;"
                    + "-fx-background-color: " + CONTROL_BACKGROUND + ";"
                    + "-fx-border-color: " + HUD_BORDER_STRONG + ";"
                    + "-fx-text-fill: " + HUD_TEXT + ";"
                    + "-fx-font-weight: bold;"
                    + "-fx-cursor: hand;";
        }

        return "-fx-padding: 8 12;"
                + "-fx-background-radius: 5;"
                + "-fx-border-radius: 5;"
                + "-fx-background-color: transparent;"
                + "-fx-border-color: " + HUD_BORDER + ";"
                + "-fx-text-fill: " + HUD_TEXT_MUTED + ";"
                + "-fx-cursor: hand;";
    }

    private void showSettingsContent(StackPane contentHost, Node content) {
        contentHost.getChildren().setAll(content);
        if (drawer != null) {
            drawer.requestLayout();
        }
    }

    private HBox createHeader() {
        Label title = new Label("Settings");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + HUD_TEXT + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button closeButton = new Button("X");
        closeButton.setFocusTraversable(false);
        closeButton.setMinSize(32, 32);
        closeButton.setPrefSize(32, 32);
        closeButton.setStyle(
                "-fx-font-size: 13px;"
                        + "-fx-font-weight: bold;"
                        + "-fx-text-fill: " + HUD_TEXT + ";"
                        + "-fx-background-color: transparent;"
                        + "-fx-background-radius: 4;"
                        + "-fx-cursor: hand;"
        );
        closeButton.setOnAction(event -> close());

        HBox header = new HBox(10, title, spacer, closeButton);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private HBox createSectionHeader(String titleText, Node trailingAction) {
        Label heading = new Label(titleText);
        heading.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + HUD_TEXT + ";");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(10, heading, spacer, trailingAction);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private VBox createDroneContent() {
        VBox content = new VBox(18);
        content.setPadding(new Insets(18, 4, 8, 4));

        Slider yawSlider = createSlider(YAW_MIN, YAW_MAX, model.getYawSensitivity());
        Label yawValue = new Label(formatPercent(model.getYawSensitivity()));
        yawSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            model.setYawSensitivity(newValue.doubleValue());
            yawValue.setText(formatPercent(newValue.doubleValue()));
        });

        Slider horizontalSlider = createSlider(
                HORIZONTAL_SPEED_MIN,
                HORIZONTAL_SPEED_MAX,
                model.getMaxHorizontalSpeed()
        );
        Label horizontalValue = new Label(formatMetersPerSecond(model.getMaxHorizontalSpeed()));
        horizontalSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            model.setMaxHorizontalSpeed(newValue.doubleValue());
            horizontalValue.setText(formatMetersPerSecond(newValue.doubleValue()));
        });

        Slider verticalSlider = createSlider(
                VERTICAL_SPEED_MIN,
                VERTICAL_SPEED_MAX,
                model.getMaxVerticalSpeed()
        );
        Label verticalValue = new Label(formatMetersPerSecond(model.getMaxVerticalSpeed()));
        verticalSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            model.setMaxVerticalSpeed(newValue.doubleValue());
            verticalValue.setText(formatMetersPerSecond(newValue.doubleValue()));
        });

        Button resetButton = new Button("Reset");
        resetButton.setFocusTraversable(false);
        resetButton.setMinWidth(Region.USE_PREF_SIZE);
        resetButton.setPrefWidth(Region.USE_COMPUTED_SIZE);
        resetButton.setMaxWidth(Region.USE_PREF_SIZE);
        resetButton.setStyle(
                "-fx-padding: 5 8;"
                        + "-fx-background-radius: 5;"
                        + "-fx-border-radius: 5;"
                        + "-fx-background-color: transparent;"
                        + "-fx-border-color: " + HUD_BORDER + ";"
                        + "-fx-text-fill: " + HUD_TEXT_MUTED + ";"
                        + "-fx-font-size: 12px;"
                        + "-fx-cursor: hand;"
        );
        resetButton.setOnAction(event -> {
            model.resetFlightSettings();
            yawSlider.setValue(model.getYawSensitivity());
            horizontalSlider.setValue(model.getMaxHorizontalSpeed());
            verticalSlider.setValue(model.getMaxVerticalSpeed());
        });

        HBox heading = createSectionHeader("Drone settings", resetButton);

        content.getChildren().addAll(
                heading,
                createSliderBlock("Yaw sensitivity", yawValue, yawSlider),
                createSliderBlock("Horizontal max speed", horizontalValue, horizontalSlider),
                createSliderBlock("Vertical max speed", verticalValue, verticalSlider)
        );

        return content;
    }

    private VBox createWorldContent() {
        VBox content = new VBox(16);
        content.setPadding(new Insets(18, 4, 8, 4));

        Label heading = new Label("World settings");
        heading.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: " + HUD_TEXT + ";");

        ToggleGroup sourceGroup = new ToggleGroup();

        RadioButton predefinedRadio = new RadioButton("Predefined world");
        predefinedRadio.setToggleGroup(sourceGroup);
        predefinedRadio.setFocusTraversable(false);
        styleRadioButton(predefinedRadio);

        ComboBox<String> configsCombo = new ComboBox<>();
        configsCombo.setMaxWidth(Double.MAX_VALUE);
        configsCombo.setFocusTraversable(false);
        configsCombo.setStyle(getComboBoxStyle());
        configsCombo.setButtonCell(createConfigListCell());
        configsCombo.setCellFactory(listView -> createConfigListCell());
        configsCombo.valueProperty().addListener((observable, oldValue, newValue) ->
                state.setSelectedWorldFilename(newValue)
        );
        refreshConfigsList(configsCombo);

        VBox predefinedBox = new VBox(8, predefinedRadio, configsCombo);
        predefinedBox.setPadding(new Insets(0, 0, 0, 2));

        RadioButton randomRadio = new RadioButton("Random generated world");
        randomRadio.setToggleGroup(sourceGroup);
        randomRadio.setFocusTraversable(false);
        styleRadioButton(randomRadio);

        ToggleGroup densityGroup = new ToggleGroup();
        ToggleButton lightweight = createDensityButton("Lightweight", WorldMode.FEW_OBSTACLES, densityGroup);
        ToggleButton balanced = createDensityButton("Balanced", WorldMode.MEDIUM_OBSTACLES, densityGroup);
        ToggleButton dense = createDensityButton("Dense", WorldMode.MANY_OBSTACLES, densityGroup);
        selectDensity(densityGroup);

        HBox densityButtons = new HBox(6, lightweight, balanced, dense);
        densityButtons.setAlignment(Pos.CENTER_LEFT);

        VBox randomBox = new VBox(8, randomRadio, createFieldLabel("Density:"), densityButtons);
        randomBox.setPadding(new Insets(0, 0, 0, 2));

        Label warning = new Label("Resets simulation to spawn.");
        warning.setWrapText(true);
        warning.setStyle("-fx-text-fill: " + HUD_WARNING + "; -fx-font-size: 12px;");

        Button applyButton = new Button("Apply world change");
        applyButton.setFocusTraversable(false);
        applyButton.setMaxWidth(Double.MAX_VALUE);
        applyButton.setStyle(
                "-fx-padding: 10 12;"
                        + "-fx-background-radius: 5;"
                        + "-fx-border-radius: 5;"
                        + "-fx-background-color: " + WORLD_ACTION_BACKGROUND + ";"
                        + "-fx-border-color: " + WORLD_ACTION_BORDER + ";"
                        + "-fx-text-fill: " + HUD_TEXT + ";"
                        + "-fx-font-weight: bold;"
                        + "-fx-cursor: hand;"
        );

        if (state.getWorldSource() == WorldSource.RANDOM) {
            randomRadio.setSelected(true);
        } else {
            predefinedRadio.setSelected(true);
        }

        sourceGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null && oldValue != null) {
                oldValue.setSelected(true);
                return;
            }

            state.setWorldSource(predefinedRadio.isSelected() ? WorldSource.PREDEFINED : WorldSource.RANDOM);
            updateWorldSourceControls(predefinedRadio, configsCombo, densityButtons);
        });
        densityGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null && oldValue != null) {
                oldValue.setSelected(true);
                return;
            }

            if (newValue != null) {
                state.setSelectedWorldMode((WorldMode) newValue.getUserData());
            }
        });
        updateWorldSourceControls(predefinedRadio, configsCombo, densityButtons);

        applyButton.setOnAction(event -> applyWorldChange(predefinedRadio, configsCombo, densityGroup));

        content.getChildren().addAll(
                heading,
                createFieldLabel("World mode"),
                predefinedBox,
                randomBox,
                warning,
                applyButton
        );

        return content;
    }

    private Slider createSlider(double min, double max, double value) {
        Slider slider = new Slider(min, max, value);
        slider.setFocusTraversable(false);
        slider.setMaxWidth(Double.MAX_VALUE);
        slider.setShowTickMarks(false);
        slider.setShowTickLabels(false);
        slider.setStyle(
                "-track-color: " + CONTROL_BACKGROUND + ";"
                        + "-fx-control-inner-background: " + CONTROL_BACKGROUND + ";"
                        + "-fx-accent: " + HUD_ACCENT + ";"
        );
        return slider;
    }

    private VBox createSliderBlock(String labelText, Label valueLabel, Slider slider) {
        Label label = createFieldLabel(labelText);
        valueLabel.setStyle("-fx-text-fill: " + HUD_TEXT_MUTED + "; -fx-font-size: 12px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(8, label, spacer, valueLabel);
        header.setAlignment(Pos.CENTER_LEFT);

        VBox block = new VBox(7, header, slider);
        block.setMaxWidth(Double.MAX_VALUE);
        return block;
    }

    private ToggleButton createDensityButton(
            String text,
            WorldMode mode,
            ToggleGroup densityGroup
    ) {
        ToggleButton button = new ToggleButton(text);
        button.setUserData(mode);
        button.setToggleGroup(densityGroup);
        button.setFocusTraversable(false);
        button.setMaxWidth(Double.MAX_VALUE);
        button.setStyle(
                getDensityButtonStyle(false)
        );
        button.selectedProperty().addListener((observable, oldValue, selected) ->
                button.setStyle(getDensityButtonStyle(selected))
        );
        HBox.setHgrow(button, Priority.ALWAYS);
        return button;
    }

    private void selectDensity(ToggleGroup densityGroup) {
        for (javafx.scene.control.Toggle toggle : densityGroup.getToggles()) {
            if (toggle.getUserData() == state.getSelectedWorldMode()) {
                toggle.setSelected(true);
                return;
            }
        }

        if (!densityGroup.getToggles().isEmpty()) {
            densityGroup.getToggles().get(0).setSelected(true);
        }
    }

    private void updateWorldSourceControls(
            RadioButton predefinedRadio,
            ComboBox<String> configsCombo,
            HBox densityButtons
    ) {
        boolean predefinedSelected = predefinedRadio.isSelected();
        configsCombo.setDisable(!predefinedSelected);
        densityButtons.setDisable(predefinedSelected);
    }

    private String getDensityButtonStyle(boolean selected) {
        if (selected) {
            return "-fx-padding: 8 9;"
                    + "-fx-background-radius: 5;"
                    + "-fx-border-radius: 5;"
                    + "-fx-border-color: " + HUD_ACCENT + ";"
                    + "-fx-background-color: " + CONTROL_BACKGROUND + ";"
                    + "-fx-text-fill: " + HUD_TEXT + ";"
                    + "-fx-font-weight: bold;"
                    + "-fx-cursor: hand;";
        }

        return "-fx-padding: 8 9;"
                + "-fx-background-radius: 5;"
                + "-fx-border-radius: 5;"
                + "-fx-border-color: " + HUD_BORDER + ";"
                + "-fx-background-color: " + CONTROL_BACKGROUND_SOFT + ";"
                + "-fx-text-fill: " + HUD_TEXT_MUTED + ";"
                + "-fx-cursor: hand;";
    }

    private Label createFieldLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: " + HUD_TEXT + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        return label;
    }

    private void styleRadioButton(RadioButton radioButton) {
        radioButton.setStyle(
                "-fx-text-fill: " + HUD_TEXT + ";"
                        + "-fx-mark-color: " + HUD_ACCENT + ";"
        );
    }

    private String getComboBoxStyle() {
        return "-fx-background-color: " + CONTROL_BACKGROUND + ";"
                + "-fx-background-radius: 5;"
                + "-fx-border-color: " + HUD_BORDER + ";"
                + "-fx-border-radius: 5;"
                + "-fx-mark-color: " + HUD_TEXT + ";"
                + "-fx-font-size: 12px;";
    }

    private ListCell<String> createConfigListCell() {
        return new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                }

                setStyle(
                        "-fx-background-color: " + CONTROL_BACKGROUND + ";"
                                + "-fx-text-fill: " + HUD_TEXT + ";"
                );
            }
        };
    }

    private void refreshConfigsList(ComboBox<String> configsCombo) {
        String[] saves = WorldPersistence.listSaves();
        Arrays.sort(saves);
        configsCombo.getItems().setAll(saves);

        String preferredFilename = state.getSelectedWorldFilename();

        if (preferredFilename != null && configsCombo.getItems().contains(preferredFilename)) {
            configsCombo.getSelectionModel().select(preferredFilename);
        } else if (configsCombo.getItems().contains(currentConfigFilename)) {
            configsCombo.getSelectionModel().select(currentConfigFilename);
        } else if (!configsCombo.getItems().isEmpty()) {
            configsCombo.getSelectionModel().selectFirst();
        }

        state.setSelectedWorldFilename(configsCombo.getValue());
    }

    private void applyWorldChange(
            RadioButton predefinedRadio,
            ComboBox<String> configsCombo,
            ToggleGroup densityGroup
    ) {
        if (predefinedRadio.isSelected()) {
            loadSelectedWorld(configsCombo);
        } else {
            generateRandomWorld(densityGroup);
        }
    }

    private void loadSelectedWorld(ComboBox<String> configsCombo) {
        String selected = configsCombo.getValue();
        if (selected == null || selected.isBlank()) {
            showError("Please select a world configuration.");
            return;
        }

        try {
            WorldConfiguration config = WorldPersistence.loadWorldConfiguration(selected);
            if (config == null) {
                showError("World configuration not found.");
                return;
            }
            onWorldApplied.accept(config, selected);
        } catch (IOException | IllegalArgumentException ex) {
            showError("Failed to load world: " + ex.getMessage());
        }
    }

    private void generateRandomWorld(ToggleGroup densityGroup) {
        if (densityGroup.getSelectedToggle() == null) {
            showError("Please select a density.");
            return;
        }

        WorldMode selectedMode = (WorldMode) densityGroup.getSelectedToggle().getUserData();
        currentConfig.generateRandomObstacles(selectedMode.getObstacleCount());
        onWorldApplied.accept(currentConfig, currentConfigFilename);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Settings");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void closeImmediately() {
        open = false;
        drawer.setTranslateX(CLOSED_TRANSLATE_X);
        drawer.setVisible(false);
        drawer.setMouseTransparent(true);
        clickOutsideLayer.setVisible(false);
        clickOutsideLayer.setMouseTransparent(true);
        settingsButton.setVisible(true);
        settingsButton.setMouseTransparent(false);
    }

    private void runTransition(double targetX, Runnable onFinished) {
        transition = new TranslateTransition(Duration.millis(180), drawer);
        transition.setToX(targetX);
        transition.setOnFinished(event -> {
            transition = null;
            if (onFinished != null) {
                onFinished.run();
            }
        });
        transition.play();
    }

    private void stopTransition() {
        if (transition != null) {
            transition.stop();
            transition = null;
        }
    }

    private String formatPercent(double value) {
        return String.format("%.0f%%", value * 100.0);
    }

    private String formatMetersPerSecond(double value) {
        return String.format("%.0f m/s", value);
    }
}
