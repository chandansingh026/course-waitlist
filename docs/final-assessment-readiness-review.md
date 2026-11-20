# Final assessment-readiness review

Review updated: 2026-11-20. Primary checklist: the supplied *Focused AI-Assisted
Engineering Assessment: Course Waitlist*. This report includes AI-assisted
verification and the independent human review recorded below.

## Assessment traceability

| Criterion | Implementation and verification | Status |
|---|---|---|
| AC1: eligible learner enrolls with capacity | Service capacity and eligibility checks; `EnrollmentServiceUnitTest`, Cucumber, live HTTP. | Verified in exercised cases. |
| AC2: full course waitlists once; no duplicates | Map keyed by learner; idempotent active requests; unit, MVC, concurrency, Cucumber and live HTTP checks. | Verified in exercised cases. |
| AC3: drop promotes earliest eligible waiter | Ordered sequence scan and atomic store operation; service, MVC, concurrency, Cucumber and live HTTP checks. | Verified in exercised cases. |
| AC4: ineligible learner is not enrolled or promoted; policy is consistent | Fixture eligibility and promotion-time recheck; service tests and Cucumber use a changing test policy to verify marking ineligible and skipping. | Verified in exercised cases. |
| AC5: invalid requests and repeated drops do not corrupt state | Validation, explicit errors and staged changes; service, store, MVC and live HTTP checks. | Verified in exercised cases. |
| AC6: focused tests and practical concurrency test | Unit, in-memory integration, Cucumber and repeated coordinated concurrency tests all ran under Surefire. | Verified in exercised cases. |
| AC7: record AI prompts, responses, dispositions and verification | [AI use record](ai-usage.md) records substantial prompts, decisions and command evidence; the reviewer included it in their documented review scope. | Reviewed by the independent reviewer listed below. |

The [full traceability matrix](requirements-traceability.md) maps requirements
to specific implementation and tests. The approved re-enrollment rule is covered
by `EnrollmentServiceTest`, `EnrollmentApiIntegrationTest`, and live HTTP.

## Implemented features and boundaries

One configured offering supports eligibility-checked enrollment, automatic
waitlisting, duplicate-safe registration, lookup, drop, re-enrollment and
eligible FIFO promotion. Invalid IDs and requests receive explicit errors.
The store monitor and copy-on-success updates serialize in-process operations
and discard staged changes on failure.

There is **no database**. This reflects the user's H2 removal request and the
assessment's instruction not to add a database without a starter. In-memory
state was checked by integration tests and live requests; it is lost on restart.

No frontend, catalog, payment, identity integration, deployment, policy engine
or multi-term workflow was identified in the project. REST, OpenAPI and
Cucumber were approved project choices, not assessment mandates.

## Build and test evidence

The latest run, after correcting the OpenAPI contract, was
`mvn -B -ntp test` on 2026-11-20: **101 tests, 0 failures, 0 errors, 0
skipped; BUILD SUCCESS**. It includes generated-document assertions for
response schemas, problem media types, request constraints and nullability.
The preceding 100-test run on 2026-11-19 covered the default capacity of 10
and 20 eligible learners. Current Surefire reports are under
`target/surefire-reports` and may be overwritten.

The independent reviewer separately reports rerunning the suite in the IntelliJ
IDEA 2025.3.1 JUnit 6 runner on 2026-11-20, with exit code 0. The supplied
review details do not state a test count. The 101-test count above is from the
separately executed Maven run; it is not attributed to the IDE run.

The earlier review commands below ran on 2026-11-19 on Windows with Java 21
and Maven 3.9.12. All exit codes were zero:

| Command | Actual result |
|---|---|
| `mvn -B -ntp clean compile` | BUILD SUCCESS; 22 main sources compiled; 4.127 s. |
| `mvn -B -ntp test` | BUILD SUCCESS; 99 tests, 0 failures, 0 errors, 0 skipped; 20.755 s. |
| `mvn -B -ntp verify` | BUILD SUCCESS; 99 tests, 0 failures, 0 errors, 0 skipped; 30.064 s; executable Spring Boot JAR packaged. |

The later live-test build, `mvn -B -ntp clean package`, also passed **99 tests**
with 0 failures, 0 errors and 0 skipped on 2026-11-19. Its
[build log](../api-execution-evidence/00-environment/maven-clean-package.log)
and [Surefire reports](../api-execution-evidence/00-environment/automated-test-reports/)
are retained. Those historical runs and the live checks used the then-default
capacity of 2 and four eligible learners; the defaults have since changed.

Maven `verify` reruns the same Surefire test suite before packaging. The POM
does not configure a separate Failsafe integration-test phase. The earlier 99 tests
include 30 isolated service unit executions, 10 domain tests, 11 service/store
integration tests, 4 store tests, 9 API/HTTP tests, 10 executable Cucumber
scenarios and 25 executions of five repeated concurrency cases. These categories
are not additional runs. `EnrollmentApiIntegrationTest` uses real MVC and store
components; `ApiFailureHandlingTest` is a mocked MVC slice. The concurrent tests
exercise bounded interleavings, not all possible schedules.

Surefire reports are generated under `target/surefire-reports` and can be
overwritten by subsequent runs. Mockito's dynamic-agent warning did not fail
the tests. No coverage percentage or external security-scan result is claimed.

## Startup and live API checks

The exact README startup command was run and Tomcat started on
`127.0.0.1:8080`. The README's PowerShell POST, GET and DELETE examples
were also executed successfully. The process was stopped after verification.
Additional live checks passed for:

- Two enrollments, two ordered waitlist entries and idempotent duplicate requests.
- Invalid JSON identifier (400), unsatisfied prerequisite (422), unknown
  offering (404) and repeated drop (409).
- Two FIFO promotions, queue position updates and re-enrollment at the queue tail.
- `/v3/api-docs` and Swagger UI responding with 200.

The hand-written API examples agree with the exercised responses. The OpenAPI
response schemas and nullable fields are covered by a MockMvc test of the
generated `/v3/api-docs`; the latest test run verified that updated document
without starting a separate live server. Windows setup and startup were
exercised; macOS/Linux instructions and a clean first-time dependency download
were not separately verified.

## Security review

No authentication or authorization is implemented. Localhost binding is a
prototype boundary, not access control. Invalid input is rejected and unexpected
error responses are sanitized; the mocked MVC failure tests cover generic 500
responses. Unexpected exception logs may contain sensitive diagnostics.
There is no production TLS, rate limiting, penetration test or dependency
vulnerability scan. See [security review](../memory-bank/security-review.md).

## Independent human review

**Status: COMPLETED**

- **Reviewer:** Chandan Kumar Singh
- **Review date:** 2026-11-20
- **Scope reviewed:** [EnrollmentService](../src/main/java/com/example/coursewaitlist/application/EnrollmentService.java)
  (enrollment, drop, duplicate protection and FIFO promotion);
  [InMemoryRegistrationStore](../src/main/java/com/example/coursewaitlist/infrastructure/memory/InMemoryRegistrationStore.java)
  (synchronization, staged updates and consistency);
  [Registration](../src/main/java/com/example/coursewaitlist/domain/Registration.java)
  (domain rules and lifecycle); [EnrollmentServiceUnitTest](../src/test/java/com/example/coursewaitlist/application/EnrollmentServiceUnitTest.java);
  concurrency test classes; [architecture documentation](architecture.md);
  [AI-use record](ai-usage.md); and the supporting deliverables named in the
  review.
- **Finding and decision:** OpenAPI error response schemas needed explicit
  `application/problem+json` documentation. **Fixed:** response schemas and
  media types are now explicit, with a generated-document contract test.
- **Verification reported by reviewer:** the complete test suite was rerun in
  IntelliJ IDEA 2025.3.1 on Windows with Java 21 and JUnit 6; process exit code
  0. The reviewer cites IntelliJ console output and `target/surefire-reports`.
  The reported IDE output was not separately supplied for inspection here.
- **Independent verification:** the recorded Maven run
  `mvn -B -ntp test` passed 101 tests on 2026-11-20, as detailed above.
- **Reviewer’s final readiness decision:** **READY**.

## Outstanding issues and limitations

1. **Operational scope:** State is volatile and single-process; eligibility is a
   static fixture; there is no audit history, authentication or waitlist
   cancellation. The lock and map copying limit scalability. FIFO follows
   successful sequence allocation, not concurrent request arrival order.
2. **Assessment information:** The referenced separate scorecard was not
   supplied, so its criteria, weights and a final score cannot be verified.

## Project structure

```text
course-waitlist/
  .mvn/wrapper/       Maven wrapper configuration
  mvnw, mvnw.cmd      Maven wrapper launchers
  pom.xml             Maven build and dependencies
  README.md           Setup, commands and examples
  docs/               Architecture, API, data model, traceability and this report
  memory-bank/        Engineering decisions, risks and evidence
  src/main/java/      API, application, config, domain and in-memory infrastructure
  src/main/resources/ Application configuration and demo fixture
  src/test/java/      Unit, integration, concurrency and Cucumber step tests
  src/test/resources/features/  Executable Gherkin feature
  target/             Generated build and Surefire outputs
```

## Start and try the API

From the project directory on Windows with JDK 21:

```powershell
.\mvnw.cmd spring-boot:run
```

In another PowerShell window, on a fresh instance:

```powershell
$base = 'http://127.0.0.1:8080/api/v1/offerings/CS101-FALL'
1001..1011 | ForEach-Object {
    $body = @{ learnerId = "learner-$_" } | ConvertTo-Json
    Invoke-RestMethod -Method Post -Uri "$base/registrations" -ContentType 'application/json' -Body $body
}
Invoke-RestMethod -Method Delete -Uri "$base/enrollments/learner-1001"
Invoke-RestMethod -Method Get -Uri "$base/registrations/learner-1011"
```

The eleventh POST waitlists `learner-1011`; the DELETE promotes that learner.
Stop the server with Ctrl+C. The README describes prerequisites and other
commands. State disappears when the process stops.

## Readiness conclusion

**Reviewer decision: READY** (Chandan Kumar Singh, 2026-11-20). The recorded
Maven suite passes 101 tests and the reviewer reports a successful IntelliJ
suite run. The review also verified the OpenAPI error-schema correction.
Operational limitations remain as listed above; the separate assessment
scorecard is unavailable, and live TC12 was not executed because eligibility
cannot change at runtime. These are disclosed limitations, not unreported
verification results.
