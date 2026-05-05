package fr.enac.drone.view;

import fr.enac.drone.model.WorldMode;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * Settings screen used to select the obstacle density of the simulation world.
 */

public class SettingsView extends BorderPane {

    // Group used to ensure that only one world mode can be selected at a time
    private final ToggleGroup worldGroup = new ToggleGroup();

    private final RadioButton fewButton = new RadioButton();
    private final RadioButton mediumButton = new RadioButton();
    private final RadioButton manyButton = new RadioButton();

    private final Button backButton = new Button("←");
    private final Button saveButton = new Button("Enregistrer");

    /**
    * Builds the settings screen and initializes the selected world mode.
    */
    public SettingsView(WorldMode currentMode) {
        setBackground(new Background(new BackgroundFill(
                Color.rgb(67, 66, 80),
                CornerRadii.EMPTY,
                Insets.EMPTY
        )));

        createTopBar();
        createWorldOptions(currentMode);
        createBottomBar();
    }

    /**
    * Creates the top section containing the back button and the screen title.
    */
    private void createTopBar() {
        backButton.setTextFill(Color.WHITE);
        backButton.setFont(Font.font("System", FontWeight.BOLD, 28));
        backButton.setBackground(Background.EMPTY);
        backButton.setBorder(Border.EMPTY);

        javafx.scene.control.Label title = new javafx.scene.control.Label("Choose the world");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("System", FontWeight.BOLD, 26));

        HBox topBar = new HBox(20, backButton, title);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(25, 30, 20, 30));

        setTop(topBar);
    }

    /**
    * Creates the radio button options used to choose the obstacle density.
    */
    private void createWorldOptions(WorldMode currentMode) {
        fewButton.setToggleGroup(worldGroup);
        mediumButton.setToggleGroup(worldGroup);
        manyButton.setToggleGroup(worldGroup);

        fewButton.setUserData(WorldMode.FEW_OBSTACLES);
        mediumButton.setUserData(WorldMode.MEDIUM_OBSTACLES);
        manyButton.setUserData(WorldMode.MANY_OBSTACLES);

        // Keep the current world mode selected when opening the settings screen
        fewButton.setSelected(currentMode == WorldMode.FEW_OBSTACLES);
        mediumButton.setSelected(currentMode == WorldMode.MEDIUM_OBSTACLES);
        manyButton.setSelected(currentMode == WorldMode.MANY_OBSTACLES);

        VBox optionsBox = new VBox(22);
        optionsBox.setPadding(new Insets(20, 45, 20, 45));
        optionsBox.setAlignment(Pos.TOP_LEFT);

        optionsBox.getChildren().addAll(
                createOptionRow(
                        fewButton,
                        "Lightweight world",
                        "Few obstacles for freer flight and easy handling."
                ),
                createOptionRow(
                        mediumButton,
                        "Balanced world",
                        "An average number of obstacles for a more realistic course."
                ),
                createOptionRow(
                        manyButton,
                        "Dense world",
                        "Many obstacles for a more difficult piloting challenge."
                )
        );

        setCenter(optionsBox);
    }

    /**
    * Creates one selectable row composed of a radio button, a title, and a description.
    */
    private HBox createOptionRow(RadioButton radioButton, String titleText, String descriptionText) {
        radioButton.setTextFill(Color.WHITE);

        javafx.scene.control.Label title = new javafx.scene.control.Label(titleText);
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("System", FontWeight.BOLD, 17));

        javafx.scene.control.Label description = new javafx.scene.control.Label(descriptionText);
        description.setTextFill(Color.rgb(232, 230, 241));
        description.setFont(Font.font("System", 14));
        description.setWrapText(true);

        VBox textBox = new VBox(5, title, description);
        textBox.setAlignment(Pos.CENTER_LEFT);

        HBox row = new HBox(14, radioButton, textBox);
        row.setAlignment(Pos.CENTER_LEFT);

        return row;
    }

    /**
    * Creates the bottom section containing the save button.
    */
    private void createBottomBar() {
        saveButton.setTextFill(Color.WHITE);
        saveButton.setFont(Font.font("System", FontWeight.BOLD, 16));
        saveButton.setPadding(new Insets(12, 28, 12, 28));
        saveButton.setBackground(new Background(new BackgroundFill(
                Color.rgb(29, 26, 48),
                new CornerRadii(20),
                Insets.EMPTY
        )));

        HBox bottomBar = new HBox(saveButton);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setPadding(new Insets(20, 35, 35, 35));

        setBottom(bottomBar);
    }

    public Button getBackButton() {
        return backButton;
    }

    public Button getSaveButton() {
        return saveButton;
    }

    public WorldMode getSelectedWorldMode() {
        return (WorldMode) worldGroup.getSelectedToggle().getUserData();
    }
}