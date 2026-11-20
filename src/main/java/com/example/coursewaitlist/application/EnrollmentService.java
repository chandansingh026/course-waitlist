package com.example.coursewaitlist.application;

import com.example.coursewaitlist.domain.Offering;
import com.example.coursewaitlist.domain.Identifier;
import com.example.coursewaitlist.domain.Registration;
import com.example.coursewaitlist.domain.RegistrationStatus;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnrollmentService implements EnrollmentOperations {
    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);
    private final OfferingRepository offerings;
    private final RegistrationStore store;
    private final EligibilityPolicy eligibility;

    public EnrollmentService(OfferingRepository offerings, RegistrationStore store, EligibilityPolicy eligibility) {
        this.offerings = offerings;
        this.store = store;
        this.eligibility = eligibility;
    }

    @Override
    public RegistrationView register(String offeringId, String learnerId) {
        validateIdentifier(offeringId);
        validateIdentifier(learnerId);
        RegistrationView result = store.execute(registrations -> {
            Offering offering = requireOffering(offeringId);
            Registration current = registrations.find(offeringId, learnerId).orElse(null);
            if (current != null && current.status().isActive()) {
                return view(registrations, current, false);
            }
            if (!eligibility.isEligible(offeringId, learnerId)) {
                throw new RequestException(RequestException.Kind.INELIGIBLE, "Prerequisite is not satisfied");
            }
            Registration registration;
            if (registrations.countEnrolled(offeringId) < offering.capacity()) {
                registration = new Registration(offeringId, learnerId, RegistrationStatus.ENROLLED, null);
            } else {
                registration = new Registration(offeringId, learnerId, RegistrationStatus.WAITLISTED,
                        registrations.nextWaitlistSequence(offeringId));
            }
            registrations.save(registration);
            return view(registrations, registration, true);
        });
        if (result.created()) {
            log.info("Registration created with status {}", result.status());
        }
        return result;
    }

    @Override
    public RegistrationView getRegistration(String offeringId, String learnerId) {
        validateIdentifier(offeringId);
        validateIdentifier(learnerId);
        return store.execute(registrations -> {
            requireOffering(offeringId);
            Registration registration = registrations.find(offeringId, learnerId)
                    .orElseThrow(() -> new RequestException(RequestException.Kind.NOT_FOUND, "Registration not found"));
            return view(registrations, registration, false);
        });
    }

    @Override
    public DropResult drop(String offeringId, String learnerId) {
        validateIdentifier(offeringId);
        validateIdentifier(learnerId);
        DropResult result = store.execute(registrations -> {
            Offering offering = requireOffering(offeringId);
            Registration enrolled = registrations.find(offeringId, learnerId)
                    .filter(r -> r.status() == RegistrationStatus.ENROLLED)
                    .orElseThrow(() -> new RequestException(RequestException.Kind.CONFLICT,
                            "Learner is not enrolled"));
            List<Registration> ineligible = new ArrayList<>();
            Registration candidate = null;
            for (Registration waiting : registrations.waitlistInOrder(offeringId)) {
                if (eligibility.isEligible(offeringId, waiting.learnerId())) {
                    candidate = waiting;
                    break;
                }
                ineligible.add(waiting);
            }
            registrations.save(enrolled.transitionTo(RegistrationStatus.DROPPED));
            for (Registration skipped : ineligible) {
                registrations.save(skipped.transitionTo(RegistrationStatus.INELIGIBLE));
            }
            if (candidate != null) {
                registrations.save(candidate.transitionTo(RegistrationStatus.ENROLLED));
            }
            int remainingCapacity = Math.toIntExact(offering.capacity() - registrations.countEnrolled(offeringId));
            return new DropResult(offeringId, learnerId, candidate == null ? null : candidate.learnerId(),
                    ineligible.size(), remainingCapacity);
        });
        log.info("Enrollment dropped; promotion={}, skipped={}",
                result.promotedLearnerId() != null, result.skippedIneligibleCount());
        return result;
    }

    private Offering requireOffering(String offeringId) {
        return offerings.findById(offeringId)
                .orElseThrow(() -> new RequestException(RequestException.Kind.NOT_FOUND, "Offering not found"));
    }

    private RegistrationView view(RegistrationRepository registrations, Registration registration, boolean created) {
        Integer position = null;
        if (registration.status() == RegistrationStatus.WAITLISTED) {
            List<Registration> queue = registrations.waitlistInOrder(registration.offeringId());
            int index = queue.indexOf(registration);
            if (index < 0) {
                throw new IllegalStateException("Waitlisted registration missing from queue");
            }
            position = index + 1;
        }
        return new RegistrationView(registration.offeringId(), registration.learnerId(), registration.status(),
                position, created);
    }

    private static void validateIdentifier(String id) {
        if (!Identifier.isValid(id)) {
            throw new RequestException(RequestException.Kind.INVALID,
                    "Identifiers must be non-blank, at most 100 characters, and have no surrounding whitespace");
        }
    }
}
