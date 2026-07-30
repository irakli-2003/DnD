package com.dnd.data;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

public class IdHandlerTest {
    @Test
    public void generatesSanitizedIdFromName() throws IOException {
        IdHandler handler = new IdHandler(tempRegistryFile());
        String id = handler.generateId("Fire Bolt!!", "world/spells.json");
        assertEquals("fire-bolt", id);
    }

    @Test
    public void generatesUniqueIdOnCollision() throws IOException {
        IdHandler handler = new IdHandler(tempRegistryFile());
        String first = handler.generateId("Goblin", "world/monsters.json");
        String second = handler.generateId("Goblin", "world/monsters.json");
        String third = handler.generateId("Goblin", "world/monsters.json");

        assertEquals("goblin", first);
        assertEquals("goblin-2", second);
        assertEquals("goblin-3", third);
    }

    @Test
    public void removeIdAllowsReuse() throws IOException {
        IdHandler handler = new IdHandler(tempRegistryFile());
        String id = handler.generateId("Goblin", "world/monsters.json");

        assertTrue(handler.removeId(id));
        assertFalse(handler.find(id).isPresent());

        String reused = handler.generateId("Goblin", "world/monsters.json");
        assertEquals("goblin", reused);
    }

    @Test
    public void removingUnknownIdReturnsFalse() throws IOException {
        IdHandler handler = new IdHandler(tempRegistryFile());
        assertFalse(handler.removeId("does-not-exist"));
    }

    @Test
    public void findReturnsEmptyForUnknownId() throws IOException {
        IdHandler handler = new IdHandler(tempRegistryFile());
        Optional<IdHandler.IdRecord> result = handler.find("missing");
        assertFalse(result.isPresent());
    }

    @Test
    public void listReturnsAllRegisteredIds() throws IOException {
        IdHandler handler = new IdHandler(tempRegistryFile());
        handler.generateId("Alpha", "world/a.json");
        handler.generateId("Beta", "world/b.json");

        List<IdHandler.IdRecord> entries = handler.list();
        assertEquals(2, entries.size());
    }

    @Test
    public void persistsRegistryAcrossInstances() throws IOException {
        Path registryFile = tempRegistryFile();
        IdHandler first = new IdHandler(registryFile);
        first.generateId("Persisted Entry", "world/x.json");

        IdHandler second = new IdHandler(registryFile);
        assertTrue(second.find("persisted-entry").isPresent());
    }

    @Test
    public void loadingCorruptRegistryFailsLoudlyInsteadOfSilentlyResetting() throws IOException {
        Path registryFile = tempRegistryFile();
        Files.write(registryFile, "{ not valid json".getBytes());

        assertThrows(DataAccessException.class, () -> new IdHandler(registryFile));
    }

    private Path tempRegistryFile() throws IOException {
        Path dir = Files.createTempDirectory("dnd-id-handler-test");
        return dir.resolve("id-registry.json");
    }
}



