package com.dnd.music;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A parsed YouTube link: a single video, a playlist, or a video inside a playlist.
 * Accepts the usual shapes people paste - {@code youtube.com/watch?v=}, {@code youtu.be/},
 * {@code /shorts/}, {@code /embed/}, {@code /live/}, {@code music.youtube.com}, playlist
 * links with {@code list=}, and start times ({@code t=90}, {@code t=1m30s}, {@code start=90}).
 */
public record YouTubeLink(String videoId, String playlistId, int startSeconds) {

    private static final Pattern VIDEO_ID = Pattern.compile("[A-Za-z0-9_-]{11}");
    private static final Pattern LIST_ID = Pattern.compile("[A-Za-z0-9_-]{10,}");
    private static final Pattern TIME = Pattern.compile("(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s?)?");

    public boolean isPlaylist() {
        return playlistId != null;
    }

    /** Parses a pasted link; returns null when it isn't a recognisable YouTube video or playlist. */
    public static YouTubeLink parse(String text) {
        if (text == null) return null;
        String raw = text.trim();
        if (raw.isEmpty()) return null;
        if (VIDEO_ID.matcher(raw).matches()) return new YouTubeLink(raw, null, 0);
        if (!raw.contains("://")) raw = "https://" + raw;
        URI uri;
        try {
            uri = URI.create(raw.replace(" ", "%20"));
        } catch (IllegalArgumentException e) {
            return null;
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();
        if (!(host.endsWith("youtube.com") || host.endsWith("youtu.be") || host.endsWith("youtube-nocookie.com"))) {
            return null;
        }
        String path = uri.getRawPath() == null ? "" : uri.getRawPath();
        String video = null;
        if (host.endsWith("youtu.be")) {
            video = firstSegment(path);
        } else {
            String query = param(uri, "v");
            if (query != null) video = query;
            for (String prefix : new String[]{"/shorts/", "/embed/", "/live/", "/v/"}) {
                if (path.startsWith(prefix)) video = firstSegment(path.substring(prefix.length() - 1));
            }
        }
        if (video != null && !VIDEO_ID.matcher(video).matches()) video = null;
        String list = param(uri, "list");
        if (list != null && !LIST_ID.matcher(list).matches()) list = null;
        if (video == null && list == null) return null;
        String time = param(uri, "t");
        if (time == null) time = param(uri, "start");
        return new YouTubeLink(video, list, parseTime(time));
    }

    private static String firstSegment(String path) {
        String trimmed = path.startsWith("/") ? path.substring(1) : path;
        int slash = trimmed.indexOf('/');
        return slash < 0 ? trimmed : trimmed.substring(0, slash);
    }

    private static String param(URI uri, String name) {
        String query = uri.getRawQuery();
        if (query == null) return null;
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            String key = eq < 0 ? pair : pair.substring(0, eq);
            if (key.equals(name)) {
                return eq < 0 ? "" : URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    static int parseTime(String text) {
        if (text == null || text.isBlank()) return 0;
        Matcher m = TIME.matcher(text.trim().toLowerCase());
        if (!m.matches()) return 0;
        int h = m.group(1) == null ? 0 : Integer.parseInt(m.group(1));
        int min = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
        int s = m.group(3) == null ? 0 : Integer.parseInt(m.group(3));
        return h * 3600 + min * 60 + s;
    }
}
