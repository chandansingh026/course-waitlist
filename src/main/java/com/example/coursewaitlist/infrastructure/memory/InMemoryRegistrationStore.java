package com.example.coursewaitlist.infrastructure.memory;

import com.example.coursewaitlist.application.RegistrationRepository;
import com.example.coursewaitlist.application.RegistrationStore;
import com.example.coursewaitlist.domain.Registration;
import com.example.coursewaitlist.domain.RegistrationStatus;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class InMemoryRegistrationStore implements RegistrationStore {
    private final String offeringId;
    private Map<String, Registration> registrations = Map.of();
    private long sequence;
    private boolean executing;

    public InMemoryRegistrationStore(String offeringId) {
        this.offeringId = offeringId;
    }

    @Override
    public synchronized <T> T execute(Function<RegistrationRepository, T> operation) {
        if (executing) {
            throw new IllegalStateException("Nested registration operations are not supported");
        }
        executing = true;
        try {
            WorkingCopy working = new WorkingCopy(offeringId, registrations, sequence);
            T result = operation.apply(working);
            registrations = Map.copyOf(working.registrations);
            sequence = working.sequence;
            return result;
        } finally {
            executing = false;
        }
    }

    private static final class WorkingCopy implements RegistrationRepository {
        private final String offeringId;
        private final Map<String, Registration> registrations;
        private long sequence;

        private WorkingCopy(String offeringId, Map<String, Registration> registrations, long sequence) {
            this.offeringId = offeringId;
            this.registrations = new HashMap<>(registrations);
            this.sequence = sequence;
        }

        @Override
        public Optional<Registration> find(String offeringId, String learnerId) {
            requireOffering(offeringId);
            return Optional.ofNullable(registrations.get(learnerId));
        }

        @Override
        public long countEnrolled(String offeringId) {
            requireOffering(offeringId);
            return registrations.values().stream().filter(r -> r.status() == RegistrationStatus.ENROLLED).count();
        }

        @Override
        public List<Registration> waitlistInOrder(String offeringId) {
            requireOffering(offeringId);
            return registrations.values().stream().filter(r -> r.status() == RegistrationStatus.WAITLISTED)
                    .sorted(Comparator.comparing(Registration::waitlistSequence)).toList();
        }

        @Override
        public long nextWaitlistSequence(String offeringId) {
            requireOffering(offeringId);
            sequence = Math.incrementExact(sequence);
            return sequence;
        }

        @Override
        public void save(Registration registration) {
            requireOffering(registration.offeringId());
            if (registration.status() == RegistrationStatus.WAITLISTED) {
                if (registration.waitlistSequence() > sequence) {
                    throw new IllegalStateException("Waitlist sequence has not been allocated");
                }
                if (registrations.values().stream().anyMatch(existing ->
                            !existing.learnerId().equals(registration.learnerId())
                                    && registration.waitlistSequence().equals(existing.waitlistSequence()))) {
                    throw new IllegalStateException("Waitlist sequence already in use");
                }
            }
            registrations.put(registration.learnerId(), registration);
        }

        private void requireOffering(String id) {
            if (!offeringId.equals(id)) {
                throw new IllegalArgumentException("Registration belongs to a different offering");
            }
        }
    }
}
