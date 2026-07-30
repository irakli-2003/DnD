package com.dnd.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;

/**
 * Minimal salted-hash helper for player character passwords.
 *
 * <p>Passwords are never stored in plaintext: a random salt is generated per
 * character, and only {@code SHA-256(salt + password)} (hex-encoded) is
 * persisted. This is a lightweight, dependency-free scheme appropriate for a
 * single-player-facing hobby app - not a substitute for a production-grade
 * KDF (e.g. bcrypt/scrypt/argon2) if this project's threat model ever
 * changes.</p>
 */
public final class PasswordHasher {
    private static final String ALGORITHM = "SHA-256";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    /** Generates a new random salt, hex-encoded. */
    public static String generateSalt() {
        byte[] saltBytes = new byte[16];
        RANDOM.nextBytes(saltBytes);
        return toHex(saltBytes);
    }

    /** Hashes {@code password} with {@code salt}, returning a hex-encoded digest. */
    public static String hash(String password, String salt) {
        String combined = (salt == null ? "" : salt) + (password == null ? "" : password);
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashed = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            return toHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed to be available on every standard JDK.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** @return {@code true} if hashing {@code password} with {@code salt} equals {@code expectedHash}. */
    public static boolean matches(String password, String salt, String expectedHash) {
        if (expectedHash == null) {
            return false;
        }
        return hash(password, salt).equalsIgnoreCase(expectedHash);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format(Locale.ROOT, "%02x", b));
        }
        return sb.toString();
    }
}

