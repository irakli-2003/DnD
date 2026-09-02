package com.dnd.ui.components;

import com.dnd.cli.storage.CampaignStorage;
import com.dnd.data.CampaignRepositories;
import com.dnd.model.character.PlayerCharacter;
import com.dnd.model.character.stats.CoreStats;
import com.dnd.model.combat.Ability;
import com.dnd.model.creature.ChallengeRating;
import com.dnd.model.creature.Monster;
import javafx.application.Platform;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * The form is the only thing that makes stat blocks visible in the app, so these tests pin the
 * behaviour that used to be missing: rich properties must round-trip, and write-only setters
 * must stay out (a "password" box used to appear and blank the character's real password).
 */
public class EntityFormTest {

    @BeforeClass
    public static void startToolkit() throws Exception {
        // JavaFX controls need a running toolkit; start it once for the whole class.
        CountDownLatch ready = new CountDownLatch(1);
        try {
            Platform.startup(ready::countDown);
        } catch (IllegalStateException alreadyRunning) {
            ready.countDown();
        }
        assertTrue("JavaFX toolkit did not start", ready.await(20, TimeUnit.SECONDS));
    }

    /** Runs {@code work} on the FX thread and rethrows whatever it threw. */
    private static void onFxThread(ThrowingRunnable work) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                work.run();
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                done.countDown();
            }
        });
        assertTrue("FX task timed out", done.await(20, TimeUnit.SECONDS));
        if (failure.get() != null) {
            throw failure.get() instanceof Exception e ? e : new RuntimeException(failure.get());
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    @Test
    public void monsterStatsSurviveARoundTrip() throws Exception {
        Monster monster = new Monster();
        monster.setId("owlbear");
        monster.setName("Owlbear");
        monster.setType("monstrosity");
        monster.setChallengeRating(ChallengeRating.CR_3);
        CoreStats stats = new CoreStats();
        stats.setStrength(20);
        stats.setDexterity(12);
        stats.setConstitution(17);
        stats.setIntelligence(3);
        stats.setWisdom(12);
        stats.setCharisma(7);
        monster.setStats(stats);
        monster.setLanguages(List.of("common"));
        Ability ability = new Ability();
        ability.setId("multiattack");
        ability.setName("Multiattack");
        monster.setAbilities(List.of(ability));

        onFxThread(() -> {
            EntityForm form = EntityForm.of(Monster.class, monster, Set.of("imagePath"),
                Map.of("languages", List.of(new EntityForm.Ref("common", "Common"))));

            Monster saved = new Monster();
            form.writeTo(saved);

            assertEquals("owlbear", saved.getId());
            assertEquals("monstrosity", saved.getType());
            assertEquals(ChallengeRating.CR_3, saved.getChallengeRating());
            assertNotNull("ability scores must be editable, not dropped", saved.getStats());
            assertEquals(20, saved.getStats().getStrength());
            assertEquals(3, saved.getStats().getIntelligence());
            assertEquals(List.of("common"), saved.getLanguages());
            assertEquals(1, saved.getAbilities().size());
            assertEquals("Multiattack", saved.getAbilities().get(0).getName());
        });
    }

    @Test
    public void newEntityFormStillProducesUsableDefaults() throws Exception {
        onFxThread(() -> {
            EntityForm form = EntityForm.of(Monster.class, null, Set.of("imagePath"), Map.of());
            Monster created = new Monster();
            form.writeTo(created);
            assertNotNull(created.getStats());
            assertEquals("unedited scores default to 10", 10, created.getStats().getStrength());
        });
    }

    /**
     * Regression test for the bug where a new character saved with a blank Level field wrote
     * an in-range-violating 0 straight to disk (bypassing setLevel's validation, since a blank
     * field used to be coerced to the literal int 0 instead of being left alone), which then
     * made the whole players.json unreadable the next time anything tried to load it.
     */
    @Test
    public void newPlayerFormLeavesBlankLevelAtItsValidDefault() throws Exception {
        onFxThread(() -> {
            EntityForm form = EntityForm.of(PlayerCharacter.class, null, Set.of("imagePath", "passwordHash", "passwordSalt"), Map.of());
            PlayerCharacter created = new PlayerCharacter();
            form.writeTo(created);
            assertEquals("a blank Level field must not coerce the model to an invalid 0",
                PlayerCharacter.MIN_LEVEL, created.getLevel());
        });
    }

    @Test
    public void writeOnlyPasswordSetterIsNotEditable() throws Exception {
        PlayerCharacter player = new PlayerCharacter();
        player.setId("thorin");
        player.setName("Thorin");
        player.setPassword("secret");

        onFxThread(() -> {
            EntityForm form = EntityForm.of(PlayerCharacter.class, player,
                Set.of("imagePath", "passwordHash", "passwordSalt"), Map.of());
            form.writeTo(player);
            assertTrue("saving a character must not overwrite its password", player.checkPassword("secret"));
        });
    }

    /**
     * Opening and saving any catalogue entry must not corrupt or reject it. This walks the whole
     * shipped standard-rules dataset, which is the closest thing to a DM clicking through every
     * world-building screen.
     */
    @Test
    public void everyShippedEntityOpensAndSavesUnchanged() throws Exception {
        CampaignStorage storage = new CampaignStorage(Files.createTempDirectory("dnd-entity-form-test"));
        CampaignRepositories repos = new CampaignRepositories(
            storage.createCampaignFromDefault("form-smoke", false).getPath());

        List<Object> entities = new ArrayList<>();
        for (List<?> catalog : List.of(repos.races().list(), repos.classes().list(), repos.items().list(),
            repos.spells().list(), repos.monsters().list(), repos.beasts().list(), repos.npcs().list(),
            repos.places().list(), repos.effects().list(), repos.damageTypes().list(),
            repos.languages().list(), repos.books().list(), repos.alchemyIngredients().list(),
            repos.dice().list(), repos.players().list())) {
            entities.addAll(catalog);
        }
        assertTrue("dataset should not be empty", entities.size() > 300);

        Map<String, List<EntityForm.Ref>> catalogs = Map.of(
            "languages", refs(repos.languages().list()),
            "effects", refs(repos.effects().list()),
            "classId", refs(repos.classes().list()),
            "raceId", refs(repos.races().list()));

        onFxThread(() -> {
            for (Object entity : entities) {
                EntityForm form = EntityForm.of(entity.getClass(), entity,
                    Set.of("imagePath", "passwordHash", "passwordSalt"), catalogs);
                try {
                    form.writeTo(entity);
                } catch (RuntimeException e) {
                    throw new AssertionError("saving " + entity.getClass().getSimpleName()
                        + " failed: " + e.getMessage(), e);
                }
            }
        });

        Monster reloaded = repos.monsters().list().get(0);
        assertNotNull("monsters must still carry their stat block", reloaded.getStats());
    }

    private static List<EntityForm.Ref> refs(List<?> entities) throws Exception {
        List<EntityForm.Ref> refs = new ArrayList<>();
        for (Object entity : entities) {
            refs.add(new EntityForm.Ref(
                String.valueOf(entity.getClass().getMethod("getId").invoke(entity)),
                String.valueOf(entity.getClass().getMethod("getName").invoke(entity))));
        }
        return refs;
    }
}



