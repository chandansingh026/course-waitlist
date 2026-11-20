package com.example.coursewaitlist.application;

import static com.example.coursewaitlist.support.ConcurrentTestSupport.runConcurrently;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.coursewaitlist.domain.Registration;
import com.example.coursewaitlist.domain.RegistrationStatus;
import com.example.coursewaitlist.support.TestStore;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import org.junit.jupiter.api.RepeatedTest;

class EnrollmentConcurrencyTest {
    @RepeatedTest(5)
    void twoLearnersCompeteForTheLastSeat() throws Exception {
        TestStore store = new TestStore(2);
        EnrollmentService first = store.service((offering, learner) -> true);
        EnrollmentService second = store.service((offering, learner) -> true);
        first.register("course", "incumbent");

        List<RegistrationView> results = runConcurrently(List.of(
                () -> first.register("course", "a"), () -> second.register("course", "b")));

        assertTrue(results.stream().allMatch(RegistrationView::created));
        assertEquals(Set.of(RegistrationStatus.ENROLLED, RegistrationStatus.WAITLISTED),
                results.stream().map(RegistrationView::status).collect(Collectors.toSet()));
        assertEquals(2, store.countEnrolled());
        assertEquals(1, store.waitlist().size());
        assertEquals(RegistrationStatus.ENROLLED, store.find("incumbent").orElseThrow().status());
        assertEquals(2, List.of("a", "b").stream().filter(id -> store.find(id).isPresent()).count());
    }

    @RepeatedTest(5)
    void simultaneousRequestsForSameLearnerCreateOnlyOneRegistration() throws Exception {
        for (boolean full : List.of(false, true)) {
            TestStore store = new TestStore(1);
            EnrollmentService first = store.service((offering, learner) -> true);
            EnrollmentService second = store.service((offering, learner) -> true);
            if (full) {
                first.register("course", "incumbent");
            }
            List<Callable<RegistrationView>> requests = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                EnrollmentService selected = i % 2 == 0 ? first : second;
                requests.add(() -> selected.register("course", "a"));
            }

            List<RegistrationView> results = runConcurrently(requests);

            RegistrationStatus expected = full ? RegistrationStatus.WAITLISTED : RegistrationStatus.ENROLLED;
            assertEquals(1, results.stream().filter(RegistrationView::created).count());
            assertTrue(results.stream().allMatch(result -> result.status() == expected));
            assertEquals(expected, store.find("a").orElseThrow().status());
            assertEquals(1, store.countEnrolled());
            assertEquals(full ? 1 : 0, store.waitlist().size());
            if (full) {
                assertEquals(1L, store.find("a").orElseThrow().waitlistSequence());
            }
        }
    }

    @RepeatedTest(5)
    void simultaneousDropsOfOneEnrollmentPromoteOnlyOnce() throws Exception {
        TestStore store = new TestStore(1);
        EnrollmentService first = store.service((offering, learner) -> true);
        EnrollmentService second = store.service((offering, learner) -> true);
        first.register("course", "enrolled");
        first.register("course", "first");
        first.register("course", "second");

        List<DropAttempt> results = runConcurrently(List.of(
                () -> drop(first, "enrolled"), () -> drop(second, "enrolled")));

        assertEquals(1, results.stream().filter(result -> result.result() != null).count());
        assertEquals(1, results.stream().filter(result -> result.error() == RequestException.Kind.CONFLICT).count());
        assertEquals("first", results.stream().map(DropAttempt::result).filter(Objects::nonNull)
                .findFirst().orElseThrow().promotedLearnerId());
        assertEquals(RegistrationStatus.DROPPED, store.find("enrolled").orElseThrow().status());
        assertEquals(RegistrationStatus.ENROLLED, store.find("first").orElseThrow().status());
        assertEquals(List.of("second"), queue(store));
        assertEquals(1, store.countEnrolled());
    }

    @RepeatedTest(5)
    void concurrentQueueEntriesAndDropsPromoteInStoredFifoOrder() throws Exception {
        TestStore store = new TestStore(2);
        EnrollmentService first = store.service((offering, learner) -> true);
        EnrollmentService second = store.service((offering, learner) -> true);
        first.register("course", "enrolled-1");
        first.register("course", "enrolled-2");
        List<Callable<RegistrationView>> requests = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String learner = "waiting-" + i;
            EnrollmentService selected = i % 2 == 0 ? first : second;
            requests.add(() -> selected.register("course", learner));
        }

        List<RegistrationView> registrations = runConcurrently(requests);
        List<String> originalQueue = queue(store);
        assertEquals(4, originalQueue.size());
        assertTrue(registrations.stream().allMatch(result -> result.created()
                && result.status() == RegistrationStatus.WAITLISTED));
        assertEquals(List.of(1L, 2L, 3L, 4L),
                store.waitlist().stream().map(Registration::waitlistSequence).toList());

        CountDownLatch firstPromotion = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        EnrollmentService controlled = store.service((offering, learner) -> {
            if (learner.equals(originalQueue.getFirst())) {
                firstPromotion.countDown();
                await(release);
            }
            return true;
        });
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<DropResult> firstDrop = executor.submit(() -> controlled.drop("course", "enrolled-1"));
            assertTrue(firstPromotion.await(5, TimeUnit.SECONDS));
            Future<DropResult> secondDrop = executor.submit(() -> {
                secondStarted.countDown();
                return second.drop("course", "enrolled-2");
            });
            assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
            release.countDown();
            assertEquals(originalQueue.get(0), firstDrop.get(10, TimeUnit.SECONDS).promotedLearnerId());
            assertEquals(originalQueue.get(1), secondDrop.get(10, TimeUnit.SECONDS).promotedLearnerId());
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }

        assertEquals(originalQueue.subList(2, 4), queue(store));
        assertEquals(RegistrationStatus.DROPPED, store.find("enrolled-1").orElseThrow().status());
        assertEquals(RegistrationStatus.DROPPED, store.find("enrolled-2").orElseThrow().status());
        originalQueue.subList(0, 2).forEach(id -> assertEquals(RegistrationStatus.ENROLLED,
                store.find(id).orElseThrow().status()));
        assertEquals(2, store.countEnrolled());
    }

    @RepeatedTest(5)
    void failedOperationReleasesLockAndDoesNotPublishPartialState() throws Exception {
        TestStore store = new TestStore(0);
        EnrollmentService followerService = store.service((offering, learner) -> true);
        CountDownLatch staged = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch competing = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> failing = executor.submit(() -> store.execute(repository -> {
                long sequence = repository.nextWaitlistSequence("course");
                repository.save(new Registration("course", "a", RegistrationStatus.WAITLISTED, sequence));
                staged.countDown();
                await(release);
                throw new IllegalStateException("Simulated failure");
            }));
            assertTrue(staged.await(5, TimeUnit.SECONDS));
            Future<RegistrationView> follower = executor.submit(() -> {
                competing.countDown();
                return followerService.register("course", "b");
            });
            assertTrue(competing.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> follower.get(200, TimeUnit.MILLISECONDS));

            release.countDown();
            ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> failing.get(10, TimeUnit.SECONDS));
            assertInstanceOf(IllegalStateException.class, failure.getCause());
            assertEquals(RegistrationStatus.WAITLISTED, follower.get(10, TimeUnit.SECONDS).status());
            assertTrue(store.find("a").isEmpty());
            assertEquals(1L, store.find("b").orElseThrow().waitlistSequence());
            assertEquals(List.of("b"), queue(store));
            assertEquals(0, store.countEnrolled());
        } finally {
            release.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private static DropAttempt drop(EnrollmentService service, String learner) {
        try {
            return new DropAttempt(service.drop("course", learner), null);
        } catch (RequestException ex) {
            return new DropAttempt(null, ex.kind());
        }
    }

    private static List<String> queue(TestStore store) {
        return store.waitlist().stream().map(Registration::learnerId).toList();
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent operation timed out");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrent operation interrupted", ex);
        }
    }

    private record DropAttempt(DropResult result, RequestException.Kind error) {
    }
}
