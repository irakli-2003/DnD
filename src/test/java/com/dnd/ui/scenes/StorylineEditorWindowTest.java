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

    // ------------------------------------------------------------------ find

    private static final String HAY = "Alice met alice. ALICE smiled.";

    @Test
    public void findNextIsCaseInsensitiveByDefaultAndStartsAtFrom() {
        assertEquals(0, StorylineEditorWindow.findMatch(HAY, "alice", 0, true, false, true));
        assertEquals(10, StorylineEditorWindow.findMatch(HAY, "alice", 1, true, false, true));
        assertEquals(17, StorylineEditorWindow.findMatch(HAY, "alice", 11, true, false, true));
    }

    @Test
    public void findNextHonoursMatchCase() {
        assertEquals(10, StorylineEditorWindow.findMatch(HAY, "alice", 0, true, true, true));
        assertEquals(17, StorylineEditorWindow.findMatch(HAY, "ALICE", 0, true, true, true));
        assertEquals(-1, StorylineEditorWindow.findMatch(HAY, "aLiCe", 0, true, true, true));
    }

    @Test
    public void findNextWrapsAroundOnlyWhenAsked() {
        assertEquals(0, StorylineEditorWindow.findMatch(HAY, "alice", 18, true, false, true));
        assertEquals(-1, StorylineEditorWindow.findMatch(HAY, "alice", 18, true, false, false));
    }

    @Test
    public void findPreviousSearchesStrictlyBeforeFromAndWraps() {
        assertEquals(10, StorylineEditorWindow.findMatch(HAY, "alice", 17, false, false, true));
        assertEquals(0, StorylineEditorWindow.findMatch(HAY, "alice", 10, false, false, true));
        assertEquals(17, StorylineEditorWindow.findMatch(HAY, "alice", 0, false, false, true));
        assertEquals(-1, StorylineEditorWindow.findMatch(HAY, "alice", 0, false, false, false));
    }

    @Test
    public void findHandlesEmptyAndMissingInput() {
        assertEquals(-1, StorylineEditorWindow.findMatch(HAY, "", 0, true, false, true));
        assertEquals(-1, StorylineEditorWindow.findMatch(null, "a", 0, true, false, true));
        assertEquals(-1, StorylineEditorWindow.findMatch("ab", "abc", 0, true, false, true));
        assertEquals(-1, StorylineEditorWindow.findMatch(HAY, "dragon", 5, false, false, true));
        assertEquals(17, StorylineEditorWindow.findMatch(HAY, "alice", 999, false, false, true));
    }

    @Test
    public void findAllCountsNonOverlappingMatches() {
        assertEquals(java.util.List.of(0, 10, 17), StorylineEditorWindow.findAll(HAY, "alice", false));
        assertEquals(java.util.List.of(10), StorylineEditorWindow.findAll(HAY, "alice", true));
        assertEquals(java.util.List.of(0, 2), StorylineEditorWindow.findAll("aaaa", "aa", true));
        assertTrue(StorylineEditorWindow.findAll(HAY, "", false).isEmpty());
    }
}
