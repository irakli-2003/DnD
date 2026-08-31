package com.dnd.ui.scenes;

import com.dnd.data.CampaignRepositories;
import com.dnd.data.EntityInfoFormatter;
import com.dnd.data.StorylineService;
import com.dnd.ui.EntityCategory;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Full-window editor for a Storyline session file.
 *
 * <p>Beyond plain text editing it provides a toolbar for the two things a DM does most
 * while prepping: pulling campaign data (NPCs, items, spells, ...) into the notes, and
 * separating prose that is read aloud to players from private DM notes. Those two kinds
 * of content are delimited with explicit markers so the same file can be shown either in
 * full (prep view) or as read-aloud text only (table view).</p>
 */
final class StorylineEditorWindow {

    static final String READ_ALOUD_OPEN = "[READ ALOUD]";
    static final String READ_ALOUD_CLOSE = "[/READ ALOUD]";
    static final String DM_NOTE_OPEN = "[DM NOTE]";
    static final String DM_NOTE_CLOSE = "[/DM NOTE]";

    private final BaseScene owner;
    private final StorylineService service;
    private final CampaignRepositories repos;
    private final Path file;

    private TextArea area;
    private Label statusLabel;
    private boolean dirty;

    StorylineEditorWindow(BaseScene owner, StorylineService service, CampaignRepositories repos, Path file) {
        this.owner = owner;
        this.service = service;
        this.repos = repos;
        this.file = file;
    }

    void show() {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(file.getFileName().toString());

        area = new TextArea(service.readText(file));
        area.setWrapText(true);
        area.getStyleClass().add("storyline-editor");
        // Without an explicit grow the TextArea keeps its preferred height and leaves a blank
        // gap when the window is maximized, which is the whole point of this editor being
        // a full window rather than a small dialog.
        area.setMaxHeight(Double.MAX_VALUE);
        area.setMaxWidth(Double.MAX_VALUE);
        VBox.setVgrow(area, Priority.ALWAYS);
        area.textProperty().addListener((obs, o, n) -> markDirty(true));

        statusLabel = new Label();
        statusLabel.getStyleClass().add("body-label");

        VBox layout = new VBox(0, buildToolbar(stage), area, buildStatusBar(stage));
        layout.getStyleClass().add("root");
        VBox.setVgrow(area, Priority.ALWAYS);

        Scene scene = owner.themedScene(layout, 1000, 720);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN), this::save);
        stage.setScene(scene);
        stage.setMaximized(true);
        markDirty(false);

        stage.setOnCloseRequest(e -> {
            if (dirty && !confirmDiscard()) e.consume();
        });
        stage.showAndWait();
    }

    // ---------------------------------------------------------------- toolbar

    private Node buildToolbar(Stage stage) {
        FlowPane bar = new FlowPane(6, 6);
        bar.setPadding(new Insets(8, 10, 8, 10));
        bar.getStyleClass().add("editor-toolbar");

        bar.getChildren().addAll(
            buildInsertInfoMenu(),
            buildInsertBlockMenu(),
            toolButton("Read-Aloud", "Wrap the selected text as prose to read to the players",
                () -> wrapSelection(READ_ALOUD_OPEN, READ_ALOUD_CLOSE)),
            toolButton("DM Note", "Wrap the selected text as a private DM-only note",
                () -> wrapSelection(DM_NOTE_OPEN, DM_NOTE_CLOSE)),
            new Separator(),
            toolButton("Heading", "Turn the current line into a section heading", this::insertHeading),
            toolButton("Bullets", "Turn the selected lines into a bullet list", () -> prefixLines("- ")),
            toolButton("Checklist", "Turn the selected lines into a checklist", () -> prefixLines("[ ] ")),
            toolButton("Divider", "Insert a horizontal divider", () -> insertAtCaret("\n" + "-".repeat(60) + "\n")),
            new Separator(),
            toolButton("Player View", "Show only the read-aloud passages, for reading at the table",
                this::openPlayerView),
            toolButton("Find", "Find text in this file", this::openFind),
            new Separator(),
            toolButton("Save", "Save this file (Ctrl+S)", this::save),
            toolButton("Save & Close", "Save and close the editor", () -> { save(); stage.close(); })
        );
        return bar;
    }

    /**
     * "Insert Info" - a cascading menu of campaign catalogs. Hovering a category opens a
     * second menu beside it listing that category's entries; picking one inserts a formatted
     * information block at the caret. Both levels are scrollable, and each entry list has a
     * filter box because catalogs like items and spells get long.
     */
    private MenuButton buildInsertInfoMenu() {
        MenuButton insert = new MenuButton("Insert Info ▾");
        insert.getStyleClass().add("dnd-button");
        insert.setTooltip(new Tooltip("Insert formatted information about a campaign entry"));

        EntityInfoFormatter formatter = new EntityInfoFormatter(repos);
        for (EntityCategory category : EntityInfoFormatter.insertableCategories()) {
            Menu categoryMenu = new Menu(EntityInfoFormatter.categoryLabel(category));
            // Entries are loaded when the submenu is first shown rather than up front, so
            // opening the toolbar doesn't read and parse every catalog in the campaign.
            categoryMenu.setOnShowing(e -> {
                if (!categoryMenu.getItems().isEmpty()) return;
                categoryMenu.getItems().add(buildEntryPicker(formatter, category, insert));
            });
            insert.getItems().add(categoryMenu);
        }
        return insert;
    }

    private CustomMenuItem buildEntryPicker(EntityInfoFormatter formatter, EntityCategory category, MenuButton owner) {
        List<Object> entries = new ArrayList<>(formatter.list(category));

        ListView<Object> list = new ListView<>();
        list.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : EntityInfoFormatter.nameOf(item));
            }
        });
        list.getItems().setAll(entries);
        list.setPrefHeight(entries.isEmpty() ? 60 : Math.min(280, 26 * entries.size() + 8));
        list.setPrefWidth(240);

        TextField filter = new TextField();
        filter.setPromptText("Filter...");
        filter.getStyleClass().add("dnd-text-field");
        filter.textProperty().addListener((obs, o, needle) -> {
            String lower = needle == null ? "" : needle.toLowerCase();
            List<Object> filtered = new ArrayList<>();
            for (Object entry : entries) {
                if (EntityInfoFormatter.nameOf(entry).toLowerCase().contains(lower)) filtered.add(entry);
            }
            list.getItems().setAll(filtered);
        });

        Runnable insertSelected = () -> {
            Object selected = list.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            insertAtCaret("\n" + formatter.format(category, selected));
            owner.hide();
        };
        list.setOnMouseClicked(e -> insertSelected.run());
        list.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) insertSelected.run(); });

        VBox box = new VBox(4, filter, list);
        box.setPadding(new Insets(4));
        if (entries.isEmpty()) {
            box.getChildren().setAll(new Label("No entries in this catalog yet."));
        }

        CustomMenuItem item = new CustomMenuItem(box);
        // Clicking inside the filter box or the list must not dismiss the menu.
        item.setHideOnClick(false);
        return item;
    }

    /** Ready-made scaffolding blocks for common session-prep structures. */
    private MenuButton buildInsertBlockMenu() {
        MenuButton blocks = new MenuButton("Insert Block ▾");
        blocks.getStyleClass().add("dnd-button");

        BiConsumer<String, String> add = (label, template) -> {
            MenuItem item = new MenuItem(label);
            item.setOnAction(e -> insertAtCaret(template));
            blocks.getItems().add(item);
        };

        add.accept("Scene", """

            === SCENE: <name> ===
            Location:
            Present:
            Goal:

            [READ ALOUD]
            <what the players see and hear>
            [/READ ALOUD]

            [DM NOTE]
            <what is really going on>
            [/DM NOTE]
            """);
        add.accept("Encounter", """

            === ENCOUNTER: <name> ===
            Enemies:
            Terrain / hazards:
            Tactics:
            Treasure:
            Scaling if the party struggles:
            """);
        add.accept("NPC beat", """

            --- NPC: <name> ---
            Wants:
            Secret:
            Voice / mannerism:
            Opening line:
            """);
        add.accept("Skill check", """

            [CHECK] <skill> DC <n>
              Success:
              Failure:
            """);
        add.accept("Loot", """

            [LOOT]
              Coins:
              Items:
            """);
        add.accept("Session recap", """

            === RECAP ===
            Previously:
            Open threads:
            Next session hooks:
            """);
        return blocks;
    }

    private Button toolButton(String label, String tooltip, Runnable action) {
        Button b = new Button(label);
        b.getStyleClass().add("dnd-button");
        b.setTooltip(new Tooltip(tooltip));
        b.setOnAction(e -> action.run());
        return b;
    }

    private Node buildStatusBar(Stage stage) {
        Button close = new Button("Close");
        close.getStyleClass().add("dnd-button");
        close.setOnAction(e -> {
            if (!dirty || confirmDiscard()) stage.close();
        });
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bar = new HBox(10, statusLabel, spacer, close);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(6, 10, 8, 10));
        return bar;
    }

    // ------------------------------------------------------------ text edits

    private void insertAtCaret(String text) {
        int caret = area.getCaretPosition();
        area.insertText(caret, text);
        area.positionCaret(caret + text.length());
        area.requestFocus();
    }

    /** Wraps the selection (or the caret position) in {@code open}/{@code close} markers. */
    private void wrapSelection(String open, String close) {
        String selected = area.getSelectedText();
        int start = area.getSelection().getStart();
        int end = area.getSelection().getEnd();
        String body = selected.isEmpty() ? "" : selected;
        String replacement = "\n" + open + "\n" + body + "\n" + close + "\n";
        area.replaceText(start, end, replacement);
        // Drop the caret onto the body line so the DM can just start typing.
        area.positionCaret(start + open.length() + 2 + body.length());
        area.requestFocus();
    }

    private void insertHeading() {
        int caret = area.getCaretPosition();
        int lineStart = area.getText().lastIndexOf('\n', Math.max(0, caret - 1)) + 1;
        area.insertText(lineStart, "== ");
        area.requestFocus();
    }

    /** Prefixes every selected line (or the current line) with {@code prefix}. */
    private void prefixLines(String prefix) {
        String text = area.getText();
        int selStart = area.getSelection().getStart();
        int selEnd = area.getSelection().getEnd();
        int start = text.lastIndexOf('\n', Math.max(0, selStart - 1)) + 1;
        int end = text.indexOf('\n', selEnd);
        if (end < 0) end = text.length();
        if (end < start) end = start;

        String[] lines = text.substring(start, end).split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) sb.append('\n');
            sb.append(lines[i].isBlank() ? lines[i] : prefix + lines[i]);
        }
        area.replaceText(start, end, sb.toString());
        area.requestFocus();
    }

    // ---------------------------------------------------------------- actions

    private void openFind() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Find");
        dialog.setHeaderText(null);
        dialog.setContentText("Find:");
        owner.styleDialog(dialog);
        dialog.showAndWait().ifPresent(needle -> {
            if (needle.isEmpty()) return;
            int from = area.getSelection().getEnd();
            int idx = area.getText().toLowerCase().indexOf(needle.toLowerCase(), from);
            if (idx < 0) idx = area.getText().toLowerCase().indexOf(needle.toLowerCase());
            if (idx < 0) {
                statusLabel.setText("No match for \"" + needle + "\".");
                return;
            }
            area.selectRange(idx, idx + needle.length());
            area.requestFocus();
            statusLabel.setText("Found \"" + needle + "\".");
        });
    }

    /** Opens a large-type window containing only the read-aloud passages of this file. */
    private void openPlayerView() {
        String readAloud = extractReadAloud(area.getText());
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Read to players - " + file.getFileName());

        TextArea view = new TextArea(readAloud.isBlank()
            ? "No read-aloud passages in this file yet.\n\nSelect some text and press \"Read-Aloud\" to mark it."
            : readAloud);
        view.setWrapText(true);
        view.setEditable(false);
        view.getStyleClass().addAll("storyline-editor", "player-view");
        VBox.setVgrow(view, Priority.ALWAYS);

        Button close = new Button("Close");
        close.getStyleClass().add("dnd-button");
        close.setOnAction(e -> stage.close());
        HBox buttons = new HBox(close);
        buttons.setPadding(new Insets(8, 10, 10, 10));

        VBox layout = new VBox(0, view, buttons);
        layout.getStyleClass().add("root");
        stage.setScene(owner.themedScene(layout, 800, 600));
        stage.setMaximized(true);
        stage.showAndWait();
    }

    /** Concatenates the contents of every {@code [READ ALOUD]...[/READ ALOUD]} block. */
    static String extractReadAloud(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        int cursor = 0;
        while (true) {
            int open = text.indexOf(READ_ALOUD_OPEN, cursor);
            if (open < 0) break;
            int bodyStart = open + READ_ALOUD_OPEN.length();
            int close = text.indexOf(READ_ALOUD_CLOSE, bodyStart);
            if (close < 0) {
                // Unclosed final block: take the rest of the file rather than dropping it.
                sb.append(text.substring(bodyStart).trim()).append("\n\n");
                break;
            }
            sb.append(text, bodyStart, close).append("\n\n");
            cursor = close + READ_ALOUD_CLOSE.length();
        }
        return sb.toString().trim();
    }

    private void save() {
        service.writeText(file, area.getText());
        markDirty(false);
        statusLabel.setText("Saved " + file.getFileName() + ".");
    }

    private void markDirty(boolean value) {
        dirty = value;
        if (value) statusLabel.setText("Unsaved changes - Ctrl+S to save.");
        else if (statusLabel.getText() == null || statusLabel.getText().isEmpty()) statusLabel.setText("");
    }

    private boolean confirmDiscard() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "You have unsaved changes. Close without saving?", ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText(null);
        owner.styleDialog(alert);
        return alert.showAndWait().filter(bt -> bt == ButtonType.OK).isPresent();
    }
}
