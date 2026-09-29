package org.example.Core.Claim;

import org.example.Core.Claim.Helper.ClaimTokens;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaimTokensTest {

    @Test
    void tokensAreUniqueAndWellFormed() {
        String a = ClaimTokens.newToken();
        String b = ClaimTokens.newToken();
        assertNotEquals(a, b);
        assertTrue(ClaimTokens.looksLikeToken(a));
        assertEquals(43, a.length());
    }

    @Test
    void rejectsMalformedTokens() {
        assertFalse(ClaimTokens.looksLikeToken(null));
        assertFalse(ClaimTokens.looksLikeToken("short"));
        assertFalse(ClaimTokens.looksLikeToken("' OR 1=1 --' OR 1=1 --' OR 1=1 --' OR 1=1 "));
    }

    @Test
    void sha256IsHex64() {
        String h = ClaimTokens.sha256("hello");
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", h);
    }
}
