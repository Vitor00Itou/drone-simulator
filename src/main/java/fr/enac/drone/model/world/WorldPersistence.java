package fr.enac.drone.model.world;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Handles saving and loading world state to/from JSON files.
 */
public class WorldPersistence {
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    private static final Path SAVES_DIRECTORY = Paths.get("saves");

    static {
        try {
            Files.createDirectories(SAVES_DIRECTORY);
        } catch (IOException e) {
            System.err.println("Failed to create saves directory: " + e.getMessage());
        }
    }

    /**
     * Validates filename to prevent path traversal attacks.
     * @param filename The filename to validate
     * @throws IllegalArgumentException if filename is invalid
     */
    private static void validateFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
        
        // Prevent path traversal attacks
        if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("Invalid filename: contains path separators or parent directory references");
        }
        
        // Prevent null bytes
        if (filename.contains("\0")) {
            throw new IllegalArgumentException("Invalid filename: contains null bytes");
        }
    }

    /**
     * Save world configuration to a JSON file.
     * @param config The world configuration to save
     * @param filename The filename (without .json extension)
     * @throws IllegalArgumentException if config or filename is invalid
     * @throws IOException if save fails
     */
    public static void saveWorldConfiguration(WorldConfiguration config, String filename) throws IOException {
        Objects.requireNonNull(config, "Configuration cannot be null");
        validateFilename(filename);
        
        Path filepath = SAVES_DIRECTORY.resolve(filename + ".json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(filepath.toFile(), config);
        System.out.println("World configuration saved to: " + filepath);
    }

    /**
     * Load world configuration from a JSON file.
     * @param filename The filename (without .json extension)
     * @return The loaded WorldConfiguration, or null if file doesn't exist
     * @throws IllegalArgumentException if filename is invalid
     * @throws IOException if load fails
     */
    public static WorldConfiguration loadWorldConfiguration(String filename) throws IOException {
        validateFilename(filename);
        
        Path filepath = SAVES_DIRECTORY.resolve(filename + ".json");
        File file = filepath.toFile();
        
        if (!file.exists()) {
            System.err.println("World configuration file not found: " + filepath);
            return null;
        }
        
        WorldConfiguration config = objectMapper.readValue(file, WorldConfiguration.class);
        
        config.updateSpawnBase();
        
        System.out.println("World configuration loaded from: " + filepath);
        return config;
    }

    /**
     * Get list of available save files.
     * @return Array of save file names (without .json extension)
     */
    public static String[] listSaves() {
        File dir = SAVES_DIRECTORY.toFile();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        
        if (files == null || files.length == 0) {
            return new String[0];
        }
        
        String[] names = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            names[i] = files[i].getName().replace(".json", "");
        }
        return names;
    }

    /**
     * Delete a save file.
     * @param filename The filename (without .json extension)
     * @return true if file was deleted, false otherwise
     * @throws IllegalArgumentException if filename is invalid
     */
    public static boolean deleteSave(String filename) {
        validateFilename(filename);
        
        Path filepath = SAVES_DIRECTORY.resolve(filename + ".json");
        try {
            return Files.deleteIfExists(filepath);
        } catch (IOException e) {
            System.err.println("Failed to delete save: " + e.getMessage());
            return false;
        }
    }
}
