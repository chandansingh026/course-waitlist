package com.example.coursewaitlist.application;

import com.example.coursewaitlist.domain.RegistrationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record RegistrationView(String offeringId, String learnerId, RegistrationStatus status,
                               @Schema(description = "One-based position while WAITLISTED; null otherwise", nullable = true)
                               Integer waitlistPosition,
                               boolean created) {
}
