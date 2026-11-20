# AI prompt and use record

This record covers substantial AI-assisted work in the available session.
Prompt blocks reproduce the user's messages verbatim; they are not AI response
quotations. Suggestions and outcomes are summaries of the recorded work.
Approval of an approach does not establish independent human review of its
implementation. No credentials or unnecessary sensitive details are included.

## Evidence and implementation status

**Current authoritative implementation:** a single-process, synchronized
[in-memory store](../src/main/java/com/example/coursewaitlist/infrastructure/memory/InMemoryRegistrationStore.java),
with staged changes published on success. There is no H2 dependency in the
current project. State is lost on restart.

The latest test run, `mvn -B -ntp test`, succeeded on 2026-11-20 after the
OpenAPI contract update: **101 tests, 0 failures, 0 errors, 0 skipped**. The
previous `mvn -B -ntp test` run passed 100 tests after the default capacity
and fixture update. The earlier `mvn -B -ntp clean package` build passed 99
tests before those updates. Its [build log](../api-execution-evidence/00-environment/maven-clean-package.log)
and [Surefire reports](../api-execution-evidence/00-environment/automated-test-reports/)
are available. The [live API report](../api-execution-evidence/FINAL-API-EXECUTION-REPORT.md)
records 21 passing scenarios, zero failures, and TC12 not executed. The earlier
current-store `clean test`, `test`, and `verify` results are in
[test execution evidence](../memory-bank/test-execution-evidence.md).

**Superseded history:** H2 and JDBC were temporarily implemented at the user's
request, then removed. Their schema, locking approach, database-specific tests,
and H2-era test results describe the earlier implementation only. They are not
evidence that the current in-memory store was tested.

## Review examples

- **Edited:** the earlier AI suggestion of Java 25 was changed to Java 21,
  and optional Gherkin was changed to executable Cucumber. The user selected
  the alternatives; the available record does not state the reason for each
  selection. The approved stack is in [the POM](../pom.xml); the current build
  and Cucumber reports provide execution evidence.
- **Edited:** after a later request for JUnit 5, the user chose to keep the
  approved JUnit Jupiter 6 and Mockito stack rather than downgrade. The reason
  beyond retaining the approved stack is not recorded. The current build ran
  the tests; see the Surefire reports above.
- **Reversed:** H2 was initially used at the user's request, despite the
  assessment's no-new-database boundary. The AI review identified that
  boundary as a high assessment-scope issue; the user then requested H2
  removal to fix it. The in-memory store and subsequent test runs are the verification.
- **Edited after review:** the AI review found that `Registration` accepted
  invalid identifiers and non-positive queue sequences. Constructor checks
  and regression tests were added. The historical H2-era full suite passed
  102 tests; the subsequent in-memory suite passed 99. This was an AI review, not an
  independent human review.

## 1. Assessment analysis

**Exact prompt:**

```text
I am attaching the original assessment/requirement document.

Act as a Senior Java Backend Engineer, Solution Architect, QA Engineer, and Security Engineer.

There is no existing starter project or source code. The entire application must be developed from scratch.

First, read the complete attached document and analyze it.

Provide:

1. Problem statement and business objectives.
2. Functional requirements.
3. Non-functional requirements.
4. Business rules and constraints.
5. Mandatory acceptance criteria.
6. Expected deliverables.
7. Security requirements.
8. Testing expectations.
9. Evaluation criteria.
10. Ambiguities, missing information, and assumptions.
Create a requirements traceability matrix mapping each requirement to its expected implementation and verification.

Do not write code or create project files yet.

Treat the attached document as the single source of truth. Ask me only the questions necessary to clarify missing decisions.

Wait for my approval.
```

- **Suggestion:** limit the implementation to one offering's enrollment, drop,
  eligibility, and FIFO behavior; identify the missing scorecard and assumptions.
- **Disposition / affected:** accepted as the scope basis; informed
  [requirements analysis](../memory-bank/requirements-analysis.md) and
  [traceability](requirements-traceability.md).
- **Verification:** compared with the supplied assessment; later tests were
  mapped to its acceptance criteria. No code was created in that analysis step.

## 2. Stack selection

**Exact prompt:**

```text
Based on the assessment analysis, help me finalize the technical stack and project decisions.

Recommend and explain:

1. Java version.
2. Spring Boot version.
3. Maven or Gradle.
4. In memory storage.
5. REST API design.
6. Authentication and authorization approach, if required.
7. Validation and exception-handling strategy.
8. Logging approach.
9. JUnit 5 and Mockito testing strategy.
10. Integration testing strategy.
11. BDD/Gherkin approach.
12. Concurrency testing approach.
15. API documentation approach.
For each decision, explain why it is appropriate for this assessment.

Distinguish assessment requirements from your recommendations.

Ask me for approval of the proposed technical stack before proceeding.
```

- **Suggestion:** Java, Spring Boot, Maven, REST, in-memory storage and test
  tooling; the earlier suggested Java 25 and optional Gherkin were alternatives.
- **Disposition / affected:** edited to Java 21 and executable Cucumber; the
  user approved Spring Boot 4.1.2, Maven Wrapper and JUnit Jupiter 6. The
  rationale for each change was not recorded. See [the POM](../pom.xml).
- **Verification:** the current build and Surefire reports above exercise the
  selected dependencies. REST and OpenAPI were choices, not assessment mandates.

## 3. Architecture and re-enrollment

**Exact prompt:**

```text
Design the complete application architecture from scratch using the approved technology stack.

Provide:

1. High-level architecture diagram.
2. Maven project folder structure.
3. Package structure.
4. Domain model.
5. Entity relationships.
6. Database schema.
7. REST API endpoints with request and response examples.
8. Service-layer responsibilities.
9. Repository responsibilities.
10. Transaction boundaries.
11. Concurrency-control strategy.
12. Exception-handling architecture.
13. Security design.
14. Enrollment and drop workflows.
15. FIFO waitlist promotion algorithm.
16. Design decisions and trade-offs.
Pay particular attention to preventing over-enrollment, duplicate enrollments, and duplicate waitlist promotions.

Document assumptions and identify unresolved business rules.

Do not implement code yet. Wait for my approval.
```

**Exact follow-up:**

```text
Yes a a learner may re-enroll after dropping
```

- **Suggestion:** one offering, one active registration per learner, a shared
  lock, eligibility recheck, and skipping/removing ineligible waiters.
- **Disposition / affected:** architecture accepted; the user clarified that a
  dropped learner may re-enroll. This affected
  [architecture](architecture.md), the service and tests.
- **Verification:** `EnrollmentServiceTest` and
  `EnrollmentApiIntegrationTest` cover re-enrollment, FIFO and eligibility;
  the current suite and live API report record actual results.

## 4. Initial project

**Exact prompt:**

```text
Now create the complete Spring Boot Maven project from scratch using the approved architecture and technology stack.

Requirements:

1. Generate the complete Maven project.
2. Create the package structure.
3. Configure dependencies.
4. Configure the database.
5. Configure application properties.
6. Create domain entities.
7. Create repositories.
8. Create service interfaces and implementations where appropriate.
9. Create controllers.
10. Configure validation and exception handling.
11. Add API documentation configuration if required.
12. Add initial automated test structure.
13. Add README.md with setup instructions.
14. Add the approved architecture documentation.
Use clean code, appropriate naming, and maintainable design.

Do not implement unrelated features.

After creation:

- Show the project structure.
- Explain important dependencies.
- Run Maven compilation.
- Fix compilation errors.
- Report the actual result.
Do not claim success without executing the command.
```

- **Suggestion:** create the approved bounded Spring Boot service and use the
  assessment's in-memory fallback rather than introduce a database.
- **Disposition / affected:** implemented the project, tests and
  [README](../README.md); the original schema was conceptual.
- **Verification:** the recorded pre-H2 `.\mvnw.cmd -B -ntp clean compile`
  succeeded with 19 sources; `.\mvnw.cmd -B -ntp test` passed 14 tests.
  Those results are historical; see current evidence above.

## 5. H2 change — superseded

**Exact prompt:**

```text
let's use h2 database
```

- **Suggestion:** use embedded in-memory H2 with Spring JDBC and an offering-row
  lock for compound operations.
- **Disposition / affected:** implemented at the user's request, despite the
  assessment boundary. H2 dependencies, schema, repositories and tests were
  later removed or adapted; this is not the current design.
- **Verification:** the historical H2 `clean test` passed 17 tests. It does
  not verify the current in-memory store.

## 6. Enrollment — H2-era work superseded

**Exact prompt:**

```text
Implement the course enrollment functionality according to the approved requirements and architecture.

Cover:

1. Learner enrollment request.
2. Course capacity validation.
3. Prerequisite eligibility validation.
4. Duplicate active enrollment prevention.
5. Automatic waitlist placement when the course is full.
6. Enrollment status management.
7. Database persistence.
8. Transaction management.
9. Appropriate API responses.
10. Error handling.
Ensure that the implementation follows the approved domain and service design.

Consider concurrent enrollment requests and prevent the course capacity from being exceeded.

Write focused unit tests for all important scenarios.

Run the relevant tests and report actual results.

Update the documentation and requirements traceability matrix.
```

- **Suggestion:** retain the existing enrollment workflow and add focused
  unit, H2 rollback and API verification.
- **Disposition / affected:** implemented test and traceability additions;
  the H2 portions were later superseded. Current behavior is in
  [EnrollmentService](../src/main/java/com/example/coursewaitlist/application/EnrollmentService.java).
- **Verification:** the historical focused run passed 21 tests; current
  verification is the 99-test build and live report above.

## 7. Drop and FIFO promotion — H2-era work superseded

**Exact prompt:**

```text
Implement course dropping and FIFO waitlist promotion.

Requirements:

1. Allow an enrolled learner to drop the course.
2. Prevent invalid or repeated drops from corrupting data.
3. Identify the earliest eligible learner in the waitlist.
4. Promote the learner according to FIFO order.
5. Revalidate eligibility at promotion time if required by the approved business rules.
6. Ensure ineligible waitlisted learners are handled according to the approved policy.
7. Maintain correct enrollment and waitlist statuses.
8. Ensure course capacity remains correct.
9. Execute drop and promotion atomically.
10. Prevent concurrent requests from promoting the same learner twice.
Add unit tests covering successful, unsuccessful, and boundary scenarios.

Run all relevant tests and report actual results.

Update architecture and implementation documentation if the design changes.
```

- **Suggestion:** keep the existing drop/promotion workflow and add boundary,
  API and concurrent-drop checks.
- **Disposition / affected:** test and documentation additions implemented;
  H2 locking was later replaced by the in-memory store.
- **Verification:** the historical focused command passed 24 tests; the
  current service and live FIFO cases were subsequently exercised.

## 8. Validation and errors

**Exact prompt:**

```text
Review and implement consistent API validation and exception handling across the application.

Cover:

- Missing or invalid request parameters.
- Invalid learner identifiers.
- Invalid course identifiers.
- Duplicate enrollment.
- Course capacity exceeded or waitlist placement.
- Prerequisite failures.
- Invalid drop requests.
- Resource not found.
- Database and transaction failures.
- Appropriate HTTP status codes.
- Consistent error response structure.
Ensure internal exceptions and sensitive implementation details are not exposed to API consumers.

Add tests for positive and negative API scenarios.

Run the tests and update documentation.
```

- **Suggestion:** reject non-string JSON IDs and surrounding Unicode whitespace,
  and test sanitized `ProblemDetail` responses.
- **Disposition / affected:** implemented in the API, identifier validation,
  tests and [API documentation](api.md); duplicate/waitlist behavior retained.
- **Verification:** the H2-era focused command passed 32 tests, historically.
  Current API and failure-handler tests passed in the 99-test build; live
  negative cases passed as recorded in the API report.

## 9. Unit tests

**Exact prompt:**

```text
Create comprehensive unit tests using JUnit 5 and Mockito.

Cover the complete business logic.

Enrollment scenarios:

- Successful enrollment.
- Course with available capacity.
- Course at full capacity.
- Duplicate enrollment.
- Ineligible learner.
- Invalid course.
- Invalid learner.
Drop scenarios:

- Successful drop.
- Invalid drop.
- Repeated drop.
- Automatic promotion after dropping.
Waitlist scenarios:

- FIFO promotion.
- Multiple waitlisted learners.
- Ineligible learner at the queue head.
- Empty waitlist.
- No available seat.
Include boundary conditions and negative scenarios.

Follow Arrange-Act-Assert structure.

Avoid unnecessary mocking and implementation-dependent assertions.

Run all unit tests and report actual results.
```

- **Suggestion:** expand focused service and domain tests and retain the approved
  JUnit Jupiter 6 / Mockito stack rather than downgrade.
- **Disposition / affected:** user chose to retain Jupiter 6; tests were added
  to the service and domain test files. The reason beyond retaining the
  approved stack is not recorded.
- **Verification:** 33 isolated unit executions passed in the earlier focused
  run. Current results are in the 99-test Surefire reports.

## 10. Integration tests — H2-era implementation superseded

**Exact prompt:**

```text
Create integration tests for the complete enrollment application.

Test:

1. REST API endpoints.
2. Controller-to-service interaction.
3. Service-to-repository interaction.
4. Database persistence.
5. Transaction rollback.
6. Enrollment and drop workflows.
7. FIFO promotion.
8. Validation and exception responses.
9. Duplicate enrollment protection.
10. Database constraints.
Use the approved database testing strategy.

Verify actual database state, not only API responses.

Run the integration tests and provide execution evidence.

Do not replace integration tests with Mockito-only tests.
```

- **Suggestion:** exercise full Spring MVC through H2 with direct state and
  constraint-induced rollback assertions.
- **Disposition / affected:** implemented under the then-current H2 design;
  subsequently adapted to the real in-memory store when H2 was removed.
- **Verification:** the historical integration-focused command passed 30 tests.
  Current [EnrollmentApiIntegrationTest](../src/test/java/com/example/coursewaitlist/api/EnrollmentApiIntegrationTest.java)
  inspects in-memory state and passed in the current suite.

## 11. BDD scenarios

**Exact prompt:**

```text
Create BDD scenarios for the course enrollment and waitlist application.

Use Given-When-Then format.

Cover:

1. Enrollment when seats are available.
2. Enrollment when the course is full.
3. Duplicate enrollment.
4. Ineligible learner enrollment.
5. Successful course drop.
6. FIFO waitlist promotion.
7. Ineligible waitlisted learner.
8. Empty waitlist.
9. Concurrent enrollment attempts.
10. Concurrent drop requests.
If Cucumber is already included in the approved architecture, implement executable step definitions.

Otherwise, provide Gherkin feature files and map them to executable automated tests without adding unnecessary dependencies.

Ensure every scenario maps to a requirement or business rule.

Execute the tests and report actual results.
```

- **Suggestion:** implement executable Cucumber scenarios with requirement tags
  rather than leave Gherkin as documentation only.
- **Disposition / affected:** implemented the
  [feature](../src/test/resources/features/course-waitlist.feature), step
  definitions and [scenario mapping](requirements-traceability.md#bdd-scenario-traceability).
  H2-backed steps were later adapted to in-memory storage.
- **Verification:** the historical H2 Cucumber run passed 10 scenarios; the
  subsequent 99-test in-memory build also included 10 passing Cucumber scenarios.

## 12. Concurrency review — H2-era implementation superseded

**Exact prompt:**

```text
Perform a focused concurrency review and testing exercise.

Verify:

1. Two learners attempting to take the last available seat.
2. Multiple requests from the same learner.
3. Multiple simultaneous drops.
4. Multiple simultaneous waitlist promotions.
5. FIFO ordering under concurrent operations.
6. Transaction consistency.
7. Database locking behavior.
8. Duplicate promotion prevention.
Use deterministic concurrency tests with CountDownLatch, CyclicBarrier, or other appropriate synchronization techniques.

Avoid Thread.sleep-based tests as the primary verification mechanism.

Verify database state after concurrent operations.

Ensure capacity is never exceeded and each learner has at most one active enrollment.

Run the tests repeatedly where practical and document the actual results and limitations.
```

- **Suggestion:** coordinate competing requests and check queue, capacity,
  promotion and rollback state; define FIFO by successful queue allocation.
- **Disposition / affected:** repeated tests and the
  [concurrency review](architecture.md#concurrency-review) were added; H2
  locking was subsequently replaced by a monitor and staged copies.
- **Verification:** the historical H2-focused run passed 50 tests, including
  25 repeated concurrency executions. The subsequent 99-test in-memory run included 25
  in-memory concurrency executions; live concurrent HTTP results are in the
  API report. Neither proves all schedules.

## 13. Code review — H2-era results superseded

**Exact prompt:**

```text
Act as an independent Senior Java Backend Code Reviewer.

Review the entire project against the original assessment.

Inspect:

- SOLID principles.
- Separation of concerns.
- Naming and readability.
- Duplicate code.
- Transaction boundaries.
- Concurrency safety.
- Database design.
- API design.
- Exception handling.
- Validation.
- Security.
- Test quality.
- Dependency usage.
- Performance risks.
- Unnecessary complexity.
Identify issues by severity.

Fix genuine defects and explain significant changes.

Do not make unnecessary refactoring changes merely for stylistic preference.

Run compilation and all automated tests after the fixes.

Provide a code review report.
```

- **Suggestion:** enforce registration identifier and positive queue-sequence
  invariants; flag H2 as an assessment-scope issue and document risks.
- **Disposition / affected:** constructor checks and
  [regression tests](../src/test/java/com/example/coursewaitlist/domain/RegistrationTest.java)
  were added. H2 remained until the next user request. This AI review is not an
  independent human review.
- **Verification:** six targeted cases were recorded failing before the fix;
  historical `.\mvnw.cmd -B -ntp compile test` passed 102 tests afterward.
  The subsequent in-memory build passed 99 tests.

## 14. Engineering documentation

**Exact prompt:**

```text
Create and synchronize the project's engineering documentation.

Create a memory-bank directory containing:

1. project-overview.md
2. requirements-analysis.md
3. architecture.md
4. design-decisions.md
5. implementation-plan.md
6. security-review.md
7. test-strategy.md
8. test-execution-evidence.md
9. ai-prompt-history.md
10. code-review.md
11. limitations-and-risks.md
12. assessment-readiness.md
Also create or update:

- README.md
- API documentation.
- Database design documentation.
- Test scenario documentation.
- Requirements traceability matrix.
Record significant AI suggestions and whether they were accepted, modified, or rejected, including the reasons and verification performed.

Do not fabricate prompt history, test output, or evidence.

Ensure all documents accurately reflect the current source code.
```

- **Suggestion:** describe the actual in-memory data model and link the
  requested documents rather than imply a current schema.
- **Disposition / affected:** documentation implemented at the user's request;
  subsequent acceptance was not recorded. See [README](../README.md),
  [data design](database-design.md) and the memory bank.
- **Verification:** source and existing test reports were compared; 12 requested
  files and 80 relative links were checked. No application tests were rerun
  solely for that documentation update.

## 15. Assessment-readiness review

**Exact prompt:**

```text
Perform the final independent assessment-readiness review.

Use the original assessment document as the primary checklist.

Verify:

1. Every functional requirement is implemented.
2. Every mandatory acceptance criterion is covered.
3. Every important business rule has an automated test.
4. Unit tests pass.
5. Integration tests pass.
6. BDD scenarios are covered as agreed.
7. Concurrency tests pass.
8. Security findings have been addressed or documented.
9. The project builds successfully.
10. The application starts successfully.
11. APIs behave as documented.
12. Database persistence works.
13. README instructions are correct.
14. Architecture and design documents match the implementation.
15. AI usage documentation is accurate.
16. Test execution evidence is genuine.
17. No unrelated functionality has been added.
Execute:

- mvn clean compile
- mvn test
- mvn verify
Use the actual configured build lifecycle and explain if any command is not applicable.

Produce a final report with:

- Requirement traceability.
- Implemented features.
- Test results.
- Build results.
- Security review summary.
- Known limitations.
- Outstanding issues.
- Project folder structure.
- Exact application startup instructions.
- API testing examples.
- Final assessment readiness.
Do not mark the project complete if mandatory requirements remain unverified.
```

- **Suggestion:** report the generated OpenAPI schema mismatch and distinguish
  AI review from unrecorded independent human review.
- **Disposition / affected:** findings documented in the
  [readiness review](final-assessment-readiness-review.md); no fix to the
  generated schema was made.
- **Verification:** `mvn -B -ntp clean compile`, `mvn -B -ntp test` and
  `mvn -B -ntp verify` succeeded; both test commands ran 99 passing tests.
  Startup and live HTTP checks succeeded. Human review remains unverified.

## 16. Live API execution

**Exact prompt:**

```text
# Live Localhost Execution and End-to-End API Testing
Act as a Senior Java Backend Engineer, API Automation Test Engineer, and QA Engineer.

The complete Spring Boot application has already been developed.

I want you to start the actual application locally and execute comprehensive end-to-end tests using real HTTP requests against the running localhost application.

IMPORTANT:

- This is live API testing, not just JUnit or Mockito testing.
- Do not use mocked API responses.
- Do not assume existing tests are sufficient.
- Do not modify the business logic unless a genuine defect is discovered.
- Do not add security implementation or security testing as a separate scope.
- Follow only the original assessment requirements and approved business rules.

## 1. Inspect the application
Identify:

- Java and Spring Boot versions.
- Build tool.
- Database configuration.
- Application port.
- All REST endpoints.
- Request and response structures.
- Existing test data requirements.
- Existing unit and integration tests.
- Application startup requirements.
Discover actual endpoint paths and payloads from the source code or API documentation.

Do not invent endpoints.

## 2. Start the application on localhost

1. Build the application.
2. Start the required database.
3. Start the actual Spring Boot application.
4. Wait for successful initialization.
5. Verify database connectivity.
6. Verify the application is accepting HTTP requests.
7. Identify and record the actual localhost base URL.
8. Keep the application running throughout API execution.
Do not assume the port is 8080. Inspect the actual configuration.

## 3. Create the evidence folder
Create the following structure at the project root:

api-execution-evidence/
│
├── 00-environment/
│   ├── environment-details.md
│   ├── application-startup.log
│   └── runtime-status.md
│
├── 01-enrollment/
│   ├── TC01-successful-enrollment/
│   ├── TC02-course-full-waitlist/
│   ├── TC03-duplicate-enrollment/
│   ├── TC04-ineligible-learner/
│   ├── TC05-invalid-course/
│   └── TC06-invalid-learner/
│
├── 02-course-dropping/
│   ├── TC07-successful-drop/
│   ├── TC08-invalid-drop/
│   ├── TC09-repeated-drop/
│   └── TC10-drop-with-empty-waitlist/
│
├── 03-waitlist-promotion/
│   ├── TC11-fifo-promotion/
│   ├── TC12-ineligible-first-learner/
│   ├── TC13-multiple-waitlisted-learners/
│   └── TC14-promotion-after-drop/
│
├── 04-concurrency/
│   ├── TC15-simultaneous-enrollment/
│   ├── TC16-duplicate-concurrent-enrollment/
│   ├── TC17-simultaneous-drops/
│   └── TC18-concurrent-promotion/
│
├── 05-validation-and-errors/
│   ├── TC19-missing-fields/
│   ├── TC20-invalid-request/
│   ├── TC21-invalid-identifiers/
│   └── TC22-unsupported-method/
│
├── 06-all-request-response-logs/
│   ├── all-api-requests.json
│   ├── all-api-responses.json
│   └── complete-execution.log
│
├── 07-database-verification/
│   ├── initial-database-state.md
│   ├── scenario-wise-database-state.md
│   └── final-database-state.md
│
├── 08-screenshots/
│   ├── application-startup/
│   ├── successful-api-calls/
│   ├── error-api-calls/
│   ├── waitlist-promotion/
│   └── concurrency/
│
└── FINAL-API-EXECUTION-REPORT.md

Adapt the scenarios to the actual assessment and implementation. Clearly document scenarios that are not applicable.

## 4. Execute real HTTP API tests
Use Postman/Newman, REST Assured, curl, or another appropriate HTTP client.

All requests must target the running localhost application.

For every API request, capture:

- Test case ID.
- Scenario name.
- HTTP method.
- Complete request URL.
- Request headers.
- Request body.
- Request timestamp.
- HTTP status code.
- Response headers.
- Complete response body.
- Response timestamp.
- Expected result.
- Actual result.
- PASS or FAIL status.
Store each scenario's request, response, and execution details separately.

Use actual responses received from the application.

## 5. Enrollment scenarios
Execute and capture:

TC01: Successful enrollment.

TC02: Course has reached capacity; learner should be placed on the waitlist.

TC03: Duplicate enrollment should be prevented.

TC04: Learner who does not meet prerequisites should be handled according to the approved business rules.

TC05: Invalid course identifier.

TC06: Invalid learner identifier.

For each scenario, verify actual database state.

## 6. Course dropping scenarios
Execute and capture:

TC07: Successful course drop.

TC08: Attempt to drop a course when the learner is not enrolled.

TC09: Repeated drop request.

TC10: Drop an enrolled learner when the waitlist is empty.

Verify enrollment status, capacity, and waitlist state after each scenario.

## 7. FIFO waitlist promotion scenarios
Execute and capture:

TC11: FIFO promotion.

TC12: First waitlisted learner is ineligible at promotion time.

TC13: Multiple learners are waiting.

TC14: Multiple course drops create multiple promotion opportunities.

Verify that promotion follows the approved FIFO and eligibility policy.

Check the actual database records after each operation.

## 8. Concurrency scenarios
Execute actual simultaneous HTTP requests against localhost.

TC15: Multiple learners competing for the last available seat.

TC16: Same learner submitting multiple simultaneous enrollment requests.

TC17: Multiple learners dropping simultaneously.

TC18: Multiple concurrent promotion opportunities.

Verify:

- Course capacity is never exceeded.
- No duplicate active enrollment is created.
- No learner is promoted twice.
- FIFO ordering is preserved according to the approved policy.
- Database records remain consistent.
Capture every individual concurrent request and response, including timestamps and HTTP status codes.

Use genuine concurrent HTTP clients, not mocked service calls.

## 9. Validation and error scenarios
Test:

- Missing mandatory fields.
- Invalid request body.
- Invalid identifiers.
- Unsupported HTTP method.
- Invalid business operations.
- Appropriate HTTP status codes.
- Consistent response structure.
Use the actual API contract.

## 10. Database verification
For all important scenarios, capture the actual persisted state.

Verify:

- Course capacity.
- Number of enrolled learners.
- Enrollment statuses.
- Waitlist entries.
- Waitlist ordering.
- Promotion results.
- Duplicate records.
Use read-only database queries wherever possible.

Capture before-and-after database state for important scenarios.

## 11. Execute existing automated tests
In addition to live API tests, execute the existing:

- JUnit tests.
- Mockito tests.
- Integration tests.
- BDD tests, if configured.
- Concurrency tests.
Use the project's actual test configuration.

Capture execution logs and test reports.

Do not replace live API testing with these automated tests.

## 12. Screenshots and execution evidence
Capture genuine screenshots showing:

1. Application running on localhost.
2. Successful enrollment.
3. Full-course waitlist response.
4. Duplicate enrollment response.
5. Successful drop.
6. FIFO promotion.
7. Ineligible learner handling.
8. Concurrent enrollment results.
9. Final execution report.
Save all screenshots under api-execution-evidence/08-screenshots/.

If screenshot capture is unavailable, save the genuine request/response logs and document the limitation.

## 13. Consolidated API execution report
Generate:

api-execution-evidence/FINAL-API-EXECUTION-REPORT.md

Include:

1. Application startup details.
2. Localhost base URL.
3. Discovered API endpoints.
4. Environment information.
5. Total scenarios executed.
6. Total HTTP requests sent.
7. Passed scenarios.
8. Failed scenarios.
9. Not-executed scenarios.
10. Request/response evidence locations.
11. Database verification results.
12. Concurrency test results.
13. Automated test results.
14. Known issues and limitations.
15. Final assessment readiness.
Use these statuses:

- PASS: Expected behavior verified.
- FAIL: Actual behavior differs from expected.
- NOT EXECUTED: Test could not be performed.
- NOT APPLICABLE: Scenario is outside the assessment requirements.

## 14. Final execution instructions

- Start the actual application on localhost.
- Keep it running while tests execute.
- Execute real HTTP requests.
- Capture every request and response.
- Verify actual database state.
- Preserve failed test evidence.
- Do not fabricate screenshots or execution results.
- Do not add security implementation or security testing.
- Do not introduce features outside the original assessment.
- Keep all evidence inside api-execution-evidence/.
At completion, show:

1. Localhost URL.
2. Application runtime status.
3. Complete test execution summary.
4. Failed scenarios.
5. Evidence folder structure.
6. Final API execution report location.
Leave the application running if the execution environment permits.
```

- **Suggestion:** use real HTTP requests against the configured application;
  report the immutable-fixture limitation instead of simulating TC12.
- **Disposition / affected:** Java socket client and evidence implemented in
  [the live evidence folder](../api-execution-evidence/). The initial PowerShell
  harness was blocked before sending requests; the Java client was used instead.
- **Verification:** `mvn -B -ntp clean package` passed 99 tests. The live
  execution sent 643 requests: 21 scenarios passed, none failed, TC12 was not
  executed. See the [actual report](../api-execution-evidence/FINAL-API-EXECUTION-REPORT.md)
  and captured request/response files. The overlapping concurrent responses do
  not independently prove their exact server commit order.

## 17. OpenAPI contract correction

**Exact prompt:**

```text
Update the openAPI spec to fix the findings
```

- **Suggestion:** explicitly model successful response bodies, RFC problem
  responses, nullable fields, and the identifier constraints instead of
  relying on Springdoc inference.
- **Disposition / affected:** accepted. Controller annotations now document
  `application/problem+json` errors with a dedicated `ApiProblem` schema;
  response DTOs declare nullable fields; request and path identifiers include
  descriptions and bounds. README, API, architecture, review and evidence
  documentation were synchronized.
- **Verification:** targeted
  `mvn -B -ntp '-Dtest=RegistrationControllerTest,ApiFailureHandlingTest' test`
  passed 7 tests. Full `mvn -B -ntp test` passed 101 tests on 2026-11-20.
  The contract test obtains `/v3/api-docs` through MockMvc and checks success
  and error schemas, required problem fields, request bounds and nullable
  fields; API failure tests verify actual problem responses. No standalone
  server was started for this documentation change.
