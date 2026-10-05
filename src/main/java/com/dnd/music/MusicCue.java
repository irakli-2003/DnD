package com.dnd.music;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A music or sound cue written into a storyline file: {@code [music: Tavern]},
 * {@code [music: stop]} or {@code [sfx: Thunder]}. Double-clicking it in the editor plays it.
 * Names (not ids) are used so the files stay readable when printed or read aloud.
 */
public record MusicCue(Kind kind, String name, int start, int end) {

    public enum Kind { MUSIC, SFX }

    private static final Pattern PATTERN = Pattern.compile("\\[(music|sfx)\\s*:\\s*([^\\]]+)\\]", Pattern.CASE_INSENSITIVE);

    public boolean isStop() {
        return kind == Kind.MUSIC && "stop".equalsIgnoreCase(name);
    }

    public static String marker(Kind kind, String name) {
        String safe = name == null ? "" : name.replace("]", "").trim();
        return "[" + (kind == Kind.SFX ? "sfx" : "music") + ": " + safe + "]";
    }

    public static List<MusicCue> findAll(String text) {
        List<MusicCue> cues = new ArrayList<>();
        if (text == null || text.isEmpty()) return cues;
        Matcher m = PATTERN.matcher(text);
        while (m.find()) {
            Kind kind = m.group(1).equalsIgnoreCase("sfx") ? Kind.SFX : Kind.MUSIC;
            cues.add(new MusicCue(kind, m.group(2).trim(), m.start(), m.end()));
        }
        return cues;
    }

    public static MusicCue at(String text, int caret) {
        for (MusicCue cue : findAll(text)) {
            if (caret >= cue.start() && caret <= cue.end()) return cue;
        }
        return null;
    }
}
