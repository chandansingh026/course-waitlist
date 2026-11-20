package com.example.coursewaitlist.infrastructure.memory;

import com.example.coursewaitlist.application.EligibilityPolicy;
import java.util.Set;

public class FixtureEligibilityPolicy implements EligibilityPolicy {
    private final Set<String> eligibleLearners;

    public FixtureEligibilityPolicy(Set<String> eligibleLearners) {
        this.eligibleLearners = Set.copyOf(eligibleLearners);
    }

    @Override
    public boolean isEligible(String offeringId, String learnerId) {
        return eligibleLearners.contains(learnerId);
    }
}
