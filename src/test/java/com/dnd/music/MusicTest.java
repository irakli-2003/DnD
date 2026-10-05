package com.dnd.music;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.util.List;

import static org.junit.Assert.*;

public class MusicTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void parsesCommonVideoLinks() {
        assertEquals("dQw4w9WgXcQ", YouTubeLink.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ").videoId());
        assertEquals("dQw4w9WgXcQ", YouTubeLink.parse("youtube.com/watch?feature=share&v=dQw4w9WgXcQ").videoId());
        assertEquals("dQw4w9WgXcQ", YouTubeLink.parse("https://youtu.be/dQw4w9WgXcQ?si=abc").videoId());
        assertEquals("dQw4w9WgXcQ", YouTubeLink.parse("https://www.youtube.com/shorts/dQw4w9WgXcQ").videoId());
        assertEquals("dQw4w9WgXcQ", YouTubeLink.parse("https://www.youtube.com/embed/dQw4w9WgXcQ").videoId());
        assertEquals("dQw4w9WgXcQ", YouTubeLink.parse("https://music.youtube.com/watch?v=dQw4w9WgXcQ").videoId());
        assertEquals("dQw4w9WgXcQ", YouTubeLink.parse("dQw4w9WgXcQ").videoId());
        assertFalse(YouTubeLink.parse("https://youtu.be/dQw4w9WgXcQ").isPlaylist());
    }

    @Test
    public void parsesPlaylistsAndStartTimes() {
        YouTubeLink list = YouTubeLink.parse("https://www.youtube.com/playlist?list=PLx0sYbCqOb8TBPRdmBHs5Iftvv9TPboYG");
        assertTrue(list.isPlaylist());
        assertNull(list.videoId());
        YouTubeLink inList = YouTubeLink.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PLx0sYbCqOb8TBPRdmBHs5Iftvv9TPboYG");
        assertTrue(inList.isPlaylist());
        assertEquals(90, YouTubeLink.parse("https://youtu.be/dQw4w9WgXcQ?t=90").startSeconds());
        assertEquals(90, YouTubeLink.parse("https://youtu.be/dQw4w9WgXcQ?t=1m30s").startSeconds());
        assertEquals(3725, YouTubeLink.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=1h2m5s").startSeconds());
    }

    @Test
    public void rejectsNonYouTubeLinks() {
        assertNull(YouTubeLink.parse(""));
        assertNull(YouTubeLink.parse("https://vimeo.com/123456"));
        assertNull(YouTubeLink.parse("https://www.youtube.com/watch?v=short"));
        assertNull(YouTubeLink.parse("https://www.youtube.com/"));
        assertNull(YouTubeLink.parse("not a link at all"));
    }

    @Test
    public void findsCuesInSessionText() {
        String text = "The door opens. [music: Boss Fight]\nThunder! [SFX: thunder] ... [music: stop]";
        List<MusicCue> cues = MusicCue.findAll(text);
        assertEquals(3, cues.size());
        assertEquals("Boss Fight", cues.get(0).name());
        assertEquals(MusicCue.Kind.SFX, cues.get(1).kind());
        assertTrue(cues.get(2).isStop());
        assertEquals("thunder", MusicCue.at(text, text.indexOf("thunder") + 2).name());
        assertNull(MusicCue.at(text, 2));
        assertEquals("[music: Tavern]", MusicCue.marker(MusicCue.Kind.MUSIC, "Tavern"));
        assertEquals("[sfx: Door creak]", MusicCue.marker(MusicCue.Kind.SFX, "Door] creak"));
    }

    @Test
    public void libraryRoundTripsAndFindsByName() {
        MusicLibrary library = new MusicLibrary();
        MusicLibrary.Playlist tavern = new MusicLibrary.Playlist("Tavern");
        tavern.getTracks().add(new MusicLibrary.Track("Inn", "https://youtu.be/dQw4w9WgXcQ"));
        MusicLibrary.Playlist battle = new MusicLibrary.Playlist("Battle");
        library.getPlaylists().add(tavern);
        library.getPlaylists().add(battle);
        library.getSounds().add(new MusicLibrary.Sound("Thunder", "https://youtu.be/dQw4w9WgXcQ", 3, 8));
        library.setCombatPlaylistId(battle.getId());
        library.setVolume(150);
        library.save(tmp.getRoot().toPath());

        MusicLibrary loaded = MusicLibrary.load(tmp.getRoot().toPath());
        assertEquals(100, loaded.getVolume());
        assertEquals("Battle", loaded.findPlaylist(loaded.getCombatPlaylistId()).getName());
        assertEquals("Tavern", loaded.findPlaylist("tavern").getName());
        assertEquals(1, loaded.findPlaylist("Tavern").getTracks().size());
        assertEquals(8, loaded.findSound("THUNDER").getEndSeconds());
        assertEquals(List.of("Battle", "Tavern"), loaded.sortedPlaylists().stream().map(MusicLibrary.Playlist::getName).toList());
    }

    @Test
    public void missingFileGivesStarterLibrary() {
        MusicLibrary library = MusicLibrary.load(tmp.getRoot().toPath());
        assertTrue(library.isLoop());
        assertNotNull(library.findPlaylist("Tavern"));
        assertEquals("Combat", library.findPlaylist(library.getCombatPlaylistId()).getName());
        assertTrue(library.findSound("River").isAmbience());
        assertFalse(library.findSound("Thunder clap").isAmbience());
    }

    @Test
    public void starterPackLinksAreAllSingleYouTubeVideos() {
        MusicLibrary starter = MusicLibrary.starter();
        assertTrue(starter.getPlaylists().size() >= 10);
        assertTrue(starter.getSounds().size() >= 20);
        java.util.Set<String> ids = new java.util.HashSet<>();
        for (MusicLibrary.Playlist p : starter.getPlaylists()) {
            assertTrue(p.getName(), ids.add(p.getId()));
            assertFalse(p.getName(), p.getTracks().isEmpty());
            for (MusicLibrary.Track t : p.getTracks()) {
                assertNotNull(t.getUrl(), t.link());
                assertNotNull(t.getUrl(), t.link().videoId());
                assertFalse(t.getTitle().isBlank());
            }
        }
        for (MusicLibrary.Sound s : starter.getSounds()) {
            assertTrue(s.getName(), ids.add(s.getId()));
            assertNotNull(s.getUrl(), s.link().videoId());
        }
    }

    @Test
    public void starterPackMergesWithoutDuplicates() {
        MusicLibrary library = new MusicLibrary();
        MusicLibrary.Playlist mine = new MusicLibrary.Playlist("tavern");
        library.getPlaylists().add(mine);
        library.getSounds().add(new MusicLibrary.Sound("River", "https://youtu.be/dQw4w9WgXcQ", 0, 0));

        int added = library.addStarterPack();
        int total = MusicLibrary.starter().getPlaylists().size() + MusicLibrary.starter().getSounds().size();
        assertEquals(total - 2, added);
        assertSame(mine, library.findPlaylist("Tavern"));
        assertEquals("https://youtu.be/dQw4w9WgXcQ", library.findSound("river").getUrl());
        assertEquals("Combat", library.findPlaylist(library.getCombatPlaylistId()).getName());
        assertEquals(0, library.addStarterPack());
    }

    @Test
    public void sfxStopCueIsAStop() {
        MusicCue cue = MusicCue.findAll("[sfx: stop]").get(0);
        assertEquals(MusicCue.Kind.SFX, cue.kind());
        assertTrue(cue.isStop());
    }
}
