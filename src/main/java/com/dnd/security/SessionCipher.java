package com.dnd.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * AES-256-GCM encryption / decryption for session data shared over Firebase.
 *
 * <p>A random 12-byte IV and a fixed 16-byte salt are derived per encryption
 * call, so each ciphertext is unique even when the plaintext and passphrase are
 * identical.  The wire format is {@code Base64(salt || iv || ciphertext+tag)}.
 *
 * <p>The passphrase is stretched with PBKDF2-HMAC-SHA256 (310 000 iterations,
 * matching OWASP 2023 guidance for AES-256).
 */
public final class SessionCipher {

    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final int PBKDF2_ITERATIONS = 310_000;

    private static final SecureRandom RANDOM = new SecureRandom();

    private SessionCipher() {
    }

    /**
     * Encrypts {@code plaintext} with {@code passphrase}.
     *
     * @return Base64-encoded ciphertext (salt + IV + encrypted bytes + GCM tag)
     */
    public static String encrypt(String plaintext, String passphrase) {
        try {
            byte[] salt = new byte[SALT_BYTES];
            RANDOM.nextBytes(salt);

            byte[] iv = new byte[IV_BYTES];
            RANDOM.nextBytes(iv);

            SecretKey key = deriveKey(passphrase, salt);

            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] cipherBytes = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            ByteBuffer buf = ByteBuffer.allocate(SALT_BYTES + IV_BYTES + cipherBytes.length);
            buf.put(salt);
            buf.put(iv);
            buf.put(cipherBytes);
            return Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            throw new CipherException("Encryption failed", e);
        }
    }

    /**
     * Decrypts a Base64-encoded ciphertext produced by {@link #encrypt}.
     *
     * @return the original plaintext
     * @throws CipherException if decryption fails (wrong passphrase, tampered data, etc.)
     */
    public static String decrypt(String base64Ciphertext, String passphrase) {
        try {
            byte[] raw = Base64.getDecoder().decode(base64Ciphertext);
            ByteBuffer buf = ByteBuffer.wrap(raw);

            byte[] salt = new byte[SALT_BYTES];
            buf.get(salt);

            byte[] iv = new byte[IV_BYTES];
            buf.get(iv);

            byte[] cipherBytes = new byte[buf.remaining()];
            buf.get(cipherBytes);

            SecretKey key = deriveKey(passphrase, salt);

            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new CipherException("Decryption failed — wrong passphrase or corrupted data", e);
        }
    }

    private static SecretKey deriveKey(String passphrase, byte[] salt) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BITS);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    /** Unchecked wrapper for cipher errors so callers do not need checked exceptions. */
    public static final class CipherException extends RuntimeException {
        public CipherException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
