package com.dnd.cli.pages.player;

import com.dnd.cli.core.CliSession;
import com.dnd.cli.core.CommandSpec;
import com.dnd.cli.core.FakeConsoleIO;
import com.dnd.cli.core.Page;
import com.dnd.cli.storage.CampaignStorage;
import com.dnd.data.CampaignRepositories;
import com.dnd.model.character.CharacterClass;
import com.dnd.model.character.CharacterRace;
import com.dnd.model.character.PlayerCharacter;
import com.dnd.model.character.stats.CoreStats;
import com.dnd.model.combat.Damage;
import com.dnd.model.combat.Effect;
import com.dnd.model.item.books.Book;
import com.dnd.model.item.Item;
import com.dnd.model.magic.Spell;
import com.dnd.model.world.Dice;
import com.dnd.model.world.map.GameMap;
import com.dnd.model.world.map.MapItemToken;
import com.dnd.model.world.map.Position;
import com.dnd.model.world.map.PlayerToken;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * End-to-end exercise of the player-mode flow: choose campaign, choose
 * character, view stats/inventory/abilities, open the map, move, pick up,
 * and drop an item - using {@link FakeConsoleIO} so it runs headlessly.
 */
public class PlayerModeIntegrationTest {

    @Test
    public void playerCanViewSheetAndActOnTheMap() throws Exception {
        Path campaignRoot = Files.createTempDirectory("dnd-player-mode-test");
        CampaignRepositories repos = new CampaignRepositories(campaignRoot);

        // Seed class/race/character.
        repos.classes().add(new CharacterClass("fighter", "Fighter", "Martial combat.", null,
            Collections.singletonList("strength"), Collections.emptyMap()));
        repos.races().add(new CharacterRace("human", "Human", "Adaptable.", Collections.emptyMap(), 30));

        PlayerCharacter pc = new PlayerCharacter("hero1", "Alyx", "fighter", "human", 1,
            new CoreStats(15, 12, 14, 10, 11, 8), null,
            Collections.singletonList(new PlayerCharacter.PlayerSpell("frost_bolt", 1, true)));
        pc.setPassword("hunter2");
        repos.players().add(pc);

        // Seed an item and a spell so inventory/abilities have something to show.
        Item sword = new Book("longsword", "Longsword", "book", "A blade.", 15, 3.0, null, null, "n/a");
        repos.items().add(sword);
        Spell bolt = new Spell("frost_bolt", "Frost Bolt", "Chilling.", 1, null, 5, 30, 0, null,
            null, null, null,
            Collections.singletonList(new Effect("freeze", "Freeze", "Slows the target.", false, false, 0, 0)),
            new Damage(new Dice("d6", "d6", 6), "cold"));
        repos.spells().add(bolt);

        // Seed a map with the player's token already placed by the DM.
        // 20x20 (rather than 10x10) so an out-of-range cast target is reachable within bounds.
        GameMap map = new GameMap("dungeon", "Dungeon", 20, 20);
        PlayerToken token = new PlayerToken(pc);
        map.placeObject(token, 2, 2);
        MapItemToken itemOnGround = new MapItemToken(sword);
        map.placeObject(itemOnGround, 2, 2);
        repos.maps().add(map);

        // ── Wire pages exactly like AppContext does ──
        FakeConsoleIO console = new FakeConsoleIO();
        CliSession session = new CliSession(new CampaignStorage(campaignRoot.getParent()), console);

        PlayerHomePage homePage = new PlayerHomePage(session, null, null);
        PlayerCharacterSelectionPage characterPage = new PlayerCharacterSelectionPage(session, homePage, null);
        PlayerCampaignSelectionPage campaignPage = new PlayerCampaignSelectionPage(session.getStorage(), characterPage, null);

        // Step 1: choose the campaign (simulate what CampaignSelectionPage's dynamic command does).
        session.setCampaignContext(new com.dnd.cli.core.CampaignContext("test", campaignRoot));

        // Step 2: choose the character via the dynamically generated command.
        // First try the wrong password - should be rejected and not advance the session.
        List<CommandSpec> characterCommands = characterPage.getCommands();
        assertEquals(1, characterCommands.size());
        assertEquals("Alyx", characterCommands.get(0).getKey());
        console.queueInput("wrong-password");
        Page afterWrongPassword = characterCommands.get(0).getAction().execute(session);
        assertEquals(characterPage, afterWrongPassword);
        assertTrue(console.getAllOutput().contains("Incorrect password."));
        assertNull(session.getActivePlayerCharacterId());

        // Now with the correct password.
        console.getOutput().clear();
        console.queueInput("hunter2");
        Page afterSelect = characterCommands.get(0).getAction().execute(session);
        assertEquals(homePage, afterSelect);
        assertEquals("hero1", session.getActivePlayerCharacterId());

        // Step 3: stats.
        List<CommandSpec> homeCommands = homePage.getCommands();
        run(homeCommands, "stats", session);
        assertTrue(console.getAllOutput().contains("Name: Alyx"));
        assertTrue(console.getAllOutput().contains("Speed: 30 ft. (6 squares/turn)"));

        // Step 4: inventory is empty on the character sheet (items live on the map token instead).
        run(homeCommands, "inventory", session);
        assertTrue(console.getAllOutput().contains("Your inventory is empty."));

        // Step 5: abilities list the active Frost Bolt spell attached to pc.
        console.queueInput(""); // skip the "inspect" prompt
        run(homeCommands, "abilities", session);
        assertTrue(console.getAllOutput().contains("Frost Bolt (Rank 1)"));

        // Step 6: open the map.
        Page mapPageResult = runReturning(homeCommands, "map", session);
        assertTrue(mapPageResult instanceof PlayerMapPage);
        PlayerMapPage mapPage = (PlayerMapPage) mapPageResult;

        String body = mapPage.getBody();
        assertTrue(body.contains("Your position: (2, 2)"));
        assertTrue(body.contains("Speed: 6 squares/turn"));

        // Step 6b: stats are also viewable while in the map session.
        console.getOutput().clear();
        run(mapPage.getCommands(), "stats", session);
        assertTrue(console.getAllOutput().contains("Name: Alyx"));
        assertTrue(console.getAllOutput().contains("Speed: 30 ft. (6 squares/turn)"));

        // Step 7: pick up the sword sitting on the same cell.
        console.getOutput().clear();
        console.queueInput("1"); // pick the first (only) item
        run(mapPage.getCommands(), "pickup", session);
        assertTrue(console.getAllOutput().contains("Picked up"));
        assertEquals(1, findPlayerToken(repos.maps().getById("dungeon"), "hero1").getInventory().size());

        // Step 8: moving too far should be rejected (distance from (2,2) to (9,9) is 7 > speed 6).
        console.getOutput().clear();
        console.queueInput("9");
        console.queueInput("9");
        run(mapPage.getCommands(), "move", session);
        assertTrue(console.getAllOutput().contains("Too far!"));
        assertEquals(new Position(2, 2), findPlayerToken(repos.maps().getById("dungeon"), "hero1").getPosition()); // unchanged

        // Step 9: move within speed range (distance 3 <= speed 6).
        console.getOutput().clear();
        console.queueInput("5"); // x
        console.queueInput("5"); // y
        run(mapPage.getCommands(), "move", session);
        assertEquals(new Position(5, 5), findPlayerToken(repos.maps().getById("dungeon"), "hero1").getPosition());

        // Step 10: drop the sword back down.
        console.getOutput().clear();
        console.queueInput("1");
        run(mapPage.getCommands(), "drop", session);
        assertTrue(console.getAllOutput().contains("Dropped"));
        assertEquals(0, findPlayerToken(repos.maps().getById("dungeon"), "hero1").getInventory().size());

        // Step 11: cast Frost Bolt at a nearby cell within range (30 ft. = 6 squares; current pos is (5,5)).
        console.getOutput().clear();
        console.queueInput("1"); // choose Frost Bolt
        console.queueInput("6"); // target x
        console.queueInput("6"); // target y
        run(mapPage.getCommands(), "cast", session);
        assertTrue(console.getAllOutput().contains("casts Frost Bolt at (6, 6)"));
        assertTrue(console.getAllOutput().contains("cold damage"));
        assertTrue(console.getAllOutput().contains("Effect: Freeze"));

        // Step 12: casting beyond range should be rejected (distance from (5,5) to (19,19) is 14 > range 6).
        console.getOutput().clear();
        console.queueInput("1");
        console.queueInput("19");
        console.queueInput("19");
        run(mapPage.getCommands(), "cast", session);
        assertTrue(console.getAllOutput().contains("Out of range!"));

        // Persisted back to disk.
        GameMap reloaded = repos.maps().getById("dungeon");
        assertNotNull(reloaded);
    }

    /** Finds the {@link PlayerToken} for the given character id anywhere on the map's grid. */
    private PlayerToken findPlayerToken(GameMap gameMap, String characterId) {
        for (List<com.dnd.model.world.map.GridCell> row : gameMap.getGrid()) {
            for (com.dnd.model.world.map.GridCell cell : row) {
                for (Object obj : cell.getOccupants()) {
                    if (obj instanceof PlayerToken) {
                        PlayerToken pt = (PlayerToken) obj;
                        if (pt.getCharacter() != null && characterId.equals(pt.getCharacter().getId())) {
                            return pt;
                        }
                    }
                }
            }
        }
        throw new IllegalStateException("PlayerToken not found for character " + characterId);
    }

    private void run(List<CommandSpec> commands, String key, CliSession session) {
        runReturning(commands, key, session);
    }

    private Page runReturning(List<CommandSpec> commands, String key, CliSession session) {
        for (CommandSpec command : commands) {
            if (command.getKey().equals(key)) {
                return command.getAction().execute(session);
            }
        }
        throw new IllegalStateException("No command with key " + key);
    }
}



