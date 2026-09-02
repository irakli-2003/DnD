package com.dnd.cli.storage;

import com.dnd.cli.core.CampaignContext;
import com.dnd.data.CampaignRepositories;
import com.dnd.data.IdHandler;
import com.dnd.model.character.PlayerCharacter;
import com.dnd.model.combat.DiceRoll;
import com.dnd.model.creature.Monster;
import com.dnd.model.creature.Npc;
import com.dnd.model.item.Weapon;
import com.dnd.model.item.alchemy.AlchemyItem;
import com.dnd.model.item.armors.Armor;
import com.dnd.model.magic.Spell;
import com.dnd.model.world.Language;
import com.dnd.model.world.Place;
import com.dnd.model.world.map.GameMap;
import com.dnd.model.world.map.GridCell;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Loads a campaign seeded from the bundled standard-rules dataset through the
 * real repositories.
 *
 * <p>The dataset is hand-generated JSON, so nothing else in the build proves
 * that its enum spellings, polymorphic {@code type} discriminators and id
 * cross-references actually survive deserialization. That is what these tests
 * are for - a typo like {@code "healing"} instead of {@code "restoration"} would
 * only surface as a crash when a DM opened the catalogue otherwise.</p>
 */
public class StandardRulesDatasetTest {
    private static CampaignRepositories seedCampaign() throws IOException {
        CampaignStorage storage = new CampaignStorage(Files.createTempDirectory("dnd-srd-dataset-test"));
        CampaignContext context = storage.createCampaignFromDefault("standard-rules", false);
        return new CampaignRepositories(context.getPath());
    }

    private static Path seedCampaignPath() throws IOException {
        CampaignStorage storage = new CampaignStorage(Files.createTempDirectory("dnd-srd-dataset-test"));
        return storage.createCampaignFromDefault("standard-rules", false).getPath();
    }

    @Test
    public void everyCatalogDeserializesAndIsPopulated() throws IOException {
        CampaignRepositories repos = seedCampaign();

        assertTrue("races", repos.races().list().size() >= 20);
        assertTrue("classes", repos.classes().list().size() >= 12);
        assertTrue("items", repos.items().list().size() >= 90);
        assertTrue("spells", repos.spells().list().size() >= 60);
        assertTrue("monsters", repos.monsters().list().size() >= 50);
        assertTrue("beasts", repos.beasts().list().size() >= 20);        assertTrue("npcs", repos.npcs().list().size() >= 20);
        assertTrue("places", repos.places().list().size() >= 15);
        assertTrue("effects", repos.effects().list().size() >= 30);
        assertTrue("damage types", repos.damageTypes().list().size() >= 13);
        assertTrue("languages", repos.languages().list().size() >= 15);
        assertTrue("books", repos.books().list().size() >= 20);
        assertTrue("alchemy ingredients", repos.alchemyIngredients().list().size() >= 20);
        assertTrue("dice", repos.dice().list().size() >= 7);
        assertTrue("maps", repos.maps().list().size() >= 10);
        assertTrue("players", repos.players().list().size() >= 4);
    }

    @Test
    public void weaponsArmorAndPotionsKeepTheirConcreteTypes() throws IOException {
        CampaignRepositories repos = seedCampaign();

        assertTrue("longsword should be a weapon", repos.items().getById("longsword") instanceof Weapon);
        assertTrue("plate armor should be armor", repos.items().getById("plate-armor") instanceof Armor);
        assertTrue("healing potion should be alchemical",
            repos.items().getById("potion-of-healing") instanceof AlchemyItem);

        Weapon longsword = (Weapon) repos.items().getById("longsword");
        assertNotNull("weapon damage", longsword.getWeaponDamage());
        assertNotNull("weapon damage type", repos.damageTypes()
            .getById(longsword.getWeaponDamage().getTypeId()));
    }

    @Test
    public void spellDamageMigratesFromLegacyAmountShapeToDiceList() throws IOException {
        CampaignRepositories repos = seedCampaign();

        Spell fireball = repos.spells().getById("fireball");
        assertNotNull("fireball", fireball);
        assertNotNull("fireball damage", fireball.getDamage());
        assertTrue("fireball damage should carry at least one die", fireball.getDamage().hasDice());
        DiceRoll roll = fireball.getDamage().getDice().get(0);
        assertNotNull("rolled die should reference a real catalogue die", repos.dice().getById(roll.getDiceId()));
        assertEquals(1, roll.getCount());
    }

    @Test
    public void everyCatalogListIsSortedAlphabetically() throws IOException {
        CampaignRepositories repos = seedCampaign();

        List<String> spellNames = repos.spells().list().stream().map(Object::toString).collect(Collectors.toList());
        List<String> sorted = spellNames.stream().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
        assertEquals("spells should be alphabetically sorted", sorted, spellNames);

        List<String> itemNames = repos.items().list().stream().map(Object::toString).collect(Collectors.toList());
        List<String> sortedItems = itemNames.stream().sorted(String.CASE_INSENSITIVE_ORDER).collect(Collectors.toList());
        assertEquals("items should be alphabetically sorted", sortedItems, itemNames);
    }

    @Test
    public void engineReferencedEffectsExist() throws IOException {
        CampaignRepositories repos = seedCampaign();
        // Damage.resolveAgainst() looks these up by id, so they have to be present.
        for (String effectId : List.of("heated_armor", "freeze", "shock", "burn", "fear", "infection")) {
            assertNotNull(effectId, repos.effects().getById(effectId));
        }
    }

    @Test
    public void everyPlayerReferencesRealClassesRacesItemsAndSpells() throws IOException {
        CampaignRepositories repos = seedCampaign();

        for (PlayerCharacter player : repos.players().list()) {
            assertNotNull(player.getName(), player.getPlayerName());
            assertNotNull("class of " + player.getName(), repos.classes().getById(player.getClassId()));
            assertNotNull("race of " + player.getName(), repos.races().getById(player.getRaceId()));
            for (PlayerCharacter.PlayerItem owned : player.getItems()) {
                assertNotNull("item " + owned.getItemId(), repos.items().getById(owned.getItemId()));
            }
            for (PlayerCharacter.PlayerSpell known : player.getSpells()) {
                assertNotNull("spell " + known.getSpellId(), repos.spells().getById(known.getSpellId()));
            }
        }
    }

    @Test
    public void everyCreatureLanguageResolves() throws IOException {
        CampaignRepositories repos = seedCampaign();

        for (Npc npc : repos.npcs().list()) {
            for (String languageId : npc.getLanguages()) {
                assertNotNull("language " + languageId + " of " + npc.getId(),
                    repos.languages().getById(languageId));
            }
        }
        for (Monster monster : repos.monsters().list()) {
            for (String languageId : monster.getLanguages()) {
                assertNotNull("language " + languageId + " of " + monster.getId(),
                    repos.languages().getById(languageId));
            }
        }
    }

    @Test
    public void everyPlaceMapIdResolves() throws IOException {
        CampaignRepositories repos = seedCampaign();

        for (Place place : repos.places().list()) {
            if (place.getMapId() != null) {
                assertNotNull("map " + place.getMapId() + " of " + place.getId(),
                    repos.maps().getById(place.getMapId()));
            }
        }
    }

    @Test
    public void everyLanguagePrimerAlsoExistsInTheBookCatalog() throws IOException {
        CampaignRepositories repos = seedCampaign();

        for (Language language : repos.languages().list()) {
            if (language.getRequiredMaterial() == null) {
                continue;
            }
            String bookId = language.getRequiredMaterial().getId();
            assertNotNull("primer " + bookId, repos.books().getById(bookId));
        }
    }

    @Test
    public void mapGridsMatchTheirDeclaredDimensions() throws IOException {
        CampaignRepositories repos = seedCampaign();

        for (GameMap map : repos.maps().list()) {
            List<List<GridCell>> grid = map.getGrid();
            assertEquals("row count of " + map.getId(), map.getHeight(), grid.size());
            for (List<GridCell> row : grid) {
                assertEquals("row width of " + map.getId(), map.getWidth(), row.size());
            }
            boolean anyPassable = grid.stream().flatMap(List::stream).anyMatch(GridCell::isPassable);
            assertTrue("map " + map.getId() + " has no passable cell", anyPassable);
        }
    }

    @Test
    public void idsAreUniqueAcrossCatalogsAndAllRegistered() throws IOException {
        Path campaign = seedCampaignPath();
        CampaignRepositories repos = new CampaignRepositories(campaign);

        List<String> allIds = List.of(
                repos.races().list().stream().map(e -> e.getId()).collect(Collectors.toList()),
                repos.classes().list().stream().map(e -> e.getId()).collect(Collectors.toList()),
                repos.items().list().stream().map(e -> e.getId()).collect(Collectors.toList()),
                repos.spells().list().stream().map(e -> e.getId()).collect(Collectors.toList()),
                repos.places().list().stream().map(e -> e.getId()).collect(Collectors.toList()),
                repos.effects().list().stream().map(e -> e.getId()).collect(Collectors.toList()),
                repos.damageTypes().list().stream().map(e -> e.getId()).collect(Collectors.toList()),
                repos.npcs().list().stream().map(e -> e.getId()).collect(Collectors.toList()),
                repos.monsters().list().stream().map(e -> e.getId()).collect(Collectors.toList()),
                repos.beasts().list().stream().map(e -> e.getId()).collect(Collectors.toList()),
                repos.languages().list().stream().map(e -> e.getId()).collect(Collectors.toList()),
                repos.alchemyIngredients().list().stream().map(e -> e.getId()).collect(Collectors.toList()),
                repos.books().list().stream().map(e -> e.getId()).collect(Collectors.toList()),
                repos.dice().list().stream().map(e -> e.getId()).collect(Collectors.toList()),
                repos.maps().list().stream().map(e -> e.getId()).collect(Collectors.toList()),
                repos.players().list().stream().map(e -> e.getId()).collect(Collectors.toList()))
            .stream().flatMap(List::stream).collect(Collectors.toList());

        Set<String> unique = new HashSet<>(allIds);
        assertEquals("duplicate ids across catalogs", allIds.size(), unique.size());

        IdHandler ids = new IdHandler(campaign.resolve("world").resolve("id-registry.json"));
        for (String id : allIds) {
            assertTrue("id " + id + " is missing from the registry", ids.find(id).isPresent());
        }
    }

    @Test
    public void starterStorylineIsSeededForDefaultCampaignsOnly() throws IOException {
        Path defaultCampaign = seedCampaignPath();
        Path chapter = defaultCampaign.resolve("storyline").resolve("Chapter 1 - Trouble in Greenhollow");
        assertTrue("starter chapter", Files.isDirectory(chapter));
        String firstSession = Files.readString(chapter.resolve("Session 01 - The Gilded Stag.txt"));
        assertTrue(firstSession.contains("[READ ALOUD]"));
        assertTrue(firstSession.contains("[map:gilded-stag-ground|"));

        CampaignStorage storage = new CampaignStorage(Files.createTempDirectory("dnd-srd-blank-test"));
        Path blank = storage.createCampaignFromDefault("blank", true).getPath();
        assertTrue("blank campaigns still get the folder", Files.isDirectory(blank.resolve("storyline")));
        try (var entries = Files.list(blank.resolve("storyline"))) {
            assertFalse("blank campaigns get no starter content", entries.findAny().isPresent());
        }
    }
}
