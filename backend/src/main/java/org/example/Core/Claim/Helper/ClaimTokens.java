package org.example.Core.Claim.Helper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/** Claim tokens and hashing. Only hashes are stored, so a database leak does not expose tokens or IPs. */
public final class ClaimTokens {
    private static final SecureRandom RANDOM = new SecureRandom();

    private ClaimTokens() {
    }

    /** 32 random bytes, URL-safe (43 characters). */
    public static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public static boolean looksLikeToken(String token) {
        return token != null && token.matches("^[A-Za-z0-9_-]{43}$");
    }
}
