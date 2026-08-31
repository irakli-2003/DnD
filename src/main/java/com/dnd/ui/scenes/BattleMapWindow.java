package com.dnd.ui.scenes;

import com.dnd.data.CampaignRepositories;
import com.dnd.model.combat.Ability;
import com.dnd.model.combat.InitiativeTracker;
import com.dnd.model.item.Item;
import com.dnd.model.magic.Spell;
import com.dnd.model.world.map.*;
import com.dnd.ui.ImageStore;
import com.dnd.ui.MapRenderer;
import com.dnd.ui.UiSession;
import com.dnd.ui.components.SessionTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * The battle playground: a live, editable view of one {@link GameMap} used to run an
 * encounter.
 *
 * <p>This is deliberately separate from the map editor. The editor is about building
 * terrain - layers, drawings, passability - while this window is about running a fight:
 * moving creatures, tracking hit points and mana, cycling turn order, and answering
 * "what can this thing actually do?" without leaving the table. Everything it changes is
 * stored on the map's tokens, so a fight can be paused and resumed.</p>
 */
public class BattleMapWindow {

    /** A grid square is five feet, the usual tabletop scale, used to convert spell ranges. */
    private static final double FEET_PER_CELL = 5.0;

    private final BaseScene owner;
    private final UiSession uiSession;
    private final CampaignRepositories repos;
    private final GameMap map;

    private final MapRenderer renderer;
    private final MapRenderer.Decorations decorations = new MapRenderer.Decorations();
    private final InitiativeTracker initiative = new InitiativeTracker();

    private Canvas canvas;
    private StackPane panelHost;
    private VBox rosterPanel;
    private ScrollPane detailScroll;
    private MapObject detailToken;
    private VBox detailBars;
    private Label statusLabel;
    private Label roundLabel;
    private SessionTimer timer;

    private MapObject dragging;
    private boolean dirty;

    public BattleMapWindow(BaseScene owner, UiSession uiSession, String mapId) {
        this.owner = owner;
        this.uiSession = uiSession;
        this.repos = new CampaignRepositories(uiSession.campaignRoot());
        this.map = repos.maps().getById(mapId);
        this.renderer = new MapRenderer(uiSession.campaignRoot());
    }

    /** True when the requested map id didn't resolve, so callers can report it themselves. */
    public boolean mapExists() {
        return map != null;
    }

    public void show() {
        if (map == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "That map no longer exists in this campaign.");
            owner.styleDialog(alert);
            alert.showAndWait();
            return;
        }
        map.ensureGridSize();
        rebuildInitiative();

        Stage stage = new Stage();
        stage.initModality(Modality.NONE);
        stage.setTitle("Battle: " + map.getName());

        canvas = new Canvas(map.getWidth() * renderer.getCellSize(), map.getHeight() * renderer.getCellSize());
        wireCanvas();

        ScrollPane mapScroll = new ScrollPane(new Group(canvas));
        mapScroll.setStyle("-fx-background: #1a1a2e; -fx-background-color: #1a1a2e;");
        mapScroll.setPannable(false);

        panelHost = new StackPane();
        panelHost.setPrefWidth(340);
        panelHost.setMinWidth(340);
        panelHost.getStyleClass().add("battle-panel");

        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");
        root.setTop(buildToolbar(stage));
        root.setLeft(panelHost);
        root.setCenter(mapScroll);
        root.setBottom(buildStatusBar());

        showRoster();
        render();

        Scene scene = owner.themedScene(root, 1200, 800);
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                decorations.selected = null;
                showRoster();
                render();
            }
        });
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.setOnCloseRequest(e -> {
            if (timer != null) timer.dispose();
            if (dirty) saveMap();
        });
        stage.show();
    }

    // ── Toolbar ─────────────────────────────────────────────────────────────

    private Node buildToolbar(Stage stage) {
        FlowPane bar = new FlowPane(6, 6);
        bar.setPadding(new Insets(8, 10, 8, 10));
        bar.getStyleClass().add("editor-toolbar");

        roundLabel = new Label();
        roundLabel.getStyleClass().add("section-label");

        timer = new SessionTimer();

        bar.getChildren().addAll(
            tool("Roll Initiative", "Roll d20 + dexterity for every combatant and restart the order", () -> {
                initiative.rollAll();
                refreshAll();
                status("Rolled initiative for " + initiative.order().size() + " combatants.");
            }),
            tool("Next Turn ▸", "Advance to the next living combatant", () -> {
                MapObject next = initiative.next();
                refreshAll();
                status(next == null ? "Nothing left standing." : "Now acting: " + TokenSupport.nameOf(next));
            }),
            roundLabel,
            new Separator(),
            tool("Add Token...", "Place another creature on the map", this::openAddTokenDialog),
            tool("Remove Selected", "Take the selected token off the map", this::removeSelected),
            new Separator(),
            tool("Toggle Wall", "Make the selected token's square impassable, or passable again",
                this::toggleWallUnderSelection),
            tool("Health Bars", "Show or hide health and mana bars on tokens", () -> {
                decorations.showHealthBars = !decorations.showHealthBars;
                render();
            }),
            tool("Zoom -", "Shrink the grid", () -> zoom(-8)),
            tool("Zoom +", "Enlarge the grid", () -> zoom(8)),
            new Separator(),
            tool("Save", "Write the battle state back to the campaign", () -> {
                saveMap();
                status("Battle state saved.");
            }),
            tool("Close", "Close the battle map", stage::close),
            new Separator(),
            timer
        );
        return bar;
    }

    private Button tool(String text, String tooltip, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().add("dnd-button");
        b.setTooltip(new Tooltip(tooltip));
        b.setOnAction(e -> action.run());
        return b;
    }

    private Node buildStatusBar() {
        statusLabel = new Label("Click a token to inspect it. Drag it to move. Esc clears the selection.");
        statusLabel.getStyleClass().add("body-label");
        HBox bar = new HBox(statusLabel);
        bar.setPadding(new Insets(6, 12, 8, 12));
        bar.setStyle("-fx-background-color: #0f0f1e;");
        return bar;
    }

    private void status(String text) {
        if (statusLabel != null) statusLabel.setText(text);
    }

    private void zoom(double delta) {
        renderer.setCellSize(renderer.getCellSize() + delta);
        canvas.setWidth(map.getWidth() * renderer.getCellSize());
        canvas.setHeight(map.getHeight() * renderer.getCellSize());
        render();
    }

    // ── Canvas interaction ──────────────────────────────────────────────────

    private void wireCanvas() {
        canvas.setOnMousePressed(e -> {
            MapObject hit = tokenAt(e.getX(), e.getY());
            dragging = hit;
            decorations.selected = hit;
            decorations.rangeOrigin = null;
            if (hit == null) showRoster();
            else showDetail(hit);
            render();
        });

        canvas.setOnMouseDragged(e -> {
            if (dragging == null) return;
            int cx = (int) (e.getX() / renderer.getCellSize());
            int cy = (int) (e.getY() / renderer.getCellSize());
            moveToken(dragging, cx, cy);
        });

        canvas.setOnMouseReleased(e -> dragging = null);
    }

    private MapObject tokenAt(double px, double py) {
        int cx = (int) (px / renderer.getCellSize());
        int cy = (int) (py / renderer.getCellSize());
        if (cx < 0 || cy < 0 || cx >= map.getWidth() || cy >= map.getHeight()) return null;
        List<MapObject> occupants = map.getCell(cx, cy).getOccupants();
        // The topmost occupant is the one drawn last and so the one the DM sees and means.
        return occupants.isEmpty() ? null : occupants.get(occupants.size() - 1);
    }

    private void moveToken(MapObject token, int cx, int cy) {
        if (cx < 0 || cy < 0 || cx >= map.getWidth() || cy >= map.getHeight()) return;
        Position current = token.getPosition();
        if (current != null && current.getX() == cx && current.getY() == cy) return;
        try {
            map.moveObject(token, cx, cy);
            dirty = true;
            render();
        } catch (IllegalArgumentException | IllegalStateException ex) {
            // Walking into a wall is a normal thing to try, so it reports rather than throws.
            status("Can't move there: " + ex.getMessage());
        }
    }

    private void removeSelected() {
        MapObject token = decorations.selected;
        if (token == null) {
            status("Select a token on the map first.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Remove " + TokenSupport.nameOf(token) + " from this map?");
        confirm.setHeaderText(null);
        owner.styleDialog(confirm);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        map.removeObject(token);
        initiative.remove(token);
        decorations.selected = null;
        dirty = true;
        showRoster();
        refreshAll();
        status("Removed " + TokenSupport.nameOf(token) + ".");
    }

    private void toggleWallUnderSelection() {
        MapObject token = decorations.selected;
        if (token == null || token.getPosition() == null) {
            status("Select a token to toggle the wall under it.");
            return;
        }
        GridCell cell = map.getCell(token.getPosition().getX(), token.getPosition().getY());
        cell.setPassable(!cell.isPassable());
        dirty = true;
        render();
        status(cell.isPassable() ? "Square is passable." : "Square is now a wall.");
    }

    // ── Roster panel ────────────────────────────────────────────────────────

    private void rebuildInitiative() {
        List<MapObject> creatures = new ArrayList<>();
        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                for (MapObject obj : map.getCell(x, y).getOccupants()) {
                    if (TokenSupport.isCreature(obj)) creatures.add(obj);
                }
            }
        }
        initiative.setCombatants(creatures);
    }

    private void refreshAll() {
        rebuildInitiative();
        decorations.currentTurn = initiative.current();
        if (roundLabel != null) roundLabel.setText("Round " + initiative.round());
        // Only rebuild the roster when it is the visible panel; otherwise this would throw
        // away a token detail sheet the DM is working in.
        if (panelHost != null && detailScroll == null) showRoster();
        render();
    }

    private void render() {
        decorations.currentTurn = initiative.current();
        if (roundLabel != null) roundLabel.setText("Round " + initiative.round());
        renderer.render(canvas, map, decorations);
    }

    private void showRoster() {
        detailScroll = null;
        detailToken = null;
        detailBars = null;
        rosterPanel = new VBox(10);
        rosterPanel.setPadding(new Insets(12));

        rosterPanel.getChildren().add(owner.sectionLabel("Turn Order"));
        rosterPanel.getChildren().add(buildTurnStack());

        rosterPanel.getChildren().add(new Separator());
        rosterPanel.getChildren().add(owner.sectionLabel("Combatants"));
        for (MapObject token : initiative.order()) {
            rosterPanel.getChildren().add(buildRosterRow(token));
        }
        if (initiative.order().isEmpty()) {
            rosterPanel.getChildren().add(owner.body("No creatures on this map yet. Use \"Add Token...\"."));
        }

        ScrollPane scroll = new ScrollPane(rosterPanel);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("scroll-pane");
        panelHost.getChildren().setAll(scroll);
    }

    /**
     * The "up next" stack: the acting creature plus the following few. Showing the whole
     * cycle would be meaningless because the order repeats forever.
     */
    private Node buildTurnStack() {
        VBox stack = new VBox(4);
        MapObject current = initiative.current();
        if (current == null) {
            stack.getChildren().add(owner.body("Nobody is standing."));
            return stack;
        }
        stack.getChildren().add(turnChip(current, true));
        for (MapObject next : initiative.upcoming(3)) {
            stack.getChildren().add(turnChip(next, false));
        }
        return stack;
    }

    private Node turnChip(MapObject token, boolean active) {
        Label label = new Label((active ? "▶ " : "   ") + TokenSupport.nameOf(token)
            + "  (" + TokenSupport.combatOf(token).getInitiative() + ")");
        label.getStyleClass().add(active ? "turn-chip-active" : "turn-chip");
        label.setMaxWidth(Double.MAX_VALUE);
        label.setOnMouseClicked(e -> select(token));
        return label;
    }

    private Node buildRosterRow(MapObject token) {
        CombatState state = TokenSupport.combatOf(token);

        Label name = new Label(TokenSupport.nameOf(token));
        name.getStyleClass().add("body-label");
        Label kind = new Label(TokenSupport.kindOf(token));
        kind.getStyleClass().add("subtitle-label");

        VBox text = new VBox(1, name, kind, healthBar(state, 190));
        HBox row = new HBox(8, portrait(token, 34), text);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(4));
        row.getStyleClass().add("roster-row");
        if (state.isDead()) row.getStyleClass().add("roster-row-dead");
        row.setOnMouseClicked(e -> select(token));
        return row;
    }

    private void select(MapObject token) {
        decorations.selected = token;
        showDetail(token);
        render();
    }

    private Node healthBar(CombatState state, double width) {
        StackPane bar = new StackPane();
        Rectangle back = new Rectangle(width, 10, Color.web("#000000aa"));
        double fraction = state.getMaxHitPoints() > 0 ? state.healthFraction() : 0;
        Rectangle front = new Rectangle(Math.max(0, width * fraction), 10,
            fraction > 0.5 ? Color.web("#4caf50") : fraction > 0.25 ? Color.web("#d9a441") : Color.web("#c0392b"));
        StackPane.setAlignment(front, Pos.CENTER_LEFT);
        StackPane.setAlignment(back, Pos.CENTER_LEFT);

        Label text = new Label(state.getCurrentHitPoints() + " / " + state.getMaxHitPoints()
            + (state.isDead() ? "  DEAD" : state.isDowned() ? "  DYING (" + state.remainingDeathSaves() + ")" : ""));
        text.getStyleClass().add("bar-label");
        bar.getChildren().addAll(back, front, text);
        bar.setMaxWidth(width);
        return bar;
    }

    private Node portrait(MapObject token, double size) {
        String path = TokenSupport.imagePathOf(token);
        if (path != null && uiSession.campaignRoot() != null) {
            Image img = ImageStore.load(uiSession.campaignRoot(), path);
            if (img != null) {
                ImageView view = new ImageView(img);
                view.setFitWidth(size);
                view.setFitHeight(size);
                view.setPreserveRatio(false);
                Circle clip = new Circle(size / 2, size / 2, size / 2);
                view.setClip(clip);
                return view;
            }
        }
        StackPane fallback = new StackPane();
        Circle circle = new Circle(size / 2, MapRenderer.colorFor(token));
        Label initials = new Label(MapRenderer.abbrev(TokenSupport.nameOf(token)));
        initials.setStyle("-fx-text-fill: white; -fx-font-weight: bold;");
        fallback.getChildren().addAll(circle, initials);
        return fallback;
    }

    // ── Detail panel ────────────────────────────────────────────────────────

    /**
     * Replaces the roster with everything about one creature. The "Back" button at the top
     * left returns to the roster, which is the only way back by design: the panel is a
     * drill-down, not a second window competing for screen space.
     */
    private void showDetail(MapObject token) {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(12));

        Button back = new Button("← Back");
        back.getStyleClass().add("dnd-button");
        back.setOnAction(e -> {
            decorations.rangeOrigin = null;
            showRoster();
            render();
        });
        HBox backRow = new HBox(back);
        backRow.setAlignment(Pos.CENTER_LEFT);
        panel.getChildren().add(backRow);

        CombatState state = TokenSupport.combatOf(token);

        Label name = new Label(TokenSupport.nameOf(token));
        name.getStyleClass().add("title-label");
        Label kind = new Label(TokenSupport.kindOf(token) + " · level " + TokenSupport.levelOf(token));
        kind.getStyleClass().add("subtitle-label");
        detailToken = token;
        panel.getChildren().add(new HBox(10, portrait(token, 56), new VBox(2, name, kind)));

        String description = TokenSupport.descriptionOf(token);
        if (description != null && !description.isBlank()) {
            Label desc = new Label(description);
            desc.getStyleClass().add("body-label");
            desc.setWrapText(true);
            panel.getChildren().add(desc);
        }

        panel.getChildren().addAll(new Separator(), owner.sectionLabel("Condition"));
        detailBars = new VBox(6);
        refreshDetailBars(state);
        panel.getChildren().add(detailBars);
        panel.getChildren().add(buildVitalsEditor(token, state));
        panel.getChildren().add(buildDeathSaveControls(token, state));

        panel.getChildren().addAll(new Separator(), owner.sectionLabel("Purse & Stats"));
        panel.getChildren().add(buildPurseEditor(state));
        panel.getChildren().add(buildStatsGrid(token));

        List<Spell> spells = resolveSpells(token);
        if (!spells.isEmpty()) {
            panel.getChildren().addAll(new Separator(), owner.sectionLabel("Spells"));
            panel.getChildren().add(buildSpellList(token, spells));
        }

        List<Ability> abilities = TokenSupport.abilitiesOf(token);
        if (!abilities.isEmpty()) {
            panel.getChildren().addAll(new Separator(), owner.sectionLabel("Abilities"));
            panel.getChildren().add(buildAbilityList(token, abilities));
        }

        List<Item> items = resolveItems(token);
        if (!items.isEmpty()) {
            panel.getChildren().addAll(new Separator(), owner.sectionLabel("Items"));
            panel.getChildren().add(buildItemList(items));
        }

        detailScroll = new ScrollPane(panel);
        detailScroll.setFitToWidth(true);
        detailScroll.getStyleClass().add("scroll-pane");
        panelHost.getChildren().setAll(detailScroll);
    }

    private Node buildVitalsEditor(MapObject token, CombatState state) {
        Spinner<Integer> amount = new Spinner<>(1, 999, 5);
        amount.setPrefWidth(80);
        amount.setEditable(true);

        Button damage = new Button("Damage");
        damage.getStyleClass().add("danger-button");
        damage.setOnAction(e -> {
            state.applyDamage(amount.getValue());
            afterVitalsChange(token);
        });

        Button heal = new Button("Heal");
        heal.getStyleClass().add("dnd-button");
        heal.setOnAction(e -> {
            state.heal(amount.getValue());
            afterVitalsChange(token);
        });

        Spinner<Integer> maxHp = new Spinner<>(0, 9999, state.getMaxHitPoints());
        maxHp.setPrefWidth(90);
        maxHp.setEditable(true);
        maxHp.valueProperty().addListener((obs, o, n) -> {
            state.setMaxHitPoints(n);
            afterVitalsChange(token);
        });

        Spinner<Integer> currentMana = new Spinner<>(0, 9999, state.getCurrentMana());
        currentMana.setPrefWidth(90);
        currentMana.setEditable(true);
        currentMana.valueProperty().addListener((obs, o, n) -> {
            state.setCurrentMana(n);
            afterVitalsChange(token);
        });

        Spinner<Integer> maxMana = new Spinner<>(0, 9999, state.getMaxMana());
        maxMana.setPrefWidth(90);
        maxMana.setEditable(true);
        maxMana.valueProperty().addListener((obs, o, n) -> {
            state.setMaxMana(n);
            afterVitalsChange(token);
        });

        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        grid.addRow(0, amount, damage, heal);
        grid.addRow(1, owner.body("Max HP:"), maxHp);
        grid.addRow(2, owner.body("Mana:"), currentMana, maxMana);
        return grid;
    }

    private Node buildDeathSaveControls(MapObject token, CombatState state) {
        Button fail = new Button("Failed Save");
        fail.getStyleClass().add("danger-button");
        fail.setTooltip(new Tooltip("Count a failed death save; the third one kills"));
        fail.setOnAction(e -> {
            state.failDeathSave();
            afterVitalsChange(token);
        });

        Button succeed = new Button("Made Save");
        succeed.getStyleClass().add("dnd-button");
        succeed.setOnAction(e -> {
            state.succeedDeathSave();
            afterVitalsChange(token);
        });

        Button kill = new Button("Kill");
        kill.getStyleClass().add("danger-button");
        kill.setOnAction(e -> {
            state.setDead(true);
            afterVitalsChange(token);
        });

        Button revive = new Button("Revive");
        revive.getStyleClass().add("dnd-button");
        revive.setOnAction(e -> {
            state.revive();
            afterVitalsChange(token);
        });

        FlowPane row = new FlowPane(6, 6, fail, succeed, kill, revive);
        return row;
    }

    private Node buildPurseEditor(CombatState state) {
        Spinner<Integer> gold = new Spinner<>(0, 999999, state.getGold());
        gold.setPrefWidth(110);
        gold.setEditable(true);
        gold.valueProperty().addListener((obs, o, n) -> {
            state.setGold(n);
            dirty = true;
        });

        Spinner<Integer> init = new Spinner<>(-20, 99, state.getInitiative());
        init.setPrefWidth(90);
        init.setEditable(true);
        init.valueProperty().addListener((obs, o, n) -> {
            state.setInitiative(n);
            dirty = true;
            rebuildInitiative();
            render();
        });

        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        grid.addRow(0, owner.body("Coins:"), gold);
        grid.addRow(1, owner.body("Initiative:"), init);
        return grid;
    }

    private Node buildStatsGrid(MapObject token) {
        var stats = TokenSupport.statsOf(token);
        if (stats == null) return new Label();
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(3);
        addStat(grid, 0, "STR", stats.getStrength());
        addStat(grid, 1, "DEX", stats.getDexterity());
        addStat(grid, 2, "CON", stats.getConstitution());
        addStat(grid, 3, "INT", stats.getIntelligence());
        addStat(grid, 4, "WIS", stats.getWisdom());
        addStat(grid, 5, "CHA", stats.getCharisma());
        return grid;
    }

    private void addStat(GridPane grid, int index, String label, int value) {
        int modifier = TokenSupport.modifier(value);
        Label cell = new Label(label + " " + value + " (" + (modifier >= 0 ? "+" : "") + modifier + ")");
        cell.getStyleClass().add("body-label");
        grid.add(cell, index % 3, index / 3);
    }

    /**
     * Spells and abilities preview their reach on hover rather than on click, so the DM can
     * sweep the list and see each option's coverage without committing to anything.
     */
    private Node buildSpellList(MapObject token, List<Spell> spells) {
        VBox box = new VBox(2);
        for (Spell spell : spells) {
            Label row = new Label(spell.getName() + "  ·  lvl " + spell.getLevel()
                + (spell.getManaCost() > 0 ? "  ·  " + spell.getManaCost() + " mana" : "")
                + (spell.getRange() > 0 ? "  ·  " + spell.getRange() + " ft" : ""));
            row.getStyleClass().add("hover-row");
            row.setMaxWidth(Double.MAX_VALUE);
            row.setWrapText(true);
            if (spell.getDescription() != null) row.setTooltip(new Tooltip(spell.getDescription()));
            row.setOnMouseEntered(e -> previewRange(token, spell.getRange() / FEET_PER_CELL,
                spell.getRadius() / FEET_PER_CELL));
            row.setOnMouseExited(e -> clearRange());
            box.getChildren().add(row);
        }
        return box;
    }

    private Node buildAbilityList(MapObject token, List<Ability> abilities) {
        VBox box = new VBox(2);
        for (Ability ability : abilities) {
            Label row = new Label(ability.getName()
                + (ability.getRange() > 0 ? "  ·  " + (int) ability.getRange() + " ft" : ""));
            row.getStyleClass().add("hover-row");
            row.setMaxWidth(Double.MAX_VALUE);
            row.setWrapText(true);
            if (ability.getDescription() != null) row.setTooltip(new Tooltip(ability.getDescription()));
            row.setOnMouseEntered(e -> previewRange(token, ability.getRange() / FEET_PER_CELL, 0));
            row.setOnMouseExited(e -> clearRange());
            box.getChildren().add(row);
        }
        return box;
    }

    private Node buildItemList(List<Item> items) {
        VBox box = new VBox(2);
        for (Item item : items) {
            Label row = new Label("• " + item.getName());
            row.getStyleClass().add("body-label");
            row.setWrapText(true);
            box.getChildren().add(row);
        }
        return box;
    }

    private void previewRange(MapObject token, double rangeCells, double radiusCells) {
        decorations.rangeOrigin = token;
        decorations.rangeCells = rangeCells;
        decorations.rangeRadiusCells = radiusCells;
        render();
    }

    private void clearRange() {
        decorations.rangeOrigin = null;
        decorations.rangeCells = 0;
        decorations.rangeRadiusCells = 0;
        render();
    }

    /**
     * Vitals change constantly during a fight, so only the bars are rebuilt. Recreating the
     * whole panel would tear the spinner the DM is mid-edit in out from under them.
     */
    private void refreshDetailBars(CombatState state) {
        if (detailBars == null) return;
        detailBars.getChildren().setAll(healthBar(state, 300));
    }

    private void afterVitalsChange(MapObject token) {
        dirty = true;
        rebuildInitiative();
        if (detailToken == token && detailBars != null) {
            refreshDetailBars(TokenSupport.combatOf(token));
        } else {
            showDetail(token);
        }
        render();
    }

    private List<Spell> resolveSpells(MapObject token) {
        List<Spell> spells = new ArrayList<>();
        for (String id : TokenSupport.spellIdsOf(token)) {
            Spell spell = repos.spells().getById(id);
            if (spell != null) spells.add(spell);
        }
        return spells;
    }

    private List<Item> resolveItems(MapObject token) {
        List<Item> items = new ArrayList<>();
        for (String id : TokenSupport.itemIdsOf(token)) {
            Item item = repos.items().getById(id);
            if (item != null) items.add(item);
        }
        return items;
    }

    // ── Adding tokens ───────────────────────────────────────────────────────

    private void openAddTokenDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add Token");
        dialog.setHeaderText("Place a creature on the battle map");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        owner.styleDialog(dialog);

        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("Player", "NPC", "Monster", "Beast");
        typeBox.setValue("Monster");

        ListView<String> entries = new ListView<>();
        entries.getStyleClass().add("dnd-list-view");
        entries.setPrefHeight(220);

        Runnable refresh = () -> {
            entries.getItems().clear();
            switch (typeBox.getValue()) {
                case "Player" -> repos.players().list().forEach(p -> entries.getItems().add(p.getId() + " | " + p.getName()));
                case "NPC" -> repos.npcs().list().forEach(n -> entries.getItems().add(n.getId() + " | " + n.getName()));
                case "Monster" -> repos.monsters().list().forEach(m -> entries.getItems().add(m.getId() + " | " + m.getName()));
                case "Beast" -> repos.beasts().list().forEach(b -> entries.getItems().add(b.getId() + " | " + b.getName()));
                default -> { }
            }
        };
        typeBox.setOnAction(e -> refresh.run());
        refresh.run();

        Spinner<Integer> xSpinner = new Spinner<>(0, map.getWidth() - 1, 0);
        Spinner<Integer> ySpinner = new Spinner<>(0, map.getHeight() - 1, 0);
        xSpinner.setEditable(true);
        ySpinner.setEditable(true);

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.addRow(0, new Label("Type:"), typeBox);
        grid.addRow(1, new Label("Entry:"), entries);
        grid.addRow(2, new Label("X:"), xSpinner, new Label("Y:"), ySpinner);
        dialog.getDialogPane().setContent(grid);

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        String selected = entries.getSelectionModel().getSelectedItem();
        if (selected == null) {
            status("No entry chosen, nothing was added.");
            return;
        }
        addToken(typeBox.getValue(), selected.split("\\|")[0].trim(), xSpinner.getValue(), ySpinner.getValue());
    }

    private void addToken(String type, String id, int x, int y) {
        MapObject token = switch (type) {
            case "Player" -> {
                var pc = repos.players().getById(id);
                yield pc == null ? null : new PlayerToken(pc);
            }
            case "NPC" -> {
                var npc = repos.npcs().getById(id);
                yield npc == null ? null : new NpcToken(npc);
            }
            case "Monster" -> {
                var monster = repos.monsters().getById(id);
                yield monster == null ? null : new MonsterToken(monster);
            }
            case "Beast" -> {
                var beast = repos.beasts().getById(id);
                yield beast == null ? null : new BeastToken(beast);
            }
            default -> null;
        };
        if (token == null) {
            status("Could not find that entry in the campaign.");
            return;
        }
        try {
            map.placeObject(token, x, y);
        } catch (RuntimeException ex) {
            status("Could not place the token: " + ex.getMessage());
            return;
        }
        // Rolling straight away means a reinforcement slots into the running order instead of
        // sitting at initiative zero until the DM notices.
        CombatState state = TokenSupport.combatOf(token);
        state.setInitiative(TokenSupport.rollInitiative(token));
        dirty = true;
        refreshAll();
        showRoster();
        status("Added " + TokenSupport.nameOf(token) + " at (" + x + "," + y + ").");
    }

    private void saveMap() {
        repos.maps().save(map);
        dirty = false;
    }
}
