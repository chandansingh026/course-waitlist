# Requirements traceability matrix

Source: the supplied *Focused AI-Assisted Engineering Assessment: Course
Waitlist*. AC1–AC7 refer to its numbered acceptance criteria. The user approved
re-enrollment after a drop and later requested removal of H2, restoring the
assessment's in-memory fallback.

| ID / source | Requirement | Implementation | Verification |
|---|---|---|---|
| F1 / AC1 | Enroll an eligible learner when capacity remains. | [EnrollmentService](../src/main/java/com/example/coursewaitlist/application/EnrollmentService.java) validates eligibility and capacity under the store lock. | `EnrollmentServiceUnitTest`, `EnrollmentServiceTest`, MVC lifecycle and Cucumber enrollment scenario. |
| F2 / AC2 | Waitlist eligible learners when full; duplicate requests create no duplicate records. | The service returns an existing active registration or allocates a FIFO sequence; the [store](../src/main/java/com/example/coursewaitlist/infrastructure/memory/InMemoryRegistrationStore.java) keys registrations by learner. | `EnrollmentServiceUnitTest`, `EnrollmentApiIntegrationTest`, `InMemoryRegistrationStoreTest` and concurrent registration tests. |
| F3 / AC3 | Dropping an enrolled learner releases a seat and promotes the earliest eligible waitlisted learner. | The service scans the ordered waitlist and publishes drop and promotion together. | `EnrollmentServiceTest`, `EnrollmentApiIntegrationTest`, concurrency tests and Cucumber FIFO scenarios. |
| F4 / AC4 | Do not enroll or promote an ineligible learner; apply the documented skip policy. | The [fixture eligibility policy](../src/main/java/com/example/coursewaitlist/infrastructure/memory/FixtureEligibilityPolicy.java) is checked on registration and promotion; ineligible waiters become `INELIGIBLE`. | `EnrollmentServiceUnitTest`, `EnrollmentServiceTest` and Cucumber prerequisite scenarios. |
| F5 / AC5 | Invalid requests and duplicate drops return clear results without corrupting state. | [Request validation](../src/main/java/com/example/coursewaitlist/api/RegistrationRequest.java), shared identifier checks and the [ProblemDetail handler](../src/main/java/com/example/coursewaitlist/api/ApiExceptionHandler.java) map errors; failed store operations discard staged changes. | `RegistrationControllerTest`, `ApiFailureHandlingTest`, `EnrollmentApiIntegrationTest` and `InMemoryRegistrationStoreTest`. |
| F6 / AC6 | Test capacity, duplicates, FIFO, eligibility, invalid drops and practical concurrency. | JUnit unit and in-memory integration tests, MVC tests, and executable [Gherkin scenarios](../src/test/resources/features/course-waitlist.feature). | Full suite below; `EnrollmentConcurrencyTest` repeats coordinated contention and atomic-failure cases. |
| F7 / AC7 | Document AI prompts, responses, dispositions and verification. | [AI usage record](ai-usage.md) and [prompt summary](../memory-bank/ai-prompt-history.md). | Review record against the work and command results; independent human review remains a reviewer responsibility. |
| N1 / Deliverables | Runnable scoped code and tests with setup instructions. | Maven Wrapper, [README](../README.md), implementation and tests. | Execute the documented commands. |
| N2 / AC1–5 | Maintain capacity, uniqueness, eligibility and queue invariants. | Shared store monitor, copy-on-success updates, domain checks and one map entry per learner. | `EnrollmentConcurrencyTest`, `InMemoryRegistrationStoreTest`, service and API tests. |
| N3 / Candidate expectations | Reuse starter conventions if supplied; otherwise use the minimal fallback. | No starter was supplied; the application is a single-offering in-memory service. | Review project scope and architecture. |
| N4 / Scope boundaries | Avoid unrelated product features and a new database without a starter. | [Architecture](architecture.md), scoped endpoints and in-memory store; no database dependency or schema. | Review dependencies, endpoints and scope. |
| N5 / Suggested timebox | Suggested effort of 7–8 hours. | Architecture, tests and [AI record](ai-usage.md). | Review submitted evidence; no elapsed-time claim is made. |
| SEC1 / Candidate expectations | Exclude credentials and unnecessary sensitive data from prompts and records. | [AI usage record](ai-usage.md). | Inspect the record before submission. |
| D1–D5 / Deliverables | Supply code/tests, design, AI record, run instructions and limitations. | Source and tests; [architecture](architecture.md); [AI record](ai-usage.md); [README](../README.md); [limitations](../memory-bank/limitations-and-risks.md). | Review deliverables and run the documented commands. |
| E1 / Evaluation | Prioritize correct queue/capacity behavior, focused tests and independently reviewed AI work. | Code, tests and documented verification. The referenced AI-Assisted scorecard was not supplied. | Evaluate the available evidence; exact rubric criteria and weights are unknown. |

## Additional approved decisions

- One active registration per learner/offering; an existing active request is
  idempotent and keeps its queue position. After a drop, a new request is allowed.
- Eligibility is a demo fixture; clients cannot assert their own eligibility.
- `ENROLLED`, `WAITLISTED`, `DROPPED`, and `INELIGIBLE` are the stored statuses.
  Only the latest registration is retained; there is no audit history.
- One in-memory store serializes operations within a process. Changes and
  sequence allocation are published only on success; restart discards all state.

Additional supporting documentation: [API](api.md), [data design](database-design.md),
[test scenarios](test-scenarios.md), and
[test execution evidence](../memory-bank/test-execution-evidence.md).

## BDD scenario traceability

The tags in the [feature file](../src/test/resources/features/course-waitlist.feature)
refer to the requirement IDs and assessment acceptance criteria above.

| Scenario | Requirement / business rule |
|---|---|
| An eligible learner enrolls when a seat is available | F1 / AC1 |
| An eligible learner joins the waitlist when the course is full | F2 / AC2 |
| Duplicate enrollment and waitlist requests preserve the existing registrations | F2 / AC2; active registrations are idempotent |
| An ineligible learner cannot enroll | F4 / AC4 |
| Dropping an enrolled learner releases the seat for a waiting learner | F3 / AC3 |
| A drop promotes the earliest learner in FIFO order | F3 / AC3 |
| Skip an ineligible queue head and reject a repeated drop | F4 / AC4, F5 / AC5 |
| Dropping with an empty waitlist leaves a seat available | F3 / AC3 |
| Concurrent enrollment attempts respect capacity and prevent duplicates | F2, F6, N2 |
| Concurrent drop requests promote the first waiter only once | F3, F5, F6, N2 |

## Verification

The latest in-memory suite, `mvn -B -ntp test`, passed 101 tests on
2026-11-20 after the OpenAPI contract update; see
[test execution evidence](../memory-bank/test-execution-evidence.md).

Historical H2 results are recorded in the [AI usage record](ai-usage.md); they
describe an earlier implementation and do not verify the current in-memory
store.

Earlier on 2026-11-19, before the default capacity and fixture update:

```powershell
.\mvnw.cmd -B -ntp clean test
```

Result: **BUILD SUCCESS**, 99 tests, 0 failures, 0 errors, 0 skipped. The
suite includes 25 repeated concurrency executions, service-level
failure-after-save tests, full-context HTTP tests, and 10 Cucumber scenarios.
Mockito reported a non-failing dynamic-agent warning.

The following dependency-tree check also succeeded with no matching H2, Spring
JDBC starter or Spring JDBC artifacts:

```powershell
.\mvnw.cmd -B -ntp dependency:tree '-Dincludes=com.h2database:h2,org.springframework.boot:spring-boot-starter-jdbc,org.springframework:spring-jdbc'
```

### Historical H2 verification

These commands were run on 2026-11-19 before removing H2. Their results
describe the earlier implementation; some selectors refer to removed tests.

| Command | Result |
|---|---|
| `.\mvnw.cmd -B -ntp '-Dtest=EnrollmentServiceUnitTest,EnrollmentServiceTest,RegistrationControllerTest,JdbcSchemaTest,CucumberTest' test` | 21 passed |
| `.\mvnw.cmd -B -ntp '-Dtest=EnrollmentServiceUnitTest,EnrollmentServiceTest,RegistrationControllerTest,CucumberTest' test` | 24 passed |
| `.\mvnw.cmd -B -ntp '-Dtest=EnrollmentServiceUnitTest,IdentifierTest,OfferingTest' test` | 33 passed |
| `.\mvnw.cmd -B -ntp '-Dtest=EnrollmentApiIntegrationTest,EnrollmentServiceTest,JdbcSchemaTest,RegistrationControllerTest,ApiFailureHandlingTest,HttpServerTest,CucumberTest' test` | 30 passed |
| `.\mvnw.cmd -B -ntp '-Dtest=CucumberTest' test` | 10 passed |
| `.\mvnw.cmd -B -ntp '-Dtest=EnrollmentConcurrencyTest,EnrollmentServiceTest,EnrollmentApiIntegrationTest,CucumberTest' test` | 50 passed |
| `.\mvnw.cmd -B -ntp compile test` | 102 passed |

Each run above reported zero failures, errors or skips.
