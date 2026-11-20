package com.example.coursewaitlist.domain;

import java.util.Objects;

public record Offering(String id, int capacity) {
    public Offering {
        Objects.requireNonNull(id, "Offering ID is required");
        if (!Identifier.isValid(id)) {
            throw new IllegalArgumentException("Offering ID must contain 1 to 100 characters without surrounding whitespace");
        }
        if (capacity < 0) {
            throw new IllegalArgumentException("Capacity must not be negative");
        }
    }
}
