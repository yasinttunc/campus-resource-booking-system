package com.project.coursework2.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordUtilsTest {

    @Test
    void hashIsSaltedAndVerifiesTheOriginalPassword() {
        String first = PasswordUtils.hash("correct horse");
        String second = PasswordUtils.hash("correct horse");

        assertTrue(first.startsWith("pbkdf2$"));
        assertNotEquals(first, second, "each hash must use its own random salt");
        assertTrue(PasswordUtils.verify("correct horse", first));
        assertTrue(PasswordUtils.verify("correct horse", second));
        assertFalse(PasswordUtils.needsRehash(first));
    }

    @Test
    void rejectsWrongPassword() {
        String stored = PasswordUtils.hash("correct horse");

        assertFalse(PasswordUtils.verify("Correct horse", stored));
        assertFalse(PasswordUtils.verify("", stored));
        assertFalse(PasswordUtils.verify(null, stored));
    }

    @Test
    void acceptsLegacySha256HashesAndFlagsThemForRehash() {
        // SHA-256("admin"), the format used before PBKDF2 was introduced
        String legacy = "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918";

        assertTrue(PasswordUtils.verify("admin", legacy));
        assertFalse(PasswordUtils.verify("admin1", legacy));
        assertTrue(PasswordUtils.needsRehash(legacy));
    }

    @Test
    void rejectsMalformedStoredValues() {
        assertFalse(PasswordUtils.verify("admin", null));
        assertFalse(PasswordUtils.verify("admin", "changeme"));
        assertFalse(PasswordUtils.verify("admin", "pbkdf2$abc$$"));
        assertFalse(PasswordUtils.verify("admin", "pbkdf2$600000$not-base64!$x"));
    }
}
