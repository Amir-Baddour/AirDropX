package org.example.Core.User.Helper;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Password hashing with PBKDF2-HMAC-SHA256 from the JDK (no extra dependency).
 *
 * Stored format: {@code pbkdf2_sha256$<iterations>$<salt b64>$<hash b64>}. The iteration count is stored with
 * the hash, so it can be raised later without breaking existing passwords.
 */
public final class PasswordHasher {
    private static final String PREFIX = "pbkdf2_sha256";
    private static final int ITERATIONS = 600_000; // OWASP guidance for PBKDF2-HMAC-SHA256
    private static final int SALT_BYTES = 16;
    private static final int HASH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordHasher() {
    }

    public static String hash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(password, salt, ITERATIONS);
        return PREFIX + "$" + ITERATIONS + "$" + b64(salt) + "$" + b64(hash);
    }

    /** Constant-time check. Returns false for a null or malformed stored value instead of throwing. */
    public static boolean verify(String password, String stored) {
        if (password == null || stored == null) {
            return false;
        }
        String[] parts = stored.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            if (iterations < 1) {
                return false;
            }
            byte[] actual = derive(password, salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** A valid hash of a random password, used to keep login timing the same when the email is unknown. */
    public static String dummyHash() {
        return DummyHolder.VALUE;
    }

    private static final class DummyHolder {
        static final String VALUE = hash("dummy-password-for-timing-only");
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, HASH_BITS);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Password hashing is not available", e);
        }
    }

    private static String b64(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }
}
