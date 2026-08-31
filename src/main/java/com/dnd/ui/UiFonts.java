package com.dnd.ui;

import javafx.scene.text.Font;

/**
 * Registers the bundled DejaVu fonts with the JavaFX font system.
 *
 * <p>The stock UI fonts (Georgia, Consolas, Courier New) contain no Georgian
 * glyphs, so campaigns written in Georgian rendered as missing-glyph markers.
 * DejaVu Serif and DejaVu Sans Mono both cover the Georgian block, and DejaVu
 * Sans Mono is genuinely fixed-pitch, which keeps the ASCII tables and dot
 * leaders in storyline files aligned.
 */
public final class UiFonts {

    private static final String FONT_DIR = "/com/dnd/ui/fonts/";
    private static final String[] FONT_FILES = {
            "DejaVuSerif.ttf",
            "DejaVuSerif-Bold.ttf",
            "DejaVuSansMono.ttf",
            "DejaVuSansMono-Bold.ttf"
    };

    private static boolean loaded;

    private UiFonts() {
    }

    /** Loads the bundled fonts once. Must run before any stylesheet is applied. */
    public static synchronized void load() {
        if (loaded) return;
        loaded = true;
        for (String file : FONT_FILES) {
            try (var in = UiFonts.class.getResourceAsStream(FONT_DIR + file)) {
                if (in == null) {
                    System.err.println("Bundled font missing from resources: " + file);
                    continue;
                }
                if (Font.loadFont(in, 12) == null) {
                    System.err.println("JavaFX could not load bundled font: " + file);
                }
            } catch (Exception e) {
                System.err.println("Failed to load bundled font " + file + ": " + e.getMessage());
            }
        }
    }
}
