package com.dnd.cli.storage;

import com.dnd.cli.core.CampaignContext;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertThrows;

public class CampaignStorageTest {
    @Test
    public void ensureInitializedCreatesDefaultCampaign() throws IOException {
        CampaignStorage storage = newStorage();
        storage.ensureInitialized();

        assertTrue(Files.exists(storage.getDefaultCampaignRoot()));
        assertTrue(Files.exists(storage.getCustomCampaignRoot()));
    }

    @Test
    public void createCampaignFromDefaultSanitizesNameAndCopiesTemplate() throws IOException {
        CampaignStorage storage = newStorage();
        CampaignContext context = storage.createCampaignFromDefault("My Awesome Campaign!!", false);

        assertEquals("my-awesome-campaign", context.getName());
        assertTrue(Files.exists(context.getPath().resolve("world").resolve("classes.json")));
        assertTrue(Files.exists(context.getPath().resolve("players").resolve("players.json")));
    }

    @Test
    public void createCampaignFromDefaultDeduplicatesNames() throws IOException {
        CampaignStorage storage = newStorage();
        CampaignContext first = storage.createCampaignFromDefault("campaign", false);
        CampaignContext second = storage.createCampaignFromDefault("campaign", false);

        assertEquals("campaign", first.getName());
        assertEquals("campaign-2", second.getName());
    }

    @Test
    public void blankTemplateProducesEmptyCatalogs() throws IOException {
        CampaignStorage storage = newStorage();
        CampaignContext context = storage.createCampaignFromDefault("blank-campaign", true);

        String classesJson = Files.readString(context.getPath().resolve("world").resolve("classes.json"));
        assertTrue(classesJson.contains("\"classes\": []"));
    }

    @Test
    public void listCustomCampaignsReturnsCreatedCampaigns() throws IOException {
        CampaignStorage storage = newStorage();
        storage.createCampaignFromDefault("alpha", false);
        storage.createCampaignFromDefault("beta", false);

        List<String> campaigns = storage.listCustomCampaigns();
        assertEquals(2, campaigns.size());
        assertTrue(campaigns.contains("alpha"));
        assertTrue(campaigns.contains("beta"));
    }

    @Test
    public void listCustomCampaignsReturnsEmptyWhenNoneExist() {
        CampaignStorage storage = newStorage();
        assertTrue(storage.listCustomCampaigns().isEmpty());
    }

    @Test
    public void renameCampaignMovesDirectoryAndDeduplicates() throws IOException {
        CampaignStorage storage = newStorage();
        storage.createCampaignFromDefault("old-name", false);
        storage.createCampaignFromDefault("taken-name", false);

        String renamed = storage.renameCampaign("old-name", "taken-name");

        assertEquals("taken-name-2", renamed);
        assertFalse(Files.exists(storage.resolveCustomCampaignPath("old-name")));
        assertTrue(Files.exists(storage.resolveCustomCampaignPath("taken-name-2")));
    }

    @Test
    public void renameCampaignRejectsUnknownCampaign() {
        CampaignStorage storage = newStorage();
        assertThrows(IllegalArgumentException.class, () -> storage.renameCampaign("missing", "new-name"));
    }

    @Test
    public void deleteCampaignRemovesDirectoryAndReturnsTrue() throws IOException {
        CampaignStorage storage = newStorage();
        storage.createCampaignFromDefault("to-delete", false);

        assertTrue(storage.deleteCampaign("to-delete"));
        assertFalse(Files.exists(storage.resolveCustomCampaignPath("to-delete")));
    }

    @Test
    public void deleteCampaignReturnsFalseForUnknownCampaign() throws IOException {
        CampaignStorage storage = newStorage();
        assertFalse(storage.deleteCampaign("does-not-exist"));
    }

    private CampaignStorage newStorage() {
        try {
            return new CampaignStorage(Files.createTempDirectory("dnd-campaign-storage-test"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

