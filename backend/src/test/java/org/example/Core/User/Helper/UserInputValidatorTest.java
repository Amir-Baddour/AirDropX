package org.example.Core.User.Helper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UserInputValidatorTest {

    @Test
    void emailIsTrimmedAndLowerCased() {
        assertEquals("majd@example.com", UserInputValidator.email("  Majd@Example.COM "));
    }

    @Test
    void badEmailsAreRejected() {
        for (String bad : new String[]{null, "", "   ", "nope", "a@b", "a@@b.com", "a b@c.com", "@c.com"}) {
            assertThrows(IllegalArgumentException.class, () -> UserInputValidator.email(bad), "should reject: " + bad);
        }
    }

    @Test
    void passwordNeedsLengthLetterAndNumber() {
        assertDoesNotThrow(() -> UserInputValidator.password("abc12345"));
        assertThrows(IllegalArgumentException.class, () -> UserInputValidator.password(null));
        assertThrows(IllegalArgumentException.class, () -> UserInputValidator.password("short1"));
        assertThrows(IllegalArgumentException.class, () -> UserInputValidator.password("onlyletters"));
        assertThrows(IllegalArgumentException.class, () -> UserInputValidator.password("12345678"));
        assertThrows(IllegalArgumentException.class, () -> UserInputValidator.password("a1".repeat(70)));
    }

    @Test
    void namesAreRequiredAndTrimmed() {
        assertEquals("Majd", UserInputValidator.name("  Majd ", "First name"));
        assertThrows(IllegalArgumentException.class, () -> UserInputValidator.name("  ", "First name"));
        assertThrows(IllegalArgumentException.class, () -> UserInputValidator.name("x".repeat(101), "First name"));
    }

    @Test
    void phoneAndAddressAreOptional() {
        assertNull(UserInputValidator.phone(null));
        assertNull(UserInputValidator.phone("  "));
        assertEquals("+961 70 123 456", UserInputValidator.phone(" +961 70 123 456 "));
        assertThrows(IllegalArgumentException.class, () -> UserInputValidator.phone("call me"));
        assertNull(UserInputValidator.address(""));
        assertEquals("Beirut", UserInputValidator.address(" Beirut "));
        assertThrows(IllegalArgumentException.class, () -> UserInputValidator.address("x".repeat(256)));
    }
}
