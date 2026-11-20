package com.example.coursewaitlist.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IdentifierTest {
    @Test
    void acceptsBoundedOpaqueIdentifiersWithoutSurroundingWhitespace() {
        assertTrue(Identifier.isValid("learner-1"));
        assertTrue(Identifier.isValid("a b"));
        assertTrue(Identifier.isValid("a".repeat(100)));
    }

    @Test
    void rejectsMissingLongAndUnicodeWhitespaceSurroundedIdentifiers() {
        for (String id : new String[] {null, "", " ", "\u00a0", "\u00a0a", "a\u2003", "a".repeat(101)}) {
            assertFalse(Identifier.isValid(id));
        }
    }
}
