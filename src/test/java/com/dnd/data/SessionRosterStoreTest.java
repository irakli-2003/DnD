package com.dnd.data;

import com.dnd.model.session.TrackedCreature;
import com.dnd.model.world.map.ActiveEffect;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

public class SessionRosterStoreTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void creaturesRoundTripPerSessionFile() throws Exception {
        StorylineService service = new StorylineService(tmp.getRoot().toPath());
        service.ensureRoot();
        Path one = service.createFile(service.getRoot(), "Session 1.md");
        Path two = service.createFile(service.getRoot(), "Session 2.md");

        TrackedCreature goblin = new TrackedCreature(TrackedCreature.Kind.MONSTER, "gob", "Goblin", 7, 0, 13, null);
        goblin.setCurrentHitPoints(3);
        goblin.getActiveEffects().add(new ActiveEffect("burn", "Burning", 2, 1, 0, "DM"));
        SessionRosterStore.save(one, List.of(goblin));

        List<TrackedCreature> loaded = SessionRosterStore.load(one);
        assertEquals(1, loaded.size());
        assertEquals("Goblin", loaded.get(0).getName());
        assertEquals(3, loaded.get(0).getCurrentHitPoints());
        assertEquals(7, loaded.get(0).getMaxHitPoints());
        assertEquals("Burning", loaded.get(0).getActiveEffects().get(0).getName());
        assertTrue("other session files have their own (empty) list", SessionRosterStore.load(two).isEmpty());
    }

    @Test
    public void sidecarIsHiddenAndFollowsMovesAndDeletes() throws Exception {
        StorylineService service = new StorylineService(tmp.getRoot().toPath());
        service.ensureRoot();
        Path file = service.createFile(service.getRoot(), "Session 1.md");
        SessionRosterStore.save(file, List.of(new TrackedCreature(TrackedCreature.Kind.NPC, "n", "Bob", 5, 0, 10, null)));

        assertEquals("roster sidecar must not show up in the storyline tree",
            List.of(file), service.listChildren(service.getRoot()));

        Path folder = service.createFolder(service.getRoot(), "Act 1");
        Path moved = service.move(file, folder);
        assertEquals("Bob", SessionRosterStore.load(moved).get(0).getName());
        assertFalse(Files.exists(SessionRosterStore.rosterFile(file)));

        service.delete(moved);
        assertFalse(Files.exists(SessionRosterStore.rosterFile(moved)));
    }

    @Test
    public void savingAnEmptyListRemovesTheSidecar() throws Exception {
        StorylineService service = new StorylineService(tmp.getRoot().toPath());
        service.ensureRoot();
        Path file = service.createFile(service.getRoot(), "S.md");
        SessionRosterStore.save(file, List.of(new TrackedCreature(TrackedCreature.Kind.BEAST, "w", "Wolf", 11, 0, 13, null)));
        assertTrue(Files.exists(SessionRosterStore.rosterFile(file)));
        SessionRosterStore.save(file, List.of());
        assertFalse(Files.exists(SessionRosterStore.rosterFile(file)));
    }
}
