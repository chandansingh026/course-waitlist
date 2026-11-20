package com.example.coursewaitlist.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.coursewaitlist.application.EnrollmentService;
import com.example.coursewaitlist.domain.Registration;
import com.example.coursewaitlist.domain.RegistrationStatus;
import com.example.coursewaitlist.support.TestStore;
import org.junit.jupiter.api.Test;

class InMemoryRegistrationStoreTest {
    @Test
    void failedOperationDiscardsAllChangesAndSequenceAllocation() {
        TestStore store = new TestStore(1);
        EnrollmentService service = store.service((offering, learner) -> true);
        service.register("course", "a");
        service.register("course", "b");
        service.register("course", "c");

        assertThrows(IllegalStateException.class, () -> store.execute(repository -> {
            repository.save(repository.find("course", "a").orElseThrow().transitionTo(RegistrationStatus.DROPPED));
            repository.save(repository.find("course", "b").orElseThrow().transitionTo(RegistrationStatus.INELIGIBLE));
            repository.save(new Registration("course", "d", RegistrationStatus.WAITLISTED,
                    repository.nextWaitlistSequence("course")));
            throw new IllegalStateException("Simulated failure");
        }));

        assertEquals(RegistrationStatus.ENROLLED, store.find("a").orElseThrow().status());
        assertEquals(RegistrationStatus.WAITLISTED, store.find("b").orElseThrow().status());
        assertEquals(RegistrationStatus.WAITLISTED, store.find("c").orElseThrow().status());
        assertTrue(store.find("d").isEmpty());
        assertEquals(1, store.countEnrolled());
        assertEquals(3, service.register("course", "d").waitlistPosition());
        assertEquals(3L, store.find("d").orElseThrow().waitlistSequence());
    }

    @Test
    void nestedOperationIsRejectedWithoutCommittingOuterChanges() {
        TestStore store = new TestStore(1);

        assertThrows(IllegalStateException.class, () -> store.execute(repository -> {
            repository.save(new Registration("course", "a", RegistrationStatus.ENROLLED, null));
            return store.execute(inner -> inner.countEnrolled("course"));
        }));

        assertTrue(store.find("a").isEmpty());
        assertEquals(0, store.countEnrolled());
    }

    @Test
    void rejectsDuplicateQueueSequencesAndWrongOfferingWithoutChangingState() {
        TestStore store = new TestStore(0);
        store.service((offering, learner) -> true).register("course", "a");

        assertThrows(IllegalStateException.class, () -> store.execute(repository -> {
            repository.save(new Registration("course", "b", RegistrationStatus.WAITLISTED, 1L));
            return null;
        }));
        assertThrows(IllegalStateException.class, () -> store.execute(repository -> {
            repository.save(new Registration("course", "b", RegistrationStatus.WAITLISTED, 2L));
            return null;
        }));
        assertThrows(IllegalArgumentException.class, () -> store.execute(repository -> {
            repository.save(new Registration("other", "b", RegistrationStatus.ENROLLED, null));
            return null;
        }));

        assertTrue(store.find("b").isEmpty());
        assertEquals(1, store.waitlist().size());
        assertEquals("a", store.waitlist().getFirst().learnerId());
    }

    @Test
    void separateStoresDoNotShareRegistrations() {
        TestStore first = new TestStore(1);
        TestStore second = new TestStore(1);

        first.service((offering, learner) -> true).register("course", "a");

        assertTrue(second.find("a").isEmpty());
        assertEquals(0, second.countEnrolled());
    }
}
