package com.dnd.ui.scenes;

import com.dnd.data.CampaignRepositories;
import com.dnd.data.SessionRosterStore;
import com.dnd.model.character.PlayerCharacter;
import com.dnd.model.creature.Monster;
import com.dnd.model.session.TrackedCreature;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static org.junit.Assert.*;

public class ManagePlayersPanelTest {

    @BeforeClass
    public static void startToolkit() throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        try {
            Platform.startup(ready::countDown);
        } catch (IllegalStateException alreadyRunning) {
            ready.countDown();
        }
        assertTrue(ready.await(20, TimeUnit.SECONDS));
    }

    private interface Work { void run() throws Exception; }

    private static void onFx(Work work) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try { work.run(); } catch (Throwable t) { failure.set(t); } finally { done.countDown(); }
        });
        assertTrue(done.await(20, TimeUnit.SECONDS));
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    @Test
    public void listsPlayersAndSessionCreaturesWithQuickHpAndDetailNavigation() throws Exception {
        Path root = Files.createTempDirectory("dnd-manage-players");
        CampaignRepositories repos = new CampaignRepositories(root);
        PlayerCharacter aria = new PlayerCharacter();
        aria.setId("aria");
        aria.setName("Aria");
        aria.setMaxHitPoints(20);
        aria.setCurrentHitPoints(20);
        repos.players().save(aria);
        Monster goblin = new Monster();
        goblin.setId("gob");
        goblin.setName("Goblin");
        goblin.setMaxHitPoints(7);
        goblin.setArmorClass(13);
        repos.monsters().save(goblin);
        Path session = root.resolve("Session 1.md");
        Files.writeString(session, "text");

        onFx(() -> {
            ManagePlayersPanel panel = new ManagePlayersPanel(new CharacterCreationWizard(null, repos), repos, session, s -> { });
            panel.showList();
            assertNotNull("player card is listed", find(panel.node(), n -> n instanceof Label l && "Aria".equals(l.getText())));

            for (int i = 0; i < 2; i++) {
                panel.addCreature(new TrackedCreature(TrackedCreature.Kind.MONSTER, "gob", "Goblin", 7, 0, 13, null));
            }
            assertEquals(List.of("Goblin", "Goblin 2"), panel.creatures().stream().map(TrackedCreature::getName).toList());

            // Default amount is 5: the second goblin goes from 7 to 2, the first is untouched.
            button(card(panel, "Goblin 2"), "−").fire();
            List<TrackedCreature> saved = SessionRosterStore.load(session);
            assertEquals(7, saved.stream().filter(c -> c.getName().equals("Goblin")).findFirst().orElseThrow().getCurrentHitPoints());
            assertEquals(2, saved.stream().filter(c -> c.getName().equals("Goblin 2")).findFirst().orElseThrow().getCurrentHitPoints());

            button(card(panel, "Aria"), "+").fire();
            assertEquals("healing is capped at max HP", 20, repos.players().getById("aria").getCurrentHitPoints());
            button(card(panel, "Aria"), "−").fire();
            assertEquals(15, repos.players().getById("aria").getCurrentHitPoints());

            panel.showCreature(panel.creatures().get(1));
            Button back = (Button) find(panel.node(), n -> n instanceof Button b && b.getText().startsWith("← Back"));
            assertNotNull("detail view has a back button", back);
            back.fire();
            assertNotNull("back returns to the card list",
                find(panel.node(), n -> n instanceof Label l && "Party & Creatures".equals(l.getText())));
        });
    }

    private static Node card(ManagePlayersPanel panel, String name) {
        Node card = find(panel.node(), n -> n instanceof HBox h && h.getStyleClass().contains("creature-card")
            && find(h, m -> m instanceof Label l && name.equals(l.getText())) != null);
        assertNotNull("card for " + name, card);
        return card;
    }

    private static Button button(Node within, String text) {
        Node b = find(within, n -> n instanceof Button x && text.equals(x.getText()));
        assertNotNull("button " + text, b);
        return (Button) b;
    }

    private static Node find(Node node, Predicate<Node> test) {
        if (test.test(node)) return node;
        List<Node> children = new ArrayList<>();
        if (node instanceof ScrollPane sp && sp.getContent() != null) children.add(sp.getContent());
        if (node instanceof Parent p) children.addAll(p.getChildrenUnmodifiable());
        for (Node child : children) {
            Node found = find(child, test);
            if (found != null) return found;
        }
        return null;
    }
}
