package com.dnd.security;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thin client for syncing encrypted session state to Firebase Realtime Database.
 *
 * <h2>How it works</h2>
 * <ol>
 *   <li>The DM calls {@link #push} to encrypt and write the current session
 *       snapshot to {@code <databaseUrl>/sessions/<sessionId>.json}.</li>
 *   <li>Players call {@link #pull} to fetch the ciphertext and decrypt it with
 *       the shared passphrase.</li>
 *   <li>No Firebase SDK is required — only the standard {@code java.net.http}
 *       client (Java 11+) and the Firebase Realtime Database REST API are used.</li>
 * </ol>
 *
 * <h2>Session token format</h2>
 * The "session token" shared between DM and players encodes everything needed
 * to connect:
 * <pre>
 *   Base64( databaseUrl + "|" + sessionId + "|" + passphrase )
 * </pre>
 * The DM generates and shares this token; players paste it into the CLI.
 *
 * <h2>Firebase setup (one-time, ~5 minutes)</h2>
 * <ol>
 *   <li>Create a project at <a href="https://console.firebase.google.com">console.firebase.google.com</a>.</li>
 *   <li>Add a Realtime Database (start in <b>test mode</b> for development;
 *       lock down rules before production).</li>
 *   <li>Copy the database URL (e.g. {@code https://my-campaign-default-rtdb.firebaseio.com}).</li>
 *   <li>Pass that URL to {@link #generateToken} when starting a session.</li>
 * </ol>
 *
 * <h2>Recommended Firebase security rules</h2>
 * <pre>
 * {
 *   "rules": {
 *     "sessions": {
 *       "$sessionId": {
 *         ".read":  true,
 *         ".write": true
 *       }
 *     }
 *   }
 * }
 * </pre>
 * The data is already AES-256-GCM encrypted before it is written, so open read
 * access is safe — only holders of the passphrase can decrypt it.
 */
public final class FirebaseSessionSync {

    private static final Logger LOGGER = Logger.getLogger(FirebaseSessionSync.class.getName());
    private static final Duration TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient http;
    private final String databaseUrl;   // e.g. https://my-campaign-default-rtdb.firebaseio.com
    private final String sessionId;     // node name under /sessions/
    private final String passphrase;    // shared secret used by SessionCipher

    /** Latest decrypted snapshot received from the SSE stream. {@code null} until first event. */
    private final AtomicReference<String> latestSnapshot = new AtomicReference<>(null);
    /** Background SSE listener thread; {@code null} when not listening. */
    private volatile Thread listenerThread;

    public FirebaseSessionSync(String databaseUrl, String sessionId, String passphrase) {
        this.databaseUrl = databaseUrl.replaceAll("/$", "");
        this.sessionId = sessionId;
        this.passphrase = passphrase;
        this.http = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Encrypts {@code plainJson} and writes it to Firebase.
     *
     * @param plainJson raw (unencrypted) JSON to persist
     * @throws SyncException on network or HTTP errors
     */
    public void push(String plainJson) {
        String ciphertext = SessionCipher.encrypt(plainJson, passphrase);
        String body = "\"" + ciphertext + "\"";   // Firebase expects a JSON value
        String url = nodeUrl();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(TIMEOUT)
                .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new SyncException("Firebase PUT failed with HTTP " + response.statusCode()
                    + ": " + response.body());
        }
        LOGGER.fine("Session pushed to Firebase [" + sessionId + "]");
    }

    /**
     * Fetches the session snapshot from Firebase and decrypts it.
     *
     * @return the original plaintext JSON, or {@code null} if no data exists yet
     * @throws SyncException on network or HTTP errors
     * @throws SessionCipher.CipherException if decryption fails (wrong passphrase / corrupt data)
     */
    public String pull() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(nodeUrl()))
                .timeout(TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new SyncException("Firebase GET failed with HTTP " + response.statusCode()
                    + ": " + response.body());
        }

        String body = response.body().trim();
        if ("null".equals(body) || body.isEmpty()) {
            return null;   // session not yet started
        }

        // Firebase wraps string values in JSON quotes — strip them
        String ciphertext = stripJsonString(body);
        return SessionCipher.decrypt(ciphertext, passphrase);
    }

    /**
     * Deletes the session node from Firebase (call when the session ends).
     *
     * @throws SyncException on network or HTTP errors
     */
    public void delete() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(nodeUrl()))
                .timeout(TIMEOUT)
                .DELETE()
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new SyncException("Firebase DELETE failed with HTTP " + response.statusCode()
                    + ": " + response.body());
        }
        LOGGER.fine("Session deleted from Firebase [" + sessionId + "]");
    }

    // -------------------------------------------------------------------------
    // SSE streaming — push-based, zero-poll live updates
    // -------------------------------------------------------------------------

    /**
     * Opens a persistent Server-Sent Events connection to Firebase and fires
     * {@code onUpdate} with the decrypted plaintext JSON every time the DM
     * pushes new state.  The connection runs in a background daemon thread so
     * it never blocks the CLI.
     *
     * <p>Firebase SSE wire format (one logical event is two lines):</p>
     * <pre>
     * event: put
     * data: {"path":"/","data":"&lt;ciphertext&gt;"}
     * </pre>
     *
     * <p>Only {@code put} events that carry real data are forwarded; keep-alive
     * comments ({@code :}) and {@code cancel} events are silently ignored.</p>
     *
     * <p>If the connection drops, the thread attempts a reconnect after 5 s and
     * keeps retrying until {@link #stopListening()} is called.</p>
     *
     * @param onUpdate called on the listener thread with each new decrypted snapshot
     */
    public void startListening(Consumer<String> onUpdate) {
        stopListening();   // ensure no stale thread

        listenerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    connectAndStream(onUpdate);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    LOGGER.log(Level.FINE, "SSE connection lost, reconnecting in 5 s", e);
                }
                // Back-off before reconnect — skip if interrupted
                try {
                    Thread.sleep(5_000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            LOGGER.fine("SSE listener stopped [" + sessionId + "]");
        }, "firebase-sse-" + sessionId);

        listenerThread.setDaemon(true);
        listenerThread.start();
        LOGGER.fine("SSE listener started [" + sessionId + "]");
    }

    /** Stops the background SSE listener thread if one is running. */
    public void stopListening() {
        Thread t = listenerThread;
        if (t != null) {
            t.interrupt();
            listenerThread = null;
        }
    }

    /**
     * Returns the last decrypted snapshot received via the SSE stream, or
     * {@code null} if no event has arrived yet.  Never makes a network call.
     */
    public String getLatestSnapshot() {
        return latestSnapshot.get();
    }

    /** Node name this session lives under, used to name the player's local cache of it. */
    public String getSessionId() {
        return sessionId;
    }

    /** Blocks until the SSE stream closes or the thread is interrupted. */
    private void connectAndStream(Consumer<String> onUpdate) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(nodeUrl()))
                .header("Accept", "text/event-stream")
                .header("Cache-Control", "no-cache")
                .GET()
                .build();

        // Use an InputStream body handler so we can read line-by-line without buffering the whole response
        HttpResponse<InputStream> response = http.send(
                request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new SyncException("SSE connect failed with HTTP " + response.statusCode());
        }

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {

            String eventType = null;
            String dataLine = null;

            String line;
            while ((line = reader.readLine()) != null) {
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("SSE listener interrupted");
                }

                if (line.startsWith("event:")) {
                    eventType = line.substring("event:".length()).trim();
                } else if (line.startsWith("data:")) {
                    dataLine = line.substring("data:".length()).trim();
                } else if (line.isEmpty()) {
                    // blank line = end of one SSE event, dispatch it
                    if ("put".equals(eventType) && dataLine != null) {
                        handlePutEvent(dataLine, onUpdate);
                    }
                    eventType = null;
                    dataLine = null;
                }
                // lines starting with ':' are keep-alive comments — ignore
            }
        }
    }

    /**
     * Parses a Firebase {@code put} event data payload and fires {@code onUpdate}
     * if the payload contains real (non-null) data.
     *
     * <p>Firebase wraps the stored value in {@code {"path":"/","data":"<value>"}}.
     * We extract the {@code data} field and decrypt it.</p>
     */
    private void handlePutEvent(String eventData, Consumer<String> onUpdate) {
        try {
            // Minimal JSON extraction — avoid pulling in a full parser dependency
            // just for this one field. Looks for: "data":"..."
            int dataIdx = eventData.indexOf("\"data\":");
            if (dataIdx < 0) {
                return;
            }
            String afterKey = eventData.substring(dataIdx + "\"data\":".length()).trim();

            if (afterKey.startsWith("null")) {
                return;   // session deleted or not yet started
            }

            String ciphertext = stripJsonString(afterKey.endsWith("}")
                    ? afterKey.substring(0, afterKey.lastIndexOf('}') < afterKey.length() - 1
                            ? afterKey.lastIndexOf('}') : afterKey.length()).trim()
                    : afterKey);

            // Strip trailing brace that belongs to the outer wrapper object
            if (ciphertext.endsWith("}")) {
                ciphertext = ciphertext.substring(0, ciphertext.length() - 1).trim();
            }
            // Re-strip quotes in case stripJsonString didn't fire (no surrounding quotes)
            ciphertext = stripJsonString(ciphertext);

            String plaintext = SessionCipher.decrypt(ciphertext, passphrase);
            latestSnapshot.set(plaintext);
            onUpdate.accept(plaintext);
        } catch (SessionCipher.CipherException e) {
            LOGGER.log(Level.WARNING, "Could not decrypt SSE payload — passphrase mismatch?", e);
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Could not parse SSE event data", e);
        }
    }

    // -------------------------------------------------------------------------
    // -------------------------------------------------------------------------

    /**
     * Generates a shareable session token encoding the database URL, session ID,
     * and shared passphrase.  The DM shares this string with all players.
     *
     * <p>Token format: {@code Base64(databaseUrl + "|" + sessionId + "|" + passphrase)}
     */
    public static String generateToken(String databaseUrl, String sessionId, String passphrase) {
        String raw = databaseUrl + "|" + sessionId + "|" + passphrase;
        return Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Decodes a token produced by {@link #generateToken} into a ready-to-use
     * {@link FirebaseSessionSync} instance.
     *
     * @throws IllegalArgumentException if the token is malformed
     */
    public static FirebaseSessionSync fromToken(String token) {
        try {
            String raw = new String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|", 3);
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid session token format");
            }
            return new FirebaseSessionSync(parts[0], parts[1], parts[2]);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not decode session token", e);
        }
    }

    /**
     * Generates a random alphanumeric session ID (16 characters).
     * Suitable for use as the {@code sessionId} parameter.
     */
    public static String generateSessionId() {
        byte[] bytes = new byte[12];
        new java.security.SecureRandom().nextBytes(bytes);
        // hex-encode to get a URL-safe alphanumeric string
        return HexFormat.of().formatHex(bytes);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private String nodeUrl() {
        return databaseUrl + "/sessions/" + sessionId + ".json";
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new SyncException("Network error communicating with Firebase", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SyncException("Firebase request interrupted", e);
        }
    }

    /**
     * Strips surrounding JSON string quotes from a Firebase response.
     * Firebase returns string scalars as {@code "value"} (with quotes).
     */
    private static String stripJsonString(String json) {
        if (json.length() >= 2 && json.charAt(0) == '"' && json.charAt(json.length() - 1) == '"') {
            // unescape basic JSON escapes produced by Firebase
            return json.substring(1, json.length() - 1)
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t");
        }
        return json;
    }

    /** Unchecked wrapper for network / HTTP errors. */
    public static final class SyncException extends RuntimeException {
        public SyncException(String message) {
            super(message);
        }
        public SyncException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
