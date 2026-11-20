package com.example.coursewaitlist.application;

@FunctionalInterface
public interface EligibilityPolicy {
    boolean isEligible(String offeringId, String learnerId);
}
