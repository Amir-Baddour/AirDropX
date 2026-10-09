package org.example.Core.User.Helper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    @Test
    void acceptsTheRightPasswordAndRejectsAWrongOne() {
        String stored = PasswordHasher.hash("Secret123");
        assertTrue(PasswordHasher.verify("Secret123", stored));
        assertFalse(PasswordHasher.verify("secret123", stored));
        assertFalse(PasswordHasher.verify("", stored));
    }

    @Test
    void samePasswordTwiceGivesDifferentHashes() {
        assertNotEquals(PasswordHasher.hash("Secret123"), PasswordHasher.hash("Secret123"));
    }

    @Test
    void neverStoresThePasswordItself() {
        assertFalse(PasswordHasher.hash("Secret123").contains("Secret123"));
    }

    @Test
    void malformedStoredValuesAreRejectedNotThrown() {
        assertFalse(PasswordHasher.verify("x", null));
        assertFalse(PasswordHasher.verify("x", ""));
        assertFalse(PasswordHasher.verify("x", "garbage"));
        assertFalse(PasswordHasher.verify("x", "pbkdf2_sha256$notanumber$!!$!!"));
        assertFalse(PasswordHasher.verify(null, PasswordHasher.hash("x")));
    }

    @Test
    void dummyHashNeverMatches() {
        assertFalse(PasswordHasher.verify("anything", PasswordHasher.dummyHash()));
    }
}
