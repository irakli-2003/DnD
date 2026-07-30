package com.dnd.cli.pages.entity;

import com.dnd.cli.core.CliSession;
import com.dnd.cli.core.FakeConsoleIO;
import com.dnd.cli.pages.EntityType;
import com.dnd.cli.storage.CampaignStorage;
import com.dnd.data.CampaignRepositories;
import com.dnd.data.CampaignPaths;
import com.dnd.data.IdHandler;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Demonstrates that entity CRUD flows are now testable end-to-end via
 * {@link FakeConsoleIO}, without capturing real stdout or blocking on real
 * stdin - the concrete payoff of routing all I/O through {@link com.dnd.cli.core.ConsoleIO}.
 */
public class EntityCrudServiceTest {
    @Test
    public void createPersistsNewDamageTypeAndPrintsConfirmation() throws Exception {
        Path campaignRoot = Files.createTempDirectory("dnd-entity-crud-test");
        CampaignRepositories repositories = new CampaignRepositories(campaignRoot);
        IdHandler idHandler = new IdHandler(new CampaignPaths(campaignRoot).idRegistryFile());

        // DamageType only has id/name/description - a simple entity to script end-to-end.
        FakeConsoleIO console = new FakeConsoleIO(
            "Radiant",        // name prompt
            "Holy light damage" // description prompt (only writable, non-name property)
        );
        CliSession session = new CliSession(new CampaignStorage(campaignRoot), console);

        new EntityCrudService().create(session, repositories, idHandler, EntityType.DAMAGE_TYPE);

        assertEquals(1, repositories.damageTypes().list().size());
        assertEquals("Radiant", repositories.damageTypes().list().get(0).getName());
        assertTrue(console.getAllOutput().contains("Created Damage Type: Radiant"));
    }
}

