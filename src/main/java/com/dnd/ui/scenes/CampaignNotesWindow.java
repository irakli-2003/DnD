package com.dnd.ui.scenes;

import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Small plain-text scratchpad shared by every session file of a campaign (stored at
 * {@code <campaignRoot>/notes.txt}). Used for running state the DM tracks across a whole
 * session - counters, meters, who owes whom - without cluttering the storyline files.
 * Autosaves shortly after typing stops and when the window closes.
 */
final class CampaignNotesWindow {

    static final String FILE_NAME = "notes.txt";
    private static final Map<Path, Stage> OPEN = new HashMap<>();

    private CampaignNotesWindow() {}

    static Path notesFile(Path campaignRoot) {
        return campaignRoot.resolve(FILE_NAME);
    }

    /** Reads the notes as UTF-8, tolerating a missing file and a stray BOM from external editors. */
    static String readNotes(Path campaignRoot) {
        Path file = notesFile(campaignRoot);
        if (!Files.exists(file)) return "";
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            return text.startsWith("\uFEFF") ? text.substring(1) : text;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Writes the notes as UTF-8 without a BOM, creating the campaign folder if needed. */
    static void writeNotes(Path campaignRoot, String text) {
        Path file = notesFile(campaignRoot);
        try {
            Files.createDirectories(campaignRoot);
            Files.writeString(file, text == null ? "" : text, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Opens the campaign's notes window, or focuses it if it is already open. */
    static void open(BaseScene owner, Window ownerWindow, Path campaignRoot) {
        Path key = campaignRoot.toAbsolutePath().normalize();
        Stage existing = OPEN.get(key);
        if (existing != null) {
            existing.show();
            existing.toFront();
            existing.requestFocus();
            return;
        }

        Stage stage = new Stage();
        if (ownerWindow != null) stage.initOwner(ownerWindow);
        stage.initModality(Modality.NONE);
        stage.setTitle("Campaign Notes");
        stage.setResizable(true);

        TextArea area = new TextArea(readNotes(campaignRoot));
        area.setWrapText(true);
        area.getStyleClass().add("storyline-editor");
        area.setPromptText("Campaign-wide scratch notes shared by every session file");
        VBox.setVgrow(area, Priority.ALWAYS);

        Label status = new Label("Saved");
        status.getStyleClass().add("body-label");

        boolean[] pending = {false};
        Runnable save = () -> {
            if (!pending[0]) return;
            try {
                writeNotes(campaignRoot, area.getText());
                pending[0] = false;
                status.setText("Saved");
            } catch (UncheckedIOException ex) {
                status.setText("Save failed: " + ex.getCause().getMessage());
            }
        };
        PauseTransition debounce = new PauseTransition(Duration.seconds(1));
        debounce.setOnFinished(e -> save.run());
        area.textProperty().addListener((obs, o, n) -> {
            pending[0] = true;
            status.setText("Editing...");
            debounce.playFromStart();
        });

        HBox bar = new HBox(status);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(4, 8, 6, 8));
        VBox layout = new VBox(0, area, bar);
        layout.getStyleClass().add("root");

        stage.setScene(owner.themedScene(layout, 420, 520));
        // onHidden also fires when the owning editor closes and takes this window with it.
        stage.setOnHidden(e -> {
            debounce.stop();
            save.run();
            OPEN.remove(key);
        });
        OPEN.put(key, stage);
        stage.show();
        area.requestFocus();
        area.positionCaret(area.getLength());
    }
}
