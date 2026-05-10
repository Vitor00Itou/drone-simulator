package fr.enac.drone.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.Objects;
import java.util.function.BiConsumer;

import fr.enac.drone.model.world.WorldConfiguration;
import fr.enac.drone.model.world.WorldPersistence;

/**
 * Menu dialog for managing world configurations (save/load).
 */
public class WorldConfigMenu extends Dialog<Void> {
    private ComboBox<String> configsCombo;
    private String currentConfigFilename;
    private BiConsumer<WorldConfiguration, String> onConfigurationLoaded;

    public WorldConfigMenu(WorldConfiguration currentConfig, String currentConfigFilename, BiConsumer<WorldConfiguration, String> onConfigurationLoaded) {
        this.currentConfigFilename = currentConfigFilename;
        this.onConfigurationLoaded = Objects.requireNonNull(onConfigurationLoaded, "Callback cannot be null");
        this.setTitle("World Configuration");
        this.setWidth(600);
        this.setHeight(440);

        // Create content
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        VBox loadSection = createLoadSection();

        content.getChildren().add(loadSection);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        this.getDialogPane().setContent(scrollPane);

        // Buttons
        this.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
    }

    private VBox createLoadSection() {
        VBox section = new VBox(10);
        
        Label title = new Label("Load World Configuration");
        title.setStyle("-fx-font-size: 12; -fx-font-weight: bold;");
        
        HBox selectionBox = new HBox(8);
        configsCombo = new ComboBox<>();
        configsCombo.setPrefWidth(250);
        refreshConfigsList();
        
        selectionBox.getChildren().addAll(
            new Label("Select:"),
            configsCombo
        );
        
        HBox actionBox = new HBox(8);
        Button loadBtn = new Button("Load Configuration");
        loadBtn.setStyle("-fx-padding: 8px 15px;");
        loadBtn.setOnAction(e -> {
            try {
                loadConfigurationAction();
                this.close();
            } catch (IOException ex) {
                showError("Failed to load: " + ex.getMessage());
            }
        });
        
        actionBox.getChildren().add(loadBtn);
        
        section.getChildren().addAll(title, selectionBox, actionBox);
        return section;
    }

    private void loadConfigurationAction() throws IOException {
        String selected = configsCombo.getValue();
        if (selected == null || selected.isEmpty()) {
            showError("Please select a configuration");
            return;
        }

        WorldConfiguration config = WorldPersistence.loadWorldConfiguration(selected);
        if (config != null) {            
            if (onConfigurationLoaded != null) {
                onConfigurationLoaded.accept(config, selected);
            }
        }
    }

    private void refreshConfigsList() {
        String[] saves = WorldPersistence.listSaves();
        configsCombo.getItems().clear();
        configsCombo.getItems().addAll(saves);
        
        if (saves.length > 0) {
            if (currentConfigFilename != null && configsCombo.getItems().contains(currentConfigFilename)) {
                configsCombo.getSelectionModel().select(currentConfigFilename);
            } else {
                configsCombo.getSelectionModel().selectFirst();
            }
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
