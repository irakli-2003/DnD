package com.dnd.ui.scenes;

import com.dnd.data.CampaignRepositories;
import com.dnd.data.SessionRosterStore;
import com.dnd.model.character.CharacterClass;
import com.dnd.model.character.PlayerCharacter;
import com.dnd.model.combat.Effect;
import com.dnd.model.item.Item;
import com.dnd.model.session.TrackedCreature;
import com.dnd.model.world.map.ActiveEffect;
import com.dnd.ui.ImageStore;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * The text editor's right-hand "Manage Players" panel: a vertical list of cards - every
 * player character plus the monsters, NPCs and beasts the DM added to this session file -
 * showing HP and mana at a glance with quick damage / heal buttons. Clicking a card opens
 * that creature's details in the same panel; "Back" returns to the list.
 */
final class ManagePlayersPanel {

    static final double WIDTH = 380;

    private final BaseScene owner;
    private final CampaignRepositories repos;
    private final Path sessionFile;
    private final Consumer<String> status;

    private final VBox root = new VBox();
    private final List<TrackedCreature> creatures;
    private Spinner<Integer> amount;
    private double listScroll;

    ManagePlayersPanel(BaseScene owner, CampaignRepositories repos, Path sessionFile, Consumer<String> status) {
        this.owner = owner;
        this.repos = repos;
        this.sessionFile = sessionFile;
        this.status = status;
        this.creatures = SessionRosterStore.load(sessionFile);
        root.getStyleClass().add("side-panel");
        root.setPrefWidth(WIDTH);
        root.setMinWidth(WIDTH);
        root.setMaxWidth(WIDTH);
    }

    Node node() {
        return root;
    }

    List<TrackedCreature> creatures() {
        return creatures;
    }

    private void saveCreatures() {
        try {
            SessionRosterStore.save(sessionFile, creatures);
        } catch (RuntimeException ex) {
            status.accept("Couldn't save the session's creatures: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------------ list

    void showList() {
        Label heading = owner.sectionLabel("Party & Creatures");
        MenuButton add = buildAddMenu();
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(8, heading, spacer, add);
        header.setAlignment(Pos.CENTER_LEFT);

        int previous = amount == null ? 5 : amount.getValue();
        amount = new Spinner<>(1, 999, previous);
        amount.setEditable(true);
        amount.setPrefWidth(80);
        Label amountLabel = owner.body("Amount for − / + :");
        HBox amountRow = new HBox(8, amountLabel, amount);
        amountRow.setAlignment(Pos.CENTER_LEFT);

        VBox cards = new VBox(8);
        cards.getChildren().add(subLabel("Players"));
        List<PlayerCharacter> players = repos.players().list();
        if (players.isEmpty()) cards.getChildren().add(hint("No player characters yet - use + Add."));
        for (PlayerCharacter pc : players) cards.getChildren().add(playerCard(pc));

        cards.getChildren().add(subLabel("Creatures in this session"));
        if (creatures.isEmpty()) cards.getChildren().add(hint("Add monsters, NPCs or beasts with + Add."));
        List<TrackedCreature> sorted = new ArrayList<>(creatures);
        sorted.sort((a, b) -> String.CASE_INSENSITIVE_ORDER.compare(nameOf(a), nameOf(b)));
        for (TrackedCreature c : sorted) cards.getChildren().add(creatureCard(c));

        ScrollPane scroll = scroll(cards);
        double restore = listScroll;
        scroll.vvalueProperty().addListener((o, old, v) -> listScroll = v.doubleValue());
        javafx.application.Platform.runLater(() -> scroll.setVvalue(restore));

        VBox top = new VBox(8, header, amountRow);
        top.setPadding(new Insets(10, 10, 6, 10));
        root.getChildren().setAll(top, scroll);
    }

    private MenuButton buildAddMenu() {
        MenuButton add = new MenuButton("+ Add");
        add.getStyleClass().add("dnd-button");
        add.setStyle("-fx-min-width: 0; -fx-padding: 5 10 5 10;");

        MenuItem newPlayer = new MenuItem("New player character...");
        newPlayer.setOnAction(e -> new CharacterCreationWizard(owner.uiSession, repos).show(this::showList));

        Menu monsters = new Menu("Monster");
        for (var m : repos.monsters().list()) {
            monsters.getItems().add(addItem(m.getName(), () -> addCreature(new TrackedCreature(
                TrackedCreature.Kind.MONSTER, m.getId(), m.getName(), m.getMaxHitPoints(), m.getMaxMana(),
                m.getArmorClass(), m.getImagePath()))));
        }
        Menu npcs = new Menu("NPC");
        for (var n : repos.npcs().list()) {
            npcs.getItems().add(addItem(n.getName(), () -> addCreature(new TrackedCreature(
                TrackedCreature.Kind.NPC, n.getId(), n.getName(), n.getMaxHitPoints(), n.getMaxMana(),
                n.getArmorClass(), n.getImagePath()))));
        }
        Menu beasts = new Menu("Beast");
        for (var b : repos.beasts().list()) {
            beasts.getItems().add(addItem(b.getName(), () -> addCreature(new TrackedCreature(
                TrackedCreature.Kind.BEAST, b.getId(), b.getName(), b.getMaxHitPoints(), b.getMaxMana(),
                b.getArmorClass(), b.getImagePath()))));
        }
        for (Menu m : List.of(monsters, npcs, beasts)) {
            if (m.getItems().isEmpty()) {
                MenuItem none = new MenuItem("(none in the catalog yet)");
                none.setDisable(true);
                m.getItems().add(none);
            }
        }
        add.getItems().addAll(newPlayer, new SeparatorMenuItem(), monsters, npcs, beasts);
        return add;
    }

    private MenuItem addItem(String label, Runnable action) {
        MenuItem item = new MenuItem(label);
        item.setOnAction(e -> action.run());
        return item;
    }

    /** Adds a creature, numbering repeats ("Goblin", "Goblin 2", ...) so cards stay distinguishable. */
    void addCreature(TrackedCreature creature) {
        String base = creature.getName() == null ? "Creature" : creature.getName();
        long same = creatures.stream().filter(c -> base.equals(c.getName())
            || (c.getName() != null && c.getName().matches(java.util.regex.Pattern.quote(base) + " \\d+"))).count();
        if (same > 0) creature.setName(base + " " + (same + 1));
        creatures.add(creature);
        saveCreatures();
        status.accept(creature.getName() + " added to this session.");
        showList();
    }

    private Node playerCard(PlayerCharacter pc) {
        CharacterClass cls = pc.getClassId() == null ? null : repos.classes().getById(pc.getClassId());
        String subtitle = "Lvl " + pc.getLevel() + (cls != null ? " " + cls.getName() : "")
            + (pc.getPlayerName() != null && !pc.getPlayerName().isBlank() ? "  ·  " + pc.getPlayerName() : "");
        return card(pc.getName(), subtitle, pc.getImagePath(),
            pc.getCurrentHitPoints(), pc.getMaxHitPoints(), pc.getCurrentMana(), pc.getMaxMana(),
            pc.getActiveEffects(),
            delta -> {
                pc.setCurrentHitPoints(Math.max(0, Math.min(pc.getMaxHitPoints(), pc.getCurrentHitPoints() + delta)));
                repos.players().save(pc);
                showList();
            },
            () -> showPlayer(pc));
    }

    private Node creatureCard(TrackedCreature c) {
        String kind = c.getKind() == null ? "Creature" : switch (c.getKind()) {
            case MONSTER -> "Monster";
            case NPC -> "NPC";
            case BEAST -> "Beast";
        };
        return card(nameOf(c), kind + (c.getArmorClass() > 0 ? "  ·  AC " + c.getArmorClass() : ""), c.getImagePath(),
            c.getCurrentHitPoints(), c.getMaxHitPoints(), c.getCurrentMana(), c.getMaxMana(),
            c.getActiveEffects(),
            delta -> {
                c.setCurrentHitPoints(Math.max(0, Math.min(c.getMaxHitPoints(), c.getCurrentHitPoints() + delta)));
                saveCreatures();
                showList();
            },
            () -> showCreature(c));
    }

    private Node card(String name, String subtitle, String imagePath, int hp, int maxHp, int mana, int maxMana,
                      List<ActiveEffect> effects, IntConsumer adjustHp, Runnable open) {
        ImageView img = new ImageView(ImageStore.loadOrPlaceholder(owner.uiSession == null ? null : owner.uiSession.campaignRoot(), imagePath));
        img.setFitWidth(44);
        img.setFitHeight(44);
        img.setPreserveRatio(true);

        Label nameLabel = new Label(name == null ? "?" : name);
        nameLabel.getStyleClass().add("card-name");
        Label sub = new Label(subtitle);
        sub.getStyleClass().add("muted-label");

        VBox info = new VBox(3, nameLabel, sub, bar("HP", hp, maxHp, hp * 4 <= maxHp ? "#c03a3a" : "#4a9a3a"));
        if (maxMana > 0) info.getChildren().add(bar("MP", mana, maxMana, "#3a6ac0"));
        if (effects != null && !effects.isEmpty()) {
            StringBuilder sb = new StringBuilder("✦ ");
            for (int i = 0; i < effects.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(effects.get(i).getName()).append(" (").append(effects.get(i).getRemainingRounds()).append(")");
            }
            Label fx = new Label(sb.toString());
            fx.getStyleClass().add("card-effects");
            fx.setWrapText(true);
            info.getChildren().add(fx);
        }
        HBox.setHgrow(info, Priority.ALWAYS);

        Button hurt = quick("−", "Damage by the amount above", () -> adjustHp.accept(-amount.getValue()));
        Button heal = quick("+", "Heal by the amount above", () -> adjustHp.accept(amount.getValue()));
        VBox quick = new VBox(4, hurt, heal);
        quick.setAlignment(Pos.CENTER);

        HBox card = new HBox(10, img, info, quick);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("creature-card");
        if (maxHp > 0 && hp <= 0) card.getStyleClass().add("creature-card-down");
        card.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getTarget() instanceof Node n && isInside(n, quick)) return;
            open.run();
        });
        return card;
    }

    private static boolean isInside(Node node, Node container) {
        for (Node n = node; n != null; n = n.getParent()) if (n == container) return true;
        return false;
    }

    private Node bar(String label, int value, int max, String color) {
        ProgressBar bar = new ProgressBar(max <= 0 ? 0 : Math.max(0, Math.min(1, value / (double) max)));
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.setPrefHeight(12);
        bar.setStyle("-fx-accent: " + color + ";");
        HBox.setHgrow(bar, Priority.ALWAYS);
        Label text = new Label(label + " " + value + "/" + max);
        text.getStyleClass().add("bar-label");
        text.setMinWidth(78);
        HBox row = new HBox(6, text, bar);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Button quick(String text, String tip, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add(text.equals("−") ? "danger-button" : "dnd-button");
        b.setStyle("-fx-min-width: 30; -fx-pref-width: 30; -fx-padding: 2 0 2 0; -fx-font-size: 14px;");
        b.setTooltip(new Tooltip(tip));
        b.setOnAction(e -> { action.run(); e.consume(); });
        return b;
    }

    // --------------------------------------------------------------- details

    private VBox detailFrame(String name, Node... body) {
        Button back = new Button("← Back");
        back.getStyleClass().add("dnd-button");
        back.setStyle("-fx-min-width: 0; -fx-padding: 5 12 5 12;");
        back.setOnAction(e -> showList());
        Label title = owner.sectionLabel(name);
        title.setWrapText(true);
        HBox header = new HBox(10, back, title);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 10, 6, 10));

        VBox content = new VBox(12, body);
        root.getChildren().setAll(header, scroll(content));
        return content;
    }

    void showPlayer(PlayerCharacter pc) {
        Runnable persist = () -> repos.players().save(pc);

        GridPane vitals = vitalsGrid(
            pc.getCurrentHitPoints(), v -> { pc.setCurrentHitPoints(v); persist.run(); },
            pc.getMaxHitPoints(), v -> { pc.setMaxHitPoints(v); persist.run(); },
            pc.getCurrentMana(), v -> { pc.setCurrentMana(v); persist.run(); },
            pc.getMaxMana(), v -> { pc.setMaxMana(v); persist.run(); });

        VBox itemsBox = new VBox(6);
        Button addItem = smallBtn("+ Add Item", () -> pickItem(item -> {
            List<PlayerCharacter.PlayerItem> items = pc.getItems();
            if (items == null) {
                items = new ArrayList<>();
                pc.setItems(items);
            }
            PlayerCharacter.PlayerItem stack = items.stream()
                .filter(i -> i != null && item.getId().equals(i.getItemId())).findFirst().orElse(null);
            if (stack != null) stack.setQuantity(stack.getQuantity() + 1);
            else items.add(new PlayerCharacter.PlayerItem(item.getId(), new PlayerCharacter.ItemCondition(100), false));
            persist.run();
            refreshItems(itemsBox, pc, persist);
        }));

        VBox effectsBox = new VBox(6);
        Runnable refreshEffects = new Runnable() {
            @Override
            public void run() {
                fillEffects(effectsBox, pc.getActiveEffects(), effect -> {
                    pc.clearEffect(effect);
                    persist.run();
                    this.run();
                });
            }
        };
        Button addEffect = smallBtn("+ Add Effect", () -> pickEffect(effect -> {
            pc.addEffect(effect);
            persist.run();
            refreshEffects.run();
        }));

        refreshItems(itemsBox, pc, persist);
        refreshEffects.run();

        detailFrame(pc.getName(),
            ProgressionPanel.build(owner, repos, pc, () -> showPlayer(pc)),
            subLabel("Vitals"), vitals,
            headerRow("Items", addItem), itemsBox,
            headerRow("Active Effects", addEffect), effectsBox);
    }

    void showCreature(TrackedCreature c) {
        TextField name = new TextField(c.getName());
        name.getStyleClass().add("dnd-text-field");
        name.textProperty().addListener((o, old, v) -> { c.setName(v); saveCreatures(); });

        GridPane vitals = vitalsGrid(
            c.getCurrentHitPoints(), v -> { c.setCurrentHitPoints(v); saveCreatures(); },
            c.getMaxHitPoints(), v -> { c.setMaxHitPoints(v); saveCreatures(); },
            c.getCurrentMana(), v -> { c.setCurrentMana(v); saveCreatures(); },
            c.getMaxMana(), v -> { c.setMaxMana(v); saveCreatures(); });
        Spinner<Integer> ac = spinner(c.getArmorClass(), v -> { c.setArmorClass(v); saveCreatures(); });
        vitals.addRow(2, owner.body("AC:"), ac);

        VBox effectsBox = new VBox(6);
        Runnable refreshEffects = new Runnable() {
            @Override
            public void run() {
                fillEffects(effectsBox, c.getActiveEffects(), effect -> {
                    c.getActiveEffects().remove(effect);
                    saveCreatures();
                    this.run();
                });
            }
        };
        Button addEffect = smallBtn("+ Add Effect", () -> pickEffect(effect -> {
            c.getActiveEffects().add(effect);
            saveCreatures();
            refreshEffects.run();
        }));
        refreshEffects.run();

        TextArea notes = new TextArea(c.getNotes() == null ? "" : c.getNotes());
        notes.setWrapText(true);
        notes.setPrefRowCount(4);
        notes.textProperty().addListener((o, old, v) -> { c.setNotes(v); saveCreatures(); });

        Button remove = owner.dangerBtn("Remove from this session", () -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove " + nameOf(c) + " from this session's list?", ButtonType.OK, ButtonType.CANCEL);
            confirm.setHeaderText(null);
            owner.styleDialog(confirm);
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
            creatures.remove(c);
            saveCreatures();
            showList();
        });
        remove.setMaxWidth(Double.MAX_VALUE);

        detailFrame(nameOf(c),
            subLabel("Name"), name,
            subLabel("Vitals"), vitals,
            headerRow("Active Effects", addEffect), effectsBox,
            subLabel("Notes"), notes,
            remove);
    }

    private GridPane vitalsGrid(int curHp, IntConsumer setCurHp, int maxHp, IntConsumer setMaxHp,
                                int curMana, IntConsumer setCurMana, int maxMana, IntConsumer setMaxMana) {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.addRow(0, owner.body("HP:"), spinner(curHp, setCurHp), owner.body("of"), spinner(maxHp, setMaxHp));
        grid.addRow(1, owner.body("Mana:"), spinner(curMana, setCurMana), owner.body("of"), spinner(maxMana, setMaxMana));
        return grid;
    }

    private Spinner<Integer> spinner(int value, IntConsumer onChange) {
        Spinner<Integer> s = new Spinner<>(0, 9999, Math.max(0, value));
        s.setEditable(true);
        s.setPrefWidth(90);
        s.valueProperty().addListener((o, old, n) -> { if (n != null) onChange.accept(n); });
        return s;
    }

    private void refreshItems(VBox box, PlayerCharacter pc, Runnable persist) {
        box.getChildren().clear();
        List<PlayerCharacter.PlayerItem> items = pc.getItems();
        if (items == null || items.isEmpty()) {
            box.getChildren().add(hint("No items."));
            return;
        }
        for (PlayerCharacter.PlayerItem item : new ArrayList<>(items)) {
            Item catalogItem = repos.items().getById(item.getItemId());
            Label label = owner.body((catalogItem != null ? catalogItem.getName() : item.getItemId())
                + (item.getQuantity() > 1 ? "  ×" + item.getQuantity() : ""));
            label.setWrapText(true);
            HBox.setHgrow(label, Priority.ALWAYS);
            label.setMaxWidth(Double.MAX_VALUE);
            Button less = quick("−", "Use up / lose one", () -> {
                if (item.getQuantity() > 1) item.setQuantity(item.getQuantity() - 1);
                else items.remove(item);
                persist.run();
                refreshItems(box, pc, persist);
            });
            Button more = quick("+", "Found or crafted one more", () -> {
                item.setQuantity(item.getQuantity() + 1);
                persist.run();
                refreshItems(box, pc, persist);
            });
            HBox row = new HBox(6, label, less, more);
            row.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().add(row);
        }
    }

    private void fillEffects(VBox box, List<ActiveEffect> effects, Consumer<ActiveEffect> clear) {
        box.getChildren().clear();
        if (effects == null || effects.isEmpty()) {
            box.getChildren().add(hint("No active effects."));
            return;
        }
        for (ActiveEffect effect : new ArrayList<>(effects)) {
            Label label = owner.body(effect.label());
            label.setWrapText(true);
            label.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(label, Priority.ALWAYS);
            Button x = new Button("Clear");
            x.getStyleClass().add("danger-button");
            x.setStyle("-fx-min-width: 0; -fx-padding: 3 8 3 8;");
            x.setOnAction(e -> clear.accept(effect));
            HBox row = new HBox(6, label, x);
            row.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().add(row);
        }
    }

    private void pickItem(Consumer<Item> then) {
        List<Item> catalog = repos.items().list();
        if (catalog.isEmpty()) {
            status.accept("No items in this campaign's catalog yet.");
            return;
        }
        ChoiceDialog<Item> pick = new ChoiceDialog<>(catalog.get(0), catalog);
        pick.setTitle("Add Item");
        pick.setHeaderText(null);
        pick.setContentText("Item:");
        owner.styleDialog(pick);
        pick.showAndWait().ifPresent(then);
    }

    private void pickEffect(Consumer<ActiveEffect> then) {
        List<Effect> catalog = repos.effects().list();
        if (catalog.isEmpty()) {
            status.accept("No effects in this campaign's catalog yet.");
            return;
        }
        ChoiceDialog<Effect> pick = new ChoiceDialog<>(catalog.get(0), catalog);
        pick.setTitle("Add Effect");
        pick.setHeaderText(null);
        pick.setContentText("Effect:");
        owner.styleDialog(pick);
        pick.showAndWait().ifPresent(effect -> {
            TextInputDialog rounds = new TextInputDialog(String.valueOf(Math.max(1, effect.getDurationRounds())));
            rounds.setTitle("Add Effect");
            rounds.setHeaderText(null);
            rounds.setContentText("Rounds remaining:");
            owner.styleDialog(rounds);
            rounds.showAndWait().ifPresent(raw -> {
                try {
                    int count = Integer.parseInt(raw.trim());
                    then.accept(new ActiveEffect(effect.getId(), effect.getName(), count,
                        effect.getDamageAmount(), effect.getHealingAmount(), "DM"));
                } catch (NumberFormatException ex) {
                    status.accept("\"" + raw + "\" isn't a whole number.");
                }
            });
        });
    }

    // ---------------------------------------------------------------- helpers

    private ScrollPane scroll(Node content) {
        VBox padded = new VBox(content);
        padded.setPadding(new Insets(4, 10, 12, 10));
        ScrollPane scroll = new ScrollPane(padded);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background: #141428; -fx-background-color: #141428;");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        return scroll;
    }

    private Node headerRow(String text, Button action) {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(8, subLabel(text), spacer, action);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Button smallBtn(String text, Runnable action) {
        Button b = owner.btn(text, action);
        b.setStyle("-fx-min-width: 0; -fx-padding: 3 10 3 10; -fx-font-size: 12px;");
        return b;
    }

    private Label subLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("sub-label");
        return l;
    }

    private Label hint(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("muted-label");
        l.setWrapText(true);
        return l;
    }

    private static String nameOf(TrackedCreature c) {
        return c.getName() == null || c.getName().isBlank() ? "(unnamed)" : c.getName();
    }
}
