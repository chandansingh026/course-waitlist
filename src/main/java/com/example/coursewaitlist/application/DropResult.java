package com.example.coursewaitlist.application;

import io.swagger.v3.oas.annotations.media.Schema;

public record DropResult(String offeringId, String droppedLearnerId,
                         @Schema(description = "Learner promoted to ENROLLED, or null when no eligible waiter was found",
                                 nullable = true)
                         String promotedLearnerId,
                         int skippedIneligibleCount, int remainingCapacity) {
}
