package com.example.coursewaitlist.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.coursewaitlist.domain.Offering;
import com.example.coursewaitlist.domain.Registration;
import com.example.coursewaitlist.domain.RegistrationStatus;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class EnrollmentServiceUnitTest {
    private OfferingRepository offerings;
    private RegistrationRepository registrations;
    private EligibilityPolicy eligibility;
    private RegistrationStore store;
    private EnrollmentService service;

    @BeforeEach
    void setUp() {
        offerings = mock(OfferingRepository.class);
        registrations = mock(RegistrationRepository.class);
        eligibility = mock(EligibilityPolicy.class);
        store = mock(RegistrationStore.class);
        when(store.execute(any())).thenAnswer(invocation -> {
            Function<RegistrationRepository, ?> operation = invocation.getArgument(0);
            return operation.apply(registrations);
        });
        when(offerings.findById("course")).thenReturn(Optional.of(new Offering("course", 1)));
        when(registrations.find("course", "learner")).thenReturn(Optional.empty());
        service = new EnrollmentService(offerings, store, eligibility);
    }

    @Test
    void eligibleLearnerIsEnrolledWhenCapacityRemains() {
        when(eligibility.isEligible("course", "learner")).thenReturn(true);
        when(registrations.countEnrolled("course")).thenReturn(0L);

        RegistrationView result = service.register("course", "learner");

        assertTrue(result.created());
        assertEquals(RegistrationStatus.ENROLLED, result.status());
        ArgumentCaptor<Registration> saved = ArgumentCaptor.forClass(Registration.class);
        verify(registrations).save(saved.capture());
        assertEquals(new Registration("course", "learner", RegistrationStatus.ENROLLED, null), saved.getValue());
    }

    @Test
    void eligibleLearnerIsWaitlistedWhenCapacityIsFull() {
        when(eligibility.isEligible("course", "learner")).thenReturn(true);
        when(registrations.countEnrolled("course")).thenReturn(1L);
        when(registrations.nextWaitlistSequence("course")).thenReturn(8L);
        Registration waiting = new Registration("course", "learner", RegistrationStatus.WAITLISTED, 8L);
        when(registrations.waitlistInOrder("course")).thenReturn(List.of(waiting));

        RegistrationView result = service.register("course", "learner");

        assertTrue(result.created());
        assertEquals(RegistrationStatus.WAITLISTED, result.status());
        assertEquals(1, result.waitlistPosition());
        verify(registrations).save(waiting);
    }

    @Test
    void duplicateActiveEnrollmentReturnsExistingStateWithoutRecheckingEligibility() {
        Registration existing = new Registration("course", "learner", RegistrationStatus.ENROLLED, null);
        when(registrations.find("course", "learner")).thenReturn(Optional.of(existing));

        RegistrationView result = service.register("course", "learner");

        assertFalse(result.created());
        assertEquals(RegistrationStatus.ENROLLED, result.status());
        verifyNoInteractions(eligibility);
        verify(registrations, never()).save(any());
    }

    @Test
    void ineligibleRequestRollsBackWithoutCreatingARegistration() {
        when(eligibility.isEligible("course", "learner")).thenReturn(false);

        RequestException error = assertThrows(RequestException.class, () -> service.register("course", "learner"));

        assertEquals(RequestException.Kind.INELIGIBLE, error.kind());
        verify(registrations, never()).save(any());
        verify(registrations, never()).nextWaitlistSequence(any());
    }

    @Test
    void unknownOfferingCannotAcceptARegistration() {
        when(offerings.findById("missing")).thenReturn(Optional.empty());

        RequestException error = assertThrows(RequestException.class, () -> service.register("missing", "learner"));

        assertEquals(RequestException.Kind.NOT_FOUND, error.kind());
        verifyNoInteractions(eligibility);
        verify(registrations, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", " learner", "learner ", "\u00a0learner", "learner\u2003"})
    void invalidLearnerIdentifierIsRejected(String learnerId) {
        RequestException error = assertThrows(RequestException.class, () -> service.register("course", learnerId));

        assertEquals(RequestException.Kind.INVALID, error.kind());
        verifyNoInteractions(eligibility);
        verify(registrations, never()).save(any());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", " course", "course ", "\u00a0course", "course\u2003"})
    void invalidOfferingIdentifierIsRejected(String offeringId) {
        RequestException error = assertThrows(RequestException.class, () -> service.register(offeringId, "learner"));

        assertEquals(RequestException.Kind.INVALID, error.kind());
        verifyNoInteractions(eligibility);
        verify(registrations, never()).save(any());
    }

    @Test
    void duplicateWaitlistRequestRetainsItsPosition() {
        Registration earlier = new Registration("course", "earlier", RegistrationStatus.WAITLISTED, 1L);
        Registration existing = new Registration("course", "learner", RegistrationStatus.WAITLISTED, 2L);
        when(registrations.find("course", "learner")).thenReturn(Optional.of(existing));
        when(registrations.waitlistInOrder("course")).thenReturn(List.of(earlier, existing));

        RegistrationView result = service.register("course", "learner");

        assertFalse(result.created());
        assertEquals(RegistrationStatus.WAITLISTED, result.status());
        assertEquals(2, result.waitlistPosition());
        verify(registrations, never()).save(any());
        verifyNoInteractions(eligibility);
    }

    @Test
    void zeroCapacityPlacesEligibleLearnerOnWaitlist() {
        when(offerings.findById("course")).thenReturn(Optional.of(new Offering("course", 0)));
        when(eligibility.isEligible("course", "learner")).thenReturn(true);
        when(registrations.countEnrolled("course")).thenReturn(0L);
        when(registrations.nextWaitlistSequence("course")).thenReturn(1L);
        Registration waiting = new Registration("course", "learner", RegistrationStatus.WAITLISTED, 1L);
        when(registrations.waitlistInOrder("course")).thenReturn(List.of(waiting));

        RegistrationView result = service.register("course", "learner");

        assertEquals(RegistrationStatus.WAITLISTED, result.status());
        assertEquals(1, result.waitlistPosition());
        verify(registrations).save(waiting);
    }

    @Test
    void droppedLearnerCanRequestEnrollmentAgain() {
        Registration dropped = new Registration("course", "learner", RegistrationStatus.DROPPED, null);
        when(registrations.find("course", "learner")).thenReturn(Optional.of(dropped));
        when(eligibility.isEligible("course", "learner")).thenReturn(true);
        when(registrations.countEnrolled("course")).thenReturn(0L);

        RegistrationView result = service.register("course", "learner");

        assertTrue(result.created());
        assertEquals(RegistrationStatus.ENROLLED, result.status());
        verify(registrations).save(new Registration("course", "learner", RegistrationStatus.ENROLLED, null));
    }

    @Test
    void dropSkipsIneligibleWaiterAndPromotesFirstEligibleOnly() {
        Registration enrolled = new Registration("course", "learner", RegistrationStatus.ENROLLED, null);
        Registration ineligible = new Registration("course", "first", RegistrationStatus.WAITLISTED, 1L);
        Registration eligible = new Registration("course", "second", RegistrationStatus.WAITLISTED, 2L);
        Registration later = new Registration("course", "third", RegistrationStatus.WAITLISTED, 3L);
        when(registrations.find("course", "learner")).thenReturn(Optional.of(enrolled));
        when(registrations.waitlistInOrder("course")).thenReturn(List.of(ineligible, eligible, later));
        when(eligibility.isEligible("course", "first")).thenReturn(false);
        when(eligibility.isEligible("course", "second")).thenReturn(true);
        when(registrations.countEnrolled("course")).thenReturn(1L);

        DropResult result = service.drop("course", "learner");

        assertEquals("second", result.promotedLearnerId());
        assertEquals(1, result.skippedIneligibleCount());
        assertEquals(0, result.remainingCapacity());
        verify(registrations).save(enrolled.transitionTo(RegistrationStatus.DROPPED));
        verify(registrations).save(ineligible.transitionTo(RegistrationStatus.INELIGIBLE));
        verify(registrations).save(eligible.transitionTo(RegistrationStatus.ENROLLED));
        verify(registrations, never()).save(later.transitionTo(RegistrationStatus.ENROLLED));
        verify(eligibility, never()).isEligible("course", "third");
    }

    @Test
    void dropWithoutWaitersLeavesSeatAvailable() {
        Registration enrolled = new Registration("course", "learner", RegistrationStatus.ENROLLED, null);
        when(registrations.find("course", "learner")).thenReturn(Optional.of(enrolled));
        when(registrations.waitlistInOrder("course")).thenReturn(List.of());
        when(registrations.countEnrolled("course")).thenReturn(0L);

        DropResult result = service.drop("course", "learner");

        assertNull(result.promotedLearnerId());
        assertEquals(0, result.skippedIneligibleCount());
        assertEquals(1, result.remainingCapacity());
        verify(registrations).save(enrolled.transitionTo(RegistrationStatus.DROPPED));
        verifyNoInteractions(eligibility);
    }

    @Test
    void droppingAWaitlistedLearnerFailsWithoutMutatingTheQueue() {
        Registration waiting = new Registration("course", "learner", RegistrationStatus.WAITLISTED, 1L);
        when(registrations.find("course", "learner")).thenReturn(Optional.of(waiting));

        RequestException error = assertThrows(RequestException.class, () -> service.drop("course", "learner"));

        assertEquals(RequestException.Kind.CONFLICT, error.kind());
        verify(registrations, never()).save(any());
        verifyNoInteractions(eligibility);
    }

    @Test
    void eligibilityFailureAbortsDropBeforeAnyMutation() {
        Registration enrolled = new Registration("course", "learner", RegistrationStatus.ENROLLED, null);
        Registration waiting = new Registration("course", "first", RegistrationStatus.WAITLISTED, 1L);
        when(registrations.find("course", "learner")).thenReturn(Optional.of(enrolled));
        when(registrations.waitlistInOrder("course")).thenReturn(List.of(waiting));
        when(eligibility.isEligible("course", "first")).thenThrow(new IllegalStateException("Eligibility unavailable"));

        assertThrows(IllegalStateException.class, () -> service.drop("course", "learner"));

        verify(registrations, never()).save(any());
    }

    @Test
    void dropPromotesEarliestEligibleLearnerWhenSeveralAreWaiting() {
        Registration enrolled = new Registration("course", "learner", RegistrationStatus.ENROLLED, null);
        Registration first = new Registration("course", "first", RegistrationStatus.WAITLISTED, 1L);
        Registration second = new Registration("course", "second", RegistrationStatus.WAITLISTED, 2L);
        when(registrations.find("course", "learner")).thenReturn(Optional.of(enrolled));
        when(registrations.waitlistInOrder("course")).thenReturn(List.of(first, second));
        when(eligibility.isEligible("course", "first")).thenReturn(true);
        when(registrations.countEnrolled("course")).thenReturn(1L);

        DropResult result = service.drop("course", "learner");

        assertEquals("first", result.promotedLearnerId());
        assertEquals(0, result.remainingCapacity());
        verify(registrations).save(first.transitionTo(RegistrationStatus.ENROLLED));
        verify(registrations, never()).save(second.transitionTo(RegistrationStatus.ENROLLED));
    }

    @Test
    void dropWithOnlyIneligibleWaitersLeavesSeatAvailable() {
        Registration enrolled = new Registration("course", "learner", RegistrationStatus.ENROLLED, null);
        Registration first = new Registration("course", "first", RegistrationStatus.WAITLISTED, 1L);
        Registration second = new Registration("course", "second", RegistrationStatus.WAITLISTED, 2L);
        when(registrations.find("course", "learner")).thenReturn(Optional.of(enrolled));
        when(registrations.waitlistInOrder("course")).thenReturn(List.of(first, second));
        when(eligibility.isEligible("course", "first")).thenReturn(false);
        when(eligibility.isEligible("course", "second")).thenReturn(false);
        when(registrations.countEnrolled("course")).thenReturn(0L);

        DropResult result = service.drop("course", "learner");

        assertNull(result.promotedLearnerId());
        assertEquals(2, result.skippedIneligibleCount());
        assertEquals(1, result.remainingCapacity());
        verify(registrations).save(first.transitionTo(RegistrationStatus.INELIGIBLE));
        verify(registrations).save(second.transitionTo(RegistrationStatus.INELIGIBLE));
    }

    @Test
    void droppingUnknownLearnerIsRejected() {
        RequestException error = assertThrows(RequestException.class, () -> service.drop("course", "learner"));

        assertEquals(RequestException.Kind.CONFLICT, error.kind());
        verify(registrations, never()).save(any());
    }

    @Test
    void repeatedDropDoesNotReleaseAnotherSeat() {
        Registration enrolled = new Registration("course", "learner", RegistrationStatus.ENROLLED, null);
        Registration dropped = enrolled.transitionTo(RegistrationStatus.DROPPED);
        when(registrations.find("course", "learner")).thenReturn(Optional.of(enrolled), Optional.of(dropped));
        when(registrations.waitlistInOrder("course")).thenReturn(List.of());
        when(registrations.countEnrolled("course")).thenReturn(0L);

        DropResult first = service.drop("course", "learner");
        RequestException repeated = assertThrows(RequestException.class, () -> service.drop("course", "learner"));

        assertEquals(1, first.remainingCapacity());
        assertEquals(RequestException.Kind.CONFLICT, repeated.kind());
        verify(registrations, times(1)).save(dropped);
    }
}
