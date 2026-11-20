package com.example.coursewaitlist.support;

import com.example.coursewaitlist.application.EligibilityPolicy;
import com.example.coursewaitlist.application.EnrollmentService;
import com.example.coursewaitlist.application.RegistrationRepository;
import com.example.coursewaitlist.application.RegistrationStore;
import com.example.coursewaitlist.domain.Offering;
import com.example.coursewaitlist.domain.Registration;
import com.example.coursewaitlist.infrastructure.memory.InMemoryOfferingRepository;
import com.example.coursewaitlist.infrastructure.memory.InMemoryRegistrationStore;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class TestStore {
    private final Offering offering;
    private final RegistrationStore store;

    public TestStore(int capacity) {
        offering = new Offering("course", capacity);
        store = new InMemoryRegistrationStore(offering.id());
    }

    public EnrollmentService service(EligibilityPolicy eligibility) {
        return new EnrollmentService(new InMemoryOfferingRepository(offering), store, eligibility);
    }

    public <T> T execute(Function<RegistrationRepository, T> operation) {
        return store.execute(operation);
    }

    public Optional<Registration> find(String learner) {
        return execute(repository -> repository.find("course", learner));
    }

    public long countEnrolled() {
        return execute(repository -> repository.countEnrolled("course"));
    }

    public List<Registration> waitlist() {
        return execute(repository -> repository.waitlistInOrder("course"));
    }
}
