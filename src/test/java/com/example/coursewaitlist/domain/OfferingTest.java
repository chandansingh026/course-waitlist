package com.example.coursewaitlist.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class OfferingTest {
    @Test
    void rejectsNegativeCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new Offering("course", -1));
    }
}
