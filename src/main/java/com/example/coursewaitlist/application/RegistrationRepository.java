package com.example.coursewaitlist.application;

import com.example.coursewaitlist.domain.Registration;
import java.util.List;
import java.util.Optional;

public interface RegistrationRepository {
    Optional<Registration> find(String offeringId, String learnerId);
    long countEnrolled(String offeringId);
    List<Registration> waitlistInOrder(String offeringId);
    long nextWaitlistSequence(String offeringId);
    void save(Registration registration);
}
