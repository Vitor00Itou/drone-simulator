package fr.enac.drone.persistence;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import fr.enac.drone.model.world.WorldConfiguration;
import fr.enac.drone.model.world.WorldObject;
import fr.enac.drone.model.world.WorldObjectType;

class WorldPersistenceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Test
    void loadsExistingWorldFilesWithLowercaseObjectTypes() throws IOException {
        WorldConfiguration config = WorldPersistence.loadWorldConfiguration("world_default");

        assertNotNull(config);
        assertTrue(config.getObjects().stream().anyMatch(WorldObject::isPlane));
        assertTrue(config.getObjects().stream().anyMatch(WorldObject::isCylinder));
    }

    @Test
    void serializesWorldObjectWithoutComputedGeometryFields() throws IOException {
        WorldObject ground = new WorldObject(
                "Ground",
                WorldObjectType.PLANE,
                0.0,
                0.0,
                0.0,
                5000.0,
                1.0,
                5000.0,
                "#228B22"
        );

        String json = OBJECT_MAPPER.writeValueAsString(ground);

        assertTrue(json.contains("\"type\":\"plane\""));
        assertFalse(json.contains("radius"));
        assertFalse(json.contains("topY"));
        assertFalse(json.contains("footprint"));
    }
}
