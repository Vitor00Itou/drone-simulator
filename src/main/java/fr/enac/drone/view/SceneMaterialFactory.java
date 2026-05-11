package fr.enac.drone.view;

import fr.enac.drone.model.world.WorldObject;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Centralizes reusable JavaFX 3D materials for the simulation scene.
 */
final class SceneMaterialFactory {
    private static final String TEXTURE_ROOT = "/textures/";
    private static final Color TEXTURE_DIFFUSE_COLOR = Color.gray(0.78);

    private final Map<String, PhongMaterial> customTextureMaterials = new HashMap<>();
    private final Map<String, PhongMaterial> solidObjectMaterials = new HashMap<>();
    private final Map<String, Optional<Image>> textureImages = new HashMap<>();
    private PhongMaterial droneMaterial;

    PhongMaterial getMaterial(WorldObject object) {
        Objects.requireNonNull(object, "World object cannot be null");

        if (object.getTexture() != null) {
            return createTexturedObjectMaterial(object);
        }

        return createSolidObjectMaterial(object);
    }

    Optional<Image> getTextureImage(WorldObject object) {
        Objects.requireNonNull(object, "World object cannot be null");

        return loadTexture(object.getTexture());
    }

    PhongMaterial createDroneMaterial() {
        if (droneMaterial == null) {
            droneMaterial = createMaterial(
                Color.web("#D62828"),
                Color.web("#F5A3A3"),
                32.0
            );
        }

        return droneMaterial;
    }

    private PhongMaterial createTexturedObjectMaterial(WorldObject object) {
        String key = object.getTexture() + "|" + object.getColor() + "|" + object.getType();
        PhongMaterial cachedMaterial = customTextureMaterials.get(key);
        if (cachedMaterial != null) {
            return cachedMaterial;
        }

        PhongMaterial material = createTexturedMaterial(
            object.getTexture(),
            Color.web(object.getColor()),
            getSpecularColor(object),
            getSpecularPower(object)
        );

        customTextureMaterials.put(key, material);
        return material;
    }

    private PhongMaterial createSolidObjectMaterial(WorldObject object) {
        String key = object.getColor() + "|" + object.getType();
        PhongMaterial cachedMaterial = solidObjectMaterials.get(key);
        if (cachedMaterial != null) {
            return cachedMaterial;
        }

        Color diffuseColor = Color.web(object.getColor());
        PhongMaterial material = createMaterial(
            diffuseColor,
            getSpecularColor(object),
            getSpecularPower(object)
        );

        solidObjectMaterials.put(key, material);
        return material;
    }

    private PhongMaterial createTexturedMaterial(
            String textureFileName,
            Color fallbackDiffuseColor,
            Color specularColor,
            double specularPower
    ) {
        Optional<Image> texture = loadTexture(textureFileName);
        if (texture.isEmpty()) {
            return createMaterial(fallbackDiffuseColor, specularColor, specularPower);
        }

        PhongMaterial material = createMaterial(TEXTURE_DIFFUSE_COLOR, specularColor, specularPower);
        material.setDiffuseMap(texture.get());
        return material;
    }

    private PhongMaterial createMaterial(Color diffuseColor, Color specularColor, double specularPower) {
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(diffuseColor);
        material.setSpecularColor(specularColor);
        material.setSpecularPower(specularPower);
        return material;
    }

    private Color getSpecularColor(WorldObject object) {
        return switch (object.getType().toLowerCase(Locale.ROOT)) {
            case "cylinder" -> Color.web("#E2E8EC");
            case "box" -> Color.web("#BFC5C0");
            case "plane" -> Color.web("#1F2A1F");
            default -> Color.web("#303030");
        };
    }

    private double getSpecularPower(WorldObject object) {
        return switch (object.getType().toLowerCase(Locale.ROOT)) {
            case "cylinder" -> 48.0;
            case "box" -> 16.0;
            case "plane" -> 4.0;
            default -> 8.0;
        };
    }

    private Optional<Image> loadTexture(String textureFileName) {
        if (textureFileName == null) {
            return Optional.empty();
        }

        Optional<Image> cachedTexture = textureImages.get(textureFileName);
        if (cachedTexture != null) {
            return cachedTexture;
        }

        Optional<Image> texture = loadTextureResource(textureFileName);
        textureImages.put(textureFileName, texture);
        return texture;
    }

    private Optional<Image> loadTextureResource(String textureFileName) {
        String resourcePath = TEXTURE_ROOT + textureFileName;

        try (InputStream stream = SceneMaterialFactory.class.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                System.err.println("Texture not found: " + resourcePath + ". Using fallback material.");
                return Optional.empty();
            }

            Image texture = new Image(stream);
            if (texture.isError()) {
                Throwable exception = texture.getException();
                String reason = exception == null ? "unknown error" : exception.getMessage();
                System.err.println("Texture could not be loaded: " + resourcePath + " (" + reason + "). Using fallback material.");
                return Optional.empty();
            }

            return Optional.of(texture);
        } catch (IOException | RuntimeException e) {
            System.err.println("Texture could not be loaded: " + resourcePath + " (" + e.getMessage() + "). Using fallback material.");
            return Optional.empty();
        }
    }
}
