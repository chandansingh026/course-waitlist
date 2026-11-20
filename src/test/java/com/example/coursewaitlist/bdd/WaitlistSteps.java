package com.example.coursewaitlist.bdd;

import static com.example.coursewaitlist.support.ConcurrentTestSupport.runConcurrently;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.coursewaitlist.application.DropResult;
import com.example.coursewaitlist.application.EnrollmentService;
import com.example.coursewaitlist.application.RegistrationView;
import com.example.coursewaitlist.application.RequestException;
import com.example.coursewaitlist.domain.RegistrationStatus;
import com.example.coursewaitlist.support.TestStore;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

public class WaitlistSteps {
    private final Set<String> eligible = new HashSet<>();
    private EnrollmentService service;
    private EnrollmentService secondService;
    private TestStore store;
    private RegistrationView result;
    private DropResult drop;
    private RequestException error;
    private List<RegistrationView> concurrentRegistrations;
    private int expectedRegistrationAttempts;
    private List<DropAttempt> concurrentDrops;

    @Given("an offering with capacity {int}")
    public void offeringWithCapacity(int capacity) {
        store = new TestStore(capacity);
        service = store.service((offering, learner) -> eligible.contains(learner));
        secondService = store.service((offering, learner) -> eligible.contains(learner));
    }

    @Given("{string} satisfies the prerequisite")
    public void learnerIsEligible(String learner) {
        eligible.add(learner);
    }

    @Given("{string} no longer satisfies the prerequisite")
    public void learnerIsIneligible(String learner) {
        eligible.remove(learner);
    }

    @When("{string} requests registration")
    public void register(String learner) {
        result = service.register("course", learner);
    }

    @When("{string} requests an ineligible registration")
    public void registerIneligible(String learner) {
        error = assertThrows(RequestException.class, () -> service.register("course", learner));
    }

    @When("{string} is dropped")
    public void drop(String learner) {
        drop = service.drop("course", learner);
    }

    @When("{string} is dropped again")
    public void dropAgain(String learner) {
        error = assertThrows(RequestException.class, () -> service.drop("course", learner));
    }

    @When("{string} and {string} each make {int} concurrent registration requests")
    public void concurrentRegistrations(String first, String second, int attemptsPerLearner) throws Exception {
        List<Callable<RegistrationView>> requests = new ArrayList<>();
        for (int i = 0; i < attemptsPerLearner; i++) {
            EnrollmentService selected = i % 2 == 0 ? service : secondService;
            requests.add(() -> selected.register("course", first));
            requests.add(() -> selected.register("course", second));
        }
        expectedRegistrationAttempts = requests.size();
        concurrentRegistrations = runConcurrently(requests);
    }

    @When("{int} requests concurrently drop {string}")
    public void concurrentDrops(int requests, String learner) throws Exception {
        List<Callable<DropAttempt>> attempts = new ArrayList<>();
        for (int i = 0; i < requests; i++) {
            EnrollmentService selected = i % 2 == 0 ? service : secondService;
            attempts.add(() -> {
                try {
                    return new DropAttempt(selected.drop("course", learner), null);
                } catch (RequestException ex) {
                    return new DropAttempt(null, ex.kind());
                }
            });
        }
        concurrentDrops = runConcurrently(attempts);
    }

    @Then("the result status is {string}")
    public void resultStatus(String status) {
        assertEquals(RegistrationStatus.valueOf(status), result.status());
    }

    @Then("the request returns the existing registration")
    public void existingRegistration() {
        assertFalse(result.created());
    }

    @Then("{string} has status {string}")
    public void learnerHasStatus(String learner, String status) {
        assertEquals(RegistrationStatus.valueOf(status), service.getRegistration("course", learner).status());
    }

    @Then("{string} has waitlist position {int}")
    public void learnerHasPosition(String learner, int position) {
        assertEquals(position, service.getRegistration("course", learner).waitlistPosition());
    }

    @Then("there are {int} enrolled learners")
    public void enrolledCount(int count) {
        assertEquals(count, store.countEnrolled());
    }

    @Then("there are {int} waitlisted learners")
    public void waitlistedCount(int count) {
        assertEquals(count, store.waitlist().size());
    }

    @Then("no learner is promoted")
    public void noPromotion() {
        assertNull(drop.promotedLearnerId());
    }

    @Then("remaining capacity is {int}")
    public void remainingSeats(int count) {
        assertEquals(count, drop.remainingCapacity());
    }

    @Then("all concurrent registration requests succeed")
    public void allRegistrationsSucceed() {
        assertEquals(expectedRegistrationAttempts, concurrentRegistrations.size());
    }

    @Then("exactly {int} concurrent registrations are created")
    public void registrationsCreated(int count) {
        assertEquals(count, concurrentRegistrations.stream().filter(RegistrationView::created).count());
    }

    @Then("exactly {int} concurrent drop succeeds and {int} fails with {string}")
    public void concurrentDropOutcomes(int successes, int failures, String kind) {
        assertEquals(successes + failures, concurrentDrops.size());
        assertEquals(successes, concurrentDrops.stream().filter(attempt -> attempt.result() != null).count());
        assertEquals(failures, concurrentDrops.stream()
                .filter(attempt -> attempt.error() == RequestException.Kind.valueOf(kind)).count());
        drop = concurrentDrops.stream().map(DropAttempt::result).filter(java.util.Objects::nonNull).findFirst()
                .orElseThrow();
    }

    @Then("{string} is promoted")
    public void promoted(String learner) {
        assertEquals(learner, drop.promotedLearnerId());
        assertEquals(RegistrationStatus.ENROLLED, service.getRegistration("course", learner).status());
    }

    @Then("{string} is removed as ineligible")
    public void removedAsIneligible(String learner) {
        assertTrue(drop.skippedIneligibleCount() > 0);
        assertEquals(RegistrationStatus.INELIGIBLE, service.getRegistration("course", learner).status());
    }

    @Then("the request fails with {string}")
    public void requestFails(String kind) {
        assertEquals(RequestException.Kind.valueOf(kind), error.kind());
    }

    private record DropAttempt(DropResult result, RequestException.Kind error) {
    }
}
