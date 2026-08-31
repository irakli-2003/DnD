package com.dnd.ui.scenes;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Covers the read-aloud extraction used by the editor's "Player View", which is the
 * feature that lets a DM keep private notes and player-facing prose in one file.
 */
public class StorylineEditorWindowTest {

    @Test
    public void extractsNothingFromTextWithoutMarkers() {
        assertEquals("", StorylineEditorWindow.extractReadAloud("just some DM notes"));
        assertEquals("", StorylineEditorWindow.extractReadAloud(null));
    }

    @Test
    public void extractsSingleReadAloudBlockWithoutMarkers() {
        String text = "notes\n[READ ALOUD]\nThe door creaks open.\n[/READ ALOUD]\nmore notes";
        assertEquals("The door creaks open.", StorylineEditorWindow.extractReadAloud(text));
    }

    @Test
    public void concatenatesMultipleBlocksAndSkipsDmNotes() {
        String text = """
            [DM NOTE]
            The lever is a trap.
            [/DM NOTE]
            [READ ALOUD]
            First passage.
            [/READ ALOUD]
            some prep notes
            [READ ALOUD]
            Second passage.
            [/READ ALOUD]
            """;
        String result = StorylineEditorWindow.extractReadAloud(text);
        assertTrue(result.contains("First passage."));
        assertTrue(result.contains("Second passage."));
        assertFalse(result.contains("The lever is a trap."));
        assertFalse(result.contains("some prep notes"));
    }

    @Test
    public void unclosedFinalBlockStillYieldsItsText() {
        String text = "[READ ALOUD]\nA half-written passage.";
        assertEquals("A half-written passage.", StorylineEditorWindow.extractReadAloud(text));
    }
}
