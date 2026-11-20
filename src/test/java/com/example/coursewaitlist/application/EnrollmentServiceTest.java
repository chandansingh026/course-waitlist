package com.example.coursewaitlist.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.coursewaitlist.domain.Offering;
import com.example.coursewaitlist.domain.Registration;
import com.example.coursewaitlist.domain.RegistrationStatus;
import com.example.coursewaitlist.infrastructure.memory.InMemoryOfferingRepository;
import com.example.coursewaitlist.support.TestStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EnrollmentServiceTest {
    private TestStore store;
    private EligibilityPolicy eligibility;
    private EnrollmentService service;

    @BeforeEach
    void setUp() {
        store = new TestStore(1);
        eligibility = mock(EligibilityPolicy.class);
        service = store.service(eligibility);
    }

    @Test
    void enrollsWaitlistsAndDoesNotDuplicate() {
        when(eligibility.isEligible("course", "a")).thenReturn(true);
        when(eligibility.isEligible("course", "b")).thenReturn(true);

        assertEquals(RegistrationStatus.ENROLLED, service.register("course", "a").status());
        assertFalse(service.register("course", "a").created());
        RegistrationView waiting = service.register("course", "b");
        assertEquals(RegistrationStatus.WAITLISTED, waiting.status());
        assertEquals(1, waiting.waitlistPosition());
        assertEquals(waiting.waitlistPosition(), service.register("course", "b").waitlistPosition());
        assertEquals(1, store.countEnrolled());
        assertEquals(1, store.waitlist().size());
    }

    @Test
    void skipsLearnersWhoBecomeIneligibleAndPromotesInFifoOrder() {
        when(eligibility.isEligible("course", "a")).thenReturn(true);
        when(eligibility.isEligible("course", "b")).thenReturn(true, false);
        when(eligibility.isEligible("course", "c")).thenReturn(true);
        when(eligibility.isEligible("course", "d")).thenReturn(true);
        service.register("course", "a");
        service.register("course", "b");
        service.register("course", "c");
        service.register("course", "d");

        DropResult result = service.drop("course", "a");
        assertEquals("c", result.promotedLearnerId());
        assertEquals(1, result.skippedIneligibleCount());
        assertEquals(0, result.remainingCapacity());
        assertEquals(RegistrationStatus.INELIGIBLE, service.getRegistration("course", "b").status());
        assertEquals(RegistrationStatus.ENROLLED, service.getRegistration("course", "c").status());
        assertEquals(1, service.getRegistration("course", "d").waitlistPosition());
        assertEquals("d", service.drop("course", "c").promotedLearnerId());
    }

    @Test
    void invalidRequestsAndRepeatedDropsLeaveStateUnchangedAndAllowReenrollment() {
        when(eligibility.isEligible("course", "a")).thenReturn(true);
        assertEquals(RequestException.Kind.INELIGIBLE,
                assertThrows(RequestException.class, () -> service.register("course", "b")).kind());
        assertEquals(RequestException.Kind.INVALID,
                assertThrows(RequestException.class, () -> service.register("course", " ")).kind());
        assertEquals(RequestException.Kind.NOT_FOUND,
                assertThrows(RequestException.class, () -> service.register("missing", "a")).kind());
        service.register("course", "a");
        assertEquals(RequestException.Kind.CONFLICT,
                assertThrows(RequestException.class, () -> service.drop("course", "b")).kind());
        assertEquals(1, store.countEnrolled());
        DropResult dropped = service.drop("course", "a");
        assertEquals(1, dropped.remainingCapacity());
        assertEquals(RequestException.Kind.CONFLICT,
                assertThrows(RequestException.class, () -> service.drop("course", "a")).kind());
        assertEquals(0, store.countEnrolled());
        assertTrue(service.register("course", "a").created());
        assertEquals(RegistrationStatus.ENROLLED, service.getRegistration("course", "a").status());
    }

    @Test
    void reenrollmentWhenFullJoinsTheEndOfTheWaitlist() {
        when(eligibility.isEligible("course", "a")).thenReturn(true);
        when(eligibility.isEligible("course", "b")).thenReturn(true);
        when(eligibility.isEligible("course", "c")).thenReturn(true);
        service.register("course", "a");
        service.register("course", "b");
        service.drop("course", "a");
        service.register("course", "c");

        assertEquals(2, service.register("course", "a").waitlistPosition());
        assertEquals("c", service.drop("course", "b").promotedLearnerId());
    }

    @Test
    void zeroCapacityStillRejectsIneligibleAndWaitlistsEligibleLearners() {
        store = new TestStore(0);
        service = store.service(eligibility);
        when(eligibility.isEligible("course", "a")).thenReturn(true);

        assertEquals(RequestException.Kind.INELIGIBLE,
                assertThrows(RequestException.class, () -> service.register("course", "b")).kind());
        assertEquals(RegistrationStatus.WAITLISTED, service.register("course", "a").status());
        assertEquals(0, store.countEnrolled());
    }

    @Test
    void dropWithNoEligibleWaitersLeavesSeatAvailable() {
        when(eligibility.isEligible("course", "a")).thenReturn(true);
        when(eligibility.isEligible("course", "b")).thenReturn(true, false);
        service.register("course", "a");
        service.register("course", "b");

        DropResult result = service.drop("course", "a");
        assertEquals(1, result.remainingCapacity());
        assertEquals(1, result.skippedIneligibleCount());
        assertEquals(null, result.promotedLearnerId());
        assertEquals(RegistrationStatus.INELIGIBLE, service.getRegistration("course", "b").status());
        assertTrue(store.waitlist().isEmpty());
    }

    @Test
    void eligibilityFailureDuringPromotionLeavesAllStateUnchanged() {
        when(eligibility.isEligible("course", "a")).thenReturn(true);
        when(eligibility.isEligible("course", "b")).thenReturn(true, false, false);
        when(eligibility.isEligible("course", "c")).thenReturn(true)
                .thenThrow(new IllegalStateException("Eligibility unavailable")).thenReturn(true);
        service.register("course", "a");
        service.register("course", "b");
        service.register("course", "c");

        assertThrows(IllegalStateException.class, () -> service.drop("course", "a"));
        assertEquals(RegistrationStatus.ENROLLED, service.getRegistration("course", "a").status());
        assertEquals(1, service.getRegistration("course", "b").waitlistPosition());
        assertEquals(2, service.getRegistration("course", "c").waitlistPosition());
        assertEquals("c", service.drop("course", "a").promotedLearnerId());
    }

    @Test
    void failureAfterPromotionDiscardsAllStagedDropChanges() {
        when(eligibility.isEligible("course", "a")).thenReturn(true);
        when(eligibility.isEligible("course", "b")).thenReturn(true, false, false);
        when(eligibility.isEligible("course", "c")).thenReturn(true);
        service.register("course", "a");
        service.register("course", "b");
        service.register("course", "c");
        EnrollmentService failing = serviceFailingAfterSave(registration ->
                registration.learnerId().equals("c") && registration.status() == RegistrationStatus.ENROLLED);

        assertThrows(IllegalStateException.class, () -> failing.drop("course", "a"));

        assertEquals(RegistrationStatus.ENROLLED, store.find("a").orElseThrow().status());
        assertEquals(RegistrationStatus.WAITLISTED, store.find("b").orElseThrow().status());
        assertEquals(RegistrationStatus.WAITLISTED, store.find("c").orElseThrow().status());
        assertEquals(1, store.countEnrolled());
        assertEquals(List.of("b", "c"), store.waitlist().stream().map(Registration::learnerId).toList());
        assertEquals("c", service.drop("course", "a").promotedLearnerId());
    }

    @Test
    void failureAfterWaitlistSaveDiscardsRegistrationAndSequence() {
        when(eligibility.isEligible("course", "a")).thenReturn(true);
        when(eligibility.isEligible("course", "b")).thenReturn(true);
        service.register("course", "a");
        EnrollmentService failing = serviceFailingAfterSave(registration ->
                registration.learnerId().equals("b") && registration.status() == RegistrationStatus.WAITLISTED);

        assertThrows(IllegalStateException.class, () -> failing.register("course", "b"));

        assertTrue(store.find("b").isEmpty());
        assertEquals(RegistrationStatus.WAITLISTED, service.register("course", "b").status());
        assertEquals(1L, store.find("b").orElseThrow().waitlistSequence());
    }

    @Test
    void concurrentRequestsCannotOverEnrollDuplicateOrPromoteTwice() throws Exception {
        int requests = 24;
        EnrollmentService concurrentService = store.service((offering, learner) -> true);
        EnrollmentService secondService = store.service((offering, learner) -> true);
        ExecutorService executor = Executors.newFixedThreadPool(requests);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<RegistrationView>> results = new ArrayList<>();
            for (int i = 0; i < requests; i++) {
                String learner = "learner-" + (i % 12);
                EnrollmentService selected = i < 12 ? concurrentService : secondService;
                results.add(executor.submit(() -> {
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Start timed out");
                    }
                    return selected.register("course", learner);
                }));
            }
            start.countDown();
            for (Future<RegistrationView> result : results) {
                result.get(10, TimeUnit.SECONDS);
            }
            assertEquals(1, store.countEnrolled());
            assertEquals(11, store.waitlist().size());
            String enrolled = results.stream().map(result -> {
                try {
                    return result.get().learnerId();
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }).filter(id -> concurrentService.getRegistration("course", id).status() == RegistrationStatus.ENROLLED)
                    .findFirst().orElseThrow();
            String firstWaiting = store.waitlist().getFirst().learnerId();
            CountDownLatch dropStart = new CountDownLatch(1);
            List<Future<Boolean>> drops = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                EnrollmentService selected = i == 0 ? concurrentService : secondService;
                drops.add(executor.submit(() -> {
                    if (!dropStart.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Start timed out");
                    }
                    try {
                        selected.drop("course", enrolled);
                        return true;
                    } catch (RequestException ex) {
                        if (ex.kind() != RequestException.Kind.CONFLICT) {
                            throw ex;
                        }
                        return false;
                    }
                }));
            }
            dropStart.countDown();
            assertEquals(1, drops.stream().filter(future -> {
                try {
                    return future.get(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
            }).count());
            assertEquals(RegistrationStatus.ENROLLED, concurrentService.getRegistration("course", firstWaiting).status());
            assertEquals(1, store.countEnrolled());
            assertEquals(10, store.waitlist().size());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void concurrentDropsOfDifferentLearnersPromoteSingleWaiterOnlyOnce() throws Exception {
        store = new TestStore(2);
        EnrollmentService firstService = store.service((offering, learner) -> true);
        EnrollmentService secondService = store.service((offering, learner) -> true);
        firstService.register("course", "a");
        firstService.register("course", "b");
        firstService.register("course", "waiting");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<DropResult> first = executor.submit(() -> {
                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Start timed out");
                }
                return firstService.drop("course", "a");
            });
            Future<DropResult> second = executor.submit(() -> {
                if (!start.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Start timed out");
                }
                return secondService.drop("course", "b");
            });
            start.countDown();
            DropResult firstResult = first.get(10, TimeUnit.SECONDS);
            DropResult secondResult = second.get(10, TimeUnit.SECONDS);

            assertEquals(1, java.util.stream.Stream.of(firstResult, secondResult)
                    .filter(result -> "waiting".equals(result.promotedLearnerId())).count());
            assertEquals(1, java.util.stream.Stream.of(firstResult, secondResult)
                    .filter(result -> result.promotedLearnerId() == null).count());
            assertEquals(List.of(0, 1), java.util.stream.Stream.of(firstResult, secondResult)
                    .map(DropResult::remainingCapacity).sorted().toList());
            assertEquals(RegistrationStatus.DROPPED, firstService.getRegistration("course", "a").status());
            assertEquals(RegistrationStatus.DROPPED, firstService.getRegistration("course", "b").status());
            assertEquals(RegistrationStatus.ENROLLED, firstService.getRegistration("course", "waiting").status());
            assertEquals(1, store.countEnrolled());
            assertTrue(store.waitlist().isEmpty());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private EnrollmentService serviceFailingAfterSave(Predicate<Registration> shouldFail) {
        RegistrationStore failingStore = new RegistrationStore() {
            @Override
            public <T> T execute(Function<RegistrationRepository, T> operation) {
                return store.execute(delegate -> operation.apply(new RegistrationRepository() {
                    public Optional<Registration> find(String offeringId, String learnerId) {
                        return delegate.find(offeringId, learnerId);
                    }
                    public long countEnrolled(String offeringId) {
                        return delegate.countEnrolled(offeringId);
                    }
                    public List<Registration> waitlistInOrder(String offeringId) {
                        return delegate.waitlistInOrder(offeringId);
                    }
                    public long nextWaitlistSequence(String offeringId) {
                        return delegate.nextWaitlistSequence(offeringId);
                    }
                    public void save(Registration registration) {
                        delegate.save(registration);
                        if (shouldFail.test(registration)) {
                            throw new IllegalStateException("Simulated failure after save");
                        }
                    }
                }));
            }
        };
        return new EnrollmentService(new InMemoryOfferingRepository(new Offering("course", 1)),
                failingStore, eligibility);
    }
}
