package com.project.coursework2.util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Password hashing with salted PBKDF2-HMAC-SHA256.
 *
 * <p>Stored format: {@code pbkdf2$<iterations>$<base64 salt>$<base64 hash>}. Every hash gets a
 * fresh 16-byte random salt, so two users with the same password have different hashes, and
 * comparisons are constant-time.</p>
 *
 * <p>Earlier versions stored unsalted SHA-256 hex digests. {@link #verify} still accepts those,
 * and {@link #needsRehash} reports them so the login code can upgrade the stored hash the next
 * time the user signs in successfully.</p>
 */
public final class PasswordUtils {

    /** OWASP Password Storage Cheat Sheet recommendation for PBKDF2-HMAC-SHA256. */
    static final int ITERATIONS = 600_000;

    private static final String PREFIX = "pbkdf2";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtils() {}

    /** Hashes a plaintext password with a new random salt. */
    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] key = pbkdf2(password, salt, ITERATIONS);
        Base64.Encoder b64 = Base64.getEncoder();
        return PREFIX + "$" + ITERATIONS + "$" + b64.encodeToString(salt) + "$" + b64.encodeToString(key);
    }

    /** Returns true if {@code password} matches the stored hash (PBKDF2 or legacy SHA-256). */
    public static boolean verify(String password, String stored) {
        if (password == null || stored == null) {
            return false;
        }
        if (stored.startsWith(PREFIX + "$")) {
            String[] parts = stored.split("\\$");
            if (parts.length != 4) {
                return false;
            }
            try {
                int iterations = Integer.parseInt(parts[1]);
                byte[] salt = Base64.getDecoder().decode(parts[2]);
                byte[] expected = Base64.getDecoder().decode(parts[3]);
                return MessageDigest.isEqual(expected, pbkdf2(password, salt, iterations));
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        if (isLegacySha256(stored)) {
            byte[] expected = HexFormat.of().parseHex(stored);
            return MessageDigest.isEqual(expected, sha256(password));
        }
        return false;
    }

    /** True when the stored value should be replaced with a fresh hash after a successful login. */
    public static boolean needsRehash(String stored) {
        if (stored == null || !stored.startsWith(PREFIX + "$")) {
            return true;
        }
        String[] parts = stored.split("\\$");
        try {
            return parts.length != 4 || Integer.parseInt(parts[1]) < ITERATIONS;
        } catch (NumberFormatException e) {
            return true;
        }
    }

    private static boolean isLegacySha256(String stored) {
        return stored.length() == 64 && stored.chars().allMatch(c -> Character.digit(c, 16) >= 0);
    }

    private static byte[] pbkdf2(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(ALGORITHM + " is not available", e);
        } finally {
            spec.clearPassword();
        }
    }

    private static byte[] sha256(String password) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
