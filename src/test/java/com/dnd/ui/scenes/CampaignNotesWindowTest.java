package com.dnd.ui.scenes;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class CampaignNotesWindowTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void missingNotesReadAsEmpty() {
        assertEquals("", CampaignNotesWindow.readNotes(tmp.getRoot().toPath()));
    }

    @Test
    public void roundTripsUtf8WithoutBom() throws Exception {
        Path root = tmp.getRoot().toPath().resolve("campaign");
        String text = "Alice attention: 3/10\nქართული ✓";
        CampaignNotesWindow.writeNotes(root, text);

        byte[] bytes = Files.readAllBytes(root.resolve("notes.txt"));
        assertFalse(bytes.length >= 3 && bytes[0] == (byte) 0xEF && bytes[1] == (byte) 0xBB && bytes[2] == (byte) 0xBF);
        assertEquals(text, CampaignNotesWindow.readNotes(root));
    }

    @Test
    public void readStripsBomWrittenByOtherEditors() throws Exception {
        Path root = tmp.getRoot().toPath();
        Files.writeString(root.resolve("notes.txt"), "\uFEFFhello", StandardCharsets.UTF_8);
        assertEquals("hello", CampaignNotesWindow.readNotes(root));
    }
}
