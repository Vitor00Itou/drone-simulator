package fr.enac.drone.view.settings;

import fr.enac.drone.input.KeyboardLayout;
import fr.enac.drone.model.world.WorldGenerationMode;

import java.util.Objects;

public class SettingsState {
    private SettingsSection activeSection = SettingsSection.DRONE;
    private SettingsWorldSource worldSource = SettingsWorldSource.PREDEFINED;
    private String selectedWorldFilename;
    private WorldGenerationMode selectedWorldMode = WorldGenerationMode.MEDIUM_OBSTACLES;
    private boolean showCommandHelp = true;
    private KeyboardLayout keyboardLayout = KeyboardLayout.QWERTY;

    public SettingsSection getActiveSection() {
        return activeSection;
    }

    public void setActiveSection(SettingsSection activeSection) {
        this.activeSection = Objects.requireNonNull(activeSection);
    }

    public SettingsWorldSource getWorldSource() {
        return worldSource;
    }

    public void setWorldSource(SettingsWorldSource worldSource) {
        this.worldSource = Objects.requireNonNull(worldSource);
    }

    public void setSelectedWorldFilename(String selectedWorldFilename) {
        this.selectedWorldFilename = selectedWorldFilename;
    }

    public String getSelectedWorldFilename() {
        return selectedWorldFilename;
    }

    public WorldGenerationMode getSelectedWorldMode() {
        return selectedWorldMode;
    }

    public void setSelectedWorldMode(WorldGenerationMode selectedWorldMode) {
        this.selectedWorldMode = Objects.requireNonNull(selectedWorldMode);
    }

    public boolean isShowCommandHelp() {
        return showCommandHelp;
    }

    public void setShowCommandHelp(boolean showCommandHelp) {
        this.showCommandHelp = showCommandHelp;
    }

    public KeyboardLayout getKeyboardLayout() {
        return keyboardLayout;
    }

    public void setKeyboardLayout(KeyboardLayout keyboardLayout) {
        this.keyboardLayout = keyboardLayout == null ? KeyboardLayout.QWERTY : keyboardLayout;
    }
}
