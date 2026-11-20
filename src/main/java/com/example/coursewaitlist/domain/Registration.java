package com.example.coursewaitlist.domain;

import java.util.Objects;

public record Registration(String offeringId, String learnerId, RegistrationStatus status, Long waitlistSequence) {
    public Registration {
        Objects.requireNonNull(offeringId, "Offering ID is required");
        Objects.requireNonNull(learnerId, "Learner ID is required");
        Objects.requireNonNull(status, "Status is required");
        if (!Identifier.isValid(offeringId) || !Identifier.isValid(learnerId)) {
            throw new IllegalArgumentException("Identifiers must contain 1 to 100 characters without surrounding whitespace");
        }
        if ((status == RegistrationStatus.WAITLISTED) != (waitlistSequence != null)) {
            throw new IllegalArgumentException("Only a waitlisted registration has a queue sequence");
        }
        if (waitlistSequence != null && waitlistSequence <= 0) {
            throw new IllegalArgumentException("Waitlist sequence must be positive");
        }
    }

    public Registration transitionTo(RegistrationStatus nextStatus) {
        if (status != RegistrationStatus.ENROLLED && status != RegistrationStatus.WAITLISTED) {
            throw new IllegalStateException("Only active registrations may transition");
        }
        if (status == RegistrationStatus.ENROLLED && nextStatus != RegistrationStatus.DROPPED
                || status == RegistrationStatus.WAITLISTED
                && nextStatus != RegistrationStatus.ENROLLED && nextStatus != RegistrationStatus.INELIGIBLE) {
            throw new IllegalArgumentException("Invalid registration transition");
        }
        return new Registration(offeringId, learnerId, nextStatus, null);
    }
}
