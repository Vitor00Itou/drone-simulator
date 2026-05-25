package fr.enac.drone.view;

import fr.enac.drone.model.world.WorldObject;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Creates and caches reusable JavaFX 3D materials for the simulation scene.
 */
final class SceneMaterialFactory {
    private static final String TEXTURE_ROOT = "/textures/";
    private static final Color TEXTURE_DIFFUSE_COLOR = Color.gray(0.78);

    private final Map<String, PhongMaterial> customTextureMaterials = new HashMap<>();
    private final Map<String, PhongMaterial> solidObjectMaterials = new HashMap<>();
    private final Map<String, Optional<Image>> textureImages = new HashMap<>();
    private PhongMaterial droneMaterial;

    /**
     * Returns a cached material for a world object.
     *
     * @param object world object to render
     * @return material using the object's texture or fallback color
     */
    PhongMaterial getMaterial(WorldObject object) {
        Objects.requireNonNull(object, "World object cannot be null");

        if (object.getTexture() != null) {
            return createTexturedObjectMaterial(object);
        }

        return createSolidObjectMaterial(object);
    }

    /**
     * Returns a cached texture image for a world object.
     *
     * @param object world object whose texture should be loaded
     * @return texture image when configured and loadable
     */
    Optional<Image> getTextureImage(WorldObject object) {
        Objects.requireNonNull(object, "World object cannot be null");

        return loadTexture(object.getTexture());
    }

    /**
     * Returns the shared material used for the drone body.
     *
     * @return drone material
     */
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

    /**
     * Creates or retrieves a material that uses a diffuse texture.
     *
     * @param object textured world object
     * @return cached textured material
     */
    private PhongMaterial createTexturedObjectMaterial(WorldObject object) {
        String key = object.getTexture() + "|" + object.getColor() + "|" + object.getType();
        PhongMaterial cachedMaterial = customTextureMaterials.get(key);
        if (cachedMaterial != null) {
            return cachedMaterial;
        }

        PhongMaterial material = createTexturedMaterial(
            object.getTexture(),
            parseColor(object.getColor(), Color.GRAY),
            getSpecularColor(object),
            getSpecularPower(object)
        );

        customTextureMaterials.put(key, material);
        return material;
    }

    /**
     * Creates or retrieves a solid-color material.
     *
     * @param object world object rendered without a texture
     * @return cached solid material
     */
    private PhongMaterial createSolidObjectMaterial(WorldObject object) {
        String key = object.getColor() + "|" + object.getType();
        PhongMaterial cachedMaterial = solidObjectMaterials.get(key);
        if (cachedMaterial != null) {
            return cachedMaterial;
        }

        Color diffuseColor = parseColor(object.getColor(), Color.GRAY);
        PhongMaterial material = createMaterial(
            diffuseColor,
            getSpecularColor(object),
            getSpecularPower(object)
        );

        solidObjectMaterials.put(key, material);
        return material;
    }

    /**
     * Creates a material from a texture, or falls back to a solid material if loading fails.
     *
     * @param textureFileName texture file under {@code /textures}
     * @param fallbackDiffuseColor diffuse color used when the texture is unavailable
     * @param specularColor specular highlight color
     * @param specularPower specular highlight power
     * @return configured material
     */
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

    /**
     * Creates a plain Phong material.
     *
     * @param diffuseColor diffuse material color
     * @param specularColor specular highlight color
     * @param specularPower specular highlight power
     * @return configured material
     */
    private PhongMaterial createMaterial(Color diffuseColor, Color specularColor, double specularPower) {
        PhongMaterial material = new PhongMaterial();
        material.setDiffuseColor(diffuseColor);
        material.setSpecularColor(specularColor);
        material.setSpecularPower(specularPower);
        return material;
    }

    /**
     * Parses a JavaFX color string.
     *
     * @param colorValue RGB hex value or JavaFX color name
     * @param fallbackColor color used when parsing fails
     * @return parsed color or fallback color
     */
    private Color parseColor(String colorValue, Color fallbackColor) {
        try {
            return Color.web(colorValue);
        } catch (IllegalArgumentException | NullPointerException e) {
            System.err.println("Invalid color '" + colorValue + "'. Using fallback material color.");
            return fallbackColor;
        }
    }

    /**
     * Chooses specular color according to the object type.
     *
     * @param object world object to render
     * @return specular color
     */
    private Color getSpecularColor(WorldObject object) {
        return switch (object.getType()) {
            case CYLINDER -> Color.web("#E2E8EC");
            case BOX -> Color.web("#BFC5C0");
            case PLANE -> Color.web("#1F2A1F");
        };
    }

    /**
     * Chooses specular power according to the object type.
     *
     * @param object world object to render
     * @return specular power
     */
    private double getSpecularPower(WorldObject object) {
        return switch (object.getType()) {
            case CYLINDER -> 48.0;
            case BOX -> 16.0;
            case PLANE -> 4.0;
        };
    }

    /**
     * Loads a texture image from cache or resources.
     *
     * @param textureFileName texture file under {@code /textures}
     * @return loaded texture image, or empty when unavailable
     */
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

    /**
     * Loads a texture image from the application resources.
     *
     * @param textureFileName texture file under {@code /textures}
     * @return loaded texture image, or empty when unavailable
     */
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
