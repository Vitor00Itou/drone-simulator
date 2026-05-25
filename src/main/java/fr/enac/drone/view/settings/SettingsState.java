package fr.enac.drone.view.settings;

import fr.enac.drone.input.KeyboardLayout;
import fr.enac.drone.model.world.WorldGenerationMode;

import java.util.Objects;

/**
 * Stores settings drawer selections so they survive view reconstruction.
 */
public class SettingsState {
    private SettingsSection activeSection = SettingsSection.DRONE;
    private SettingsWorldSource worldSource = SettingsWorldSource.PREDEFINED;
    private String selectedWorldFilename;
    private WorldGenerationMode selectedWorldMode = WorldGenerationMode.MEDIUM_OBSTACLES;
    private boolean showCommandHelp = true;
    private KeyboardLayout keyboardLayout = KeyboardLayout.QWERTY;

    /**
     * Creates settings state with the default drawer, world, HUD, and keyboard selections.
     */
    public SettingsState() {
    }

    /**
     * Returns the currently selected drawer tab.
     *
     * @return active settings section
     */
    public SettingsSection getActiveSection() {
        return activeSection;
    }

    /**
     * Sets the currently selected drawer tab.
     *
     * @param activeSection active settings section
     */
    public void setActiveSection(SettingsSection activeSection) {
        this.activeSection = Objects.requireNonNull(activeSection);
    }

    /**
     * Returns the selected world source mode.
     *
     * @return predefined or random source
     */
    public SettingsWorldSource getWorldSource() {
        return worldSource;
    }

    /**
     * Sets the selected world source mode.
     *
     * @param worldSource selected source mode
     */
    public void setWorldSource(SettingsWorldSource worldSource) {
        this.worldSource = Objects.requireNonNull(worldSource);
    }

    /**
     * Stores the selected saved-world filename.
     *
     * @param selectedWorldFilename filename without extension
     */
    public void setSelectedWorldFilename(String selectedWorldFilename) {
        this.selectedWorldFilename = selectedWorldFilename;
    }

    /**
     * Returns the selected saved-world filename.
     *
     * @return filename without extension, or {@code null}
     */
    public String getSelectedWorldFilename() {
        return selectedWorldFilename;
    }

    /**
     * Returns the selected random-world generation mode.
     *
     * @return world generation mode
     */
    public WorldGenerationMode getSelectedWorldMode() {
        return selectedWorldMode;
    }

    /**
     * Sets the selected random-world generation mode.
     *
     * @param selectedWorldMode generation mode
     */
    public void setSelectedWorldMode(WorldGenerationMode selectedWorldMode) {
        this.selectedWorldMode = Objects.requireNonNull(selectedWorldMode);
    }

    /**
     * Returns whether the command help panel should be shown.
     *
     * @return {@code true} when command help is visible
     */
    public boolean isShowCommandHelp() {
        return showCommandHelp;
    }

    /**
     * Sets whether the command help panel should be shown.
     *
     * @param showCommandHelp {@code true} to show command help
     */
    public void setShowCommandHelp(boolean showCommandHelp) {
        this.showCommandHelp = showCommandHelp;
    }

    /**
     * Returns the selected keyboard layout.
     *
     * @return active keyboard layout
     */
    public KeyboardLayout getKeyboardLayout() {
        return keyboardLayout;
    }

    /**
     * Sets the selected keyboard layout.
     *
     * @param keyboardLayout layout to store, or {@code null} to restore QWERTY
     */
    public void setKeyboardLayout(KeyboardLayout keyboardLayout) {
        this.keyboardLayout = keyboardLayout == null ? KeyboardLayout.QWERTY : keyboardLayout;
    }
}
