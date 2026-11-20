# Test scenarios

The executable [Gherkin feature](../src/test/resources/features/course-waitlist.feature)
and [requirements matrix](requirements-traceability.md#bdd-scenario-traceability)
map each BDD scenario to an assessment requirement or approved rule.

| Scenario | Verification |
|---|---|
| Available seat; zero or full capacity | `EnrollmentServiceUnitTest`, `EnrollmentServiceTest`, Gherkin |
| Duplicate enrolled and waitlisted requests | Service and API tests; Gherkin; repeated concurrency test |
| Missing, invalid and ineligible requests | `IdentifierTest`, `RegistrationControllerTest`, service tests |
| Drop, repeated or invalid drop | Service and API tests; Gherkin |
| FIFO; ineligible head; no eligible waiter | Service tests and Gherkin |
| Re-enrollment after a drop | `EnrollmentServiceTest`, `EnrollmentApiIntegrationTest` |
| Failure after staged registration or promotion | `EnrollmentServiceTest`, `InMemoryRegistrationStoreTest` |
| Last-seat contention, duplicate requests and drops, concurrent FIFO promotions | `EnrollmentConcurrencyTest` and Gherkin |
| HTTP errors and unexpected failure sanitization | `RegistrationControllerTest`, `ApiFailureHandlingTest` |
| Actual local HTTP and OpenAPI endpoint | `HttpServerTest`, `RegistrationControllerTest` |

Full-context API tests inspect the real in-memory store, not only responses.
Service tests use Mockito for some eligibility behavior. The API failure-handler
slice uses a mocked service. Concurrency tests use latches and bounded waits;
five cases run five times each. These tests are not exhaustive proofs of all
thread schedules.

Run commands and actual recorded output are in
[test execution evidence](../memory-bank/test-execution-evidence.md).
