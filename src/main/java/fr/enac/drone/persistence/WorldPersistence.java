package fr.enac.drone.persistence;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.enac.drone.model.world.WorldConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Saves, loads, lists, and deletes world configurations stored as JSON files.
 */
public class WorldPersistence {
    private static final Logger LOGGER = Logger.getLogger(WorldPersistence.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    private static final Path SAVES_DIRECTORY = Paths.get("saves");

    static {
        try {
            Files.createDirectories(SAVES_DIRECTORY);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to create saves directory", e);
        }
    }

    /**
     * Creates a persistence helper instance.
     */
    public WorldPersistence() {
    }

    /**
     * Validates a save filename to prevent path traversal attacks.
     *
     * @param filename filename without path separators
     * @throws IllegalArgumentException if the filename is invalid
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
     * Saves a world configuration to a JSON file.
     *
     * @param config world configuration to save
     * @param filename filename without the {@code .json} extension
     * @throws IllegalArgumentException if the configuration or filename is invalid
     * @throws IOException if writing fails
     */
    public static void saveWorldConfiguration(WorldConfiguration config, String filename) throws IOException {
        Objects.requireNonNull(config, "Configuration cannot be null");
        validateFilename(filename);
        
        Path filepath = SAVES_DIRECTORY.resolve(filename + ".json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(filepath.toFile(), config);
        LOGGER.fine(() -> "World configuration saved to: " + filepath);
    }

    /**
     * Loads a world configuration from a JSON file.
     *
     * @param filename filename without the {@code .json} extension
     * @return loaded configuration, or {@code null} when the file does not exist
     * @throws IllegalArgumentException if the filename is invalid
     * @throws IOException if reading or parsing fails
     */
    public static WorldConfiguration loadWorldConfiguration(String filename) throws IOException {
        validateFilename(filename);
        
        Path filepath = SAVES_DIRECTORY.resolve(filename + ".json");
        File file = filepath.toFile();
        
        if (!file.exists()) {
            LOGGER.fine(() -> "World configuration file not found: " + filepath);
            return null;
        }
        
        WorldConfiguration config = objectMapper.readValue(file, WorldConfiguration.class);
        
        config.updateSpawnBase();
        
        LOGGER.fine(() -> "World configuration loaded from: " + filepath);
        return config;
    }

    /**
     * Lists available saved world configurations.
     *
     * @return save file names without the {@code .json} extension
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
     * Deletes a saved world configuration.
     *
     * @param filename filename without the {@code .json} extension
     * @return {@code true} if a file was deleted
     * @throws IllegalArgumentException if the filename is invalid
     */
    public static boolean deleteSave(String filename) {
        validateFilename(filename);
        
        Path filepath = SAVES_DIRECTORY.resolve(filename + ".json");
        try {
            return Files.deleteIfExists(filepath);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to delete save: " + filepath, e);
            return false;
        }
    }
}
