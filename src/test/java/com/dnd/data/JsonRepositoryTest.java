package com.dnd.data;

import com.dnd.model.character.CharacterClass;
import com.dnd.model.character.PlayerCharacter;
import com.dnd.model.character.stats.CoreStats;
import com.dnd.model.world.Dice;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JsonRepositoryTest {
    @Test
    public void addUpdateDeleteClass() throws IOException {
        CampaignRepositories repositories = new CampaignRepositories(Files.createTempDirectory("dnd-campaign"));
        CharacterClass fighter = new CharacterClass(
            "fighter",
            "Fighter",
            "A master of martial combat.",
            new Dice("d10", "d10", 10),
            Arrays.asList("strength", "constitution"),
            Map.of("strength", 2)
        );

        repositories.classes().add(fighter);
        assertEquals(1, repositories.classes().list().size());
        assertNotNull(repositories.classes().getById("fighter"));

        fighter.setDescription("Frontline warrior.");
        repositories.classes().update(fighter);
        assertEquals("Frontline warrior.", repositories.classes().getById("fighter").getDescription());

        assertTrue(repositories.classes().delete("fighter"));
        assertEquals(0, repositories.classes().list().size());
    }

    @Test
    public void addAndFetchPlayer() throws IOException {
        CampaignRepositories repositories = new CampaignRepositories(Files.createTempDirectory("dnd-campaign"));
        PlayerCharacter player = new PlayerCharacter(
            "player1",
            "Alyx",
            "fighter",
            "human",
            1,
            new CoreStats(15, 12, 10, 8, 10, 8),
            Collections.singletonList(new PlayerCharacter.PlayerItem(
                "longsword",
                new PlayerCharacter.ItemCondition(100),
                true
            )),
            List.of(new PlayerCharacter.PlayerSpell("frost_bolt", 1, true))
        );

        repositories.players().add(player);
        PlayerCharacter loaded = repositories.players().getById("player1");

        assertNotNull(loaded);
        assertEquals("Alyx", loaded.getName());
        assertEquals(1, loaded.getItems().size());
        assertEquals("longsword", loaded.getItems().get(0).getItemId());
        assertNull(repositories.players().getById("missing"));
    }
}
