package com.example.coursewaitlist.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RegistrationTest {
    @ParameterizedTest
    @ValueSource(strings = {"", " ", " course", "course\u2003"})
    void rejectsInvalidIdentifiers(String invalidId) {
        assertThrows(IllegalArgumentException.class,
                () -> new Registration(invalidId, "learner", RegistrationStatus.ENROLLED, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Registration("course", invalidId, RegistrationStatus.ENROLLED, null));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void rejectsNonPositiveWaitlistSequences(long sequence) {
        assertThrows(IllegalArgumentException.class,
                () -> new Registration("course", "learner", RegistrationStatus.WAITLISTED, sequence));
    }

    @Test
    void rejectsIdentifiersLongerThanTheSupportedLimit() {
        String invalidId = "a".repeat(101);
        assertThrows(IllegalArgumentException.class,
                () -> new Registration(invalidId, "learner", RegistrationStatus.ENROLLED, null));
        assertThrows(IllegalArgumentException.class,
                () -> new Registration("course", invalidId, RegistrationStatus.ENROLLED, null));
    }
}
