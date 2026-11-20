package com.example.coursewaitlist.domain;

public enum RegistrationStatus {
    ENROLLED, WAITLISTED, DROPPED, INELIGIBLE;

    public boolean isActive() {
        return this == ENROLLED || this == WAITLISTED;
    }
}
