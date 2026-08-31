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

    @Test
    public void countWordsIgnoresBlankAndCollapsesWhitespace() {
        assertEquals(0, StorylineEditorWindow.countWords(null));
        assertEquals(0, StorylineEditorWindow.countWords("   \n  "));
        assertEquals(3, StorylineEditorWindow.countWords("  one   two\nthree "));
    }

    @Test
    public void speakingMinutesSwitchesFromSecondsToMinutes() {
        assertEquals("28s", StorylineEditorWindow.speakingMinutes(60));
        assertEquals("1m 0s", StorylineEditorWindow.speakingMinutes(130));
        assertEquals("2m 0s", StorylineEditorWindow.speakingMinutes(260));
    }

    @Test
    public void statsReportTotalAndReadAloudSeparately() {
        String text = "prep notes here\n[READ ALOUD]\nYou stand before the gate.\n[/READ ALOUD]";
        String stats = StorylineEditorWindow.describeStats(text);
        assertTrue(stats.startsWith("8 words"));
        assertTrue(stats.contains("read-aloud: 5 words"));
    }

    @Test
    public void statsOmitReadAloudSectionWhenThereIsNone() {
        assertEquals("0 words", StorylineEditorWindow.describeStats(""));
        assertFalse(StorylineEditorWindow.describeStats("just notes").contains("read-aloud"));
    }
}
