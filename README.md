# Course waitlist

A local, single-offering Spring Boot service for enrollment, dropping, and FIFO
waitlist promotion. Registrations are stored in memory and lost on restart.
This assessment prototype is not a production or multi-instance service.

## Requirements and setup

- JDK 21, with `JAVA_HOME` pointing to the JDK.
- Internet access on the first run to download Maven and dependencies.
- No database installation or configuration is required.
  See [architecture](docs/architecture.md) for the storage and locking design.

From the project directory on Windows:

```powershell
.\mvnw.cmd -B -ntp compile
.\mvnw.cmd -B -ntp test
.\mvnw.cmd spring-boot:run
```

On macOS/Linux, use `./mvnw` in place of `.\mvnw.cmd`. The application listens
on `127.0.0.1:8080` by default. Local documentation is at
`http://127.0.0.1:8080/swagger-ui.html` and `/v3/api-docs`.
The generated OpenAPI document is available at `/v3/api-docs`; its response
schemas and nullable fields are checked by an API documentation contract test.
See the [API documentation](docs/api.md).

The initial offering is `CS101-FALL`, capacity 10. Its 20 eligible learners
are `learner-1001` through `learner-1019`, plus `learner-1042`.
These demo fixtures, the offering ID, and capacity are set in
`src/main/resources/application.yml`. Any other ID fails the prerequisite.
This fixture is not a real prerequisite system and cannot change while the
application is running. The waitlist has no configured size limit.

## Example

```powershell
$base = 'http://127.0.0.1:8080/api/v1/offerings/CS101-FALL'
Invoke-RestMethod -Method Post -Uri "$base/registrations" -ContentType 'application/json' -Body '{"learnerId":"learner-1001"}'
Invoke-RestMethod -Method Get -Uri "$base/registrations/learner-1001"
Invoke-RestMethod -Method Delete -Uri "$base/enrollments/learner-1001"
```

The registration response distinguishes `ENROLLED` from `WAITLISTED`. A repeated
active registration returns HTTP 200 without changing the existing position.
Dropping a learner promotes the earliest eligible waitlisted learner; repeated
drops return 409. A dropped learner may submit a new registration.

## Tests and limitations

`test` runs in-memory service and store tests, Spring MVC integration tests, and
executable Cucumber scenarios, including concurrency and atomic-failure tests.
To run the integration-focused tests separately:

```powershell
.\mvnw.cmd -B -ntp '-Dtest=EnrollmentApiIntegrationTest,EnrollmentServiceTest,InMemoryRegistrationStoreTest,RegistrationControllerTest,ApiFailureHandlingTest,HttpServerTest,CucumberTest' test
```

The full-context API tests check the actual in-memory registrations and queue.
Store tests check that failures discard staged changes and sequence allocations.
Service-level tests additionally cover dynamic eligibility and concurrent
operations; the MVC failure-handler slice uses mocked failures.

Run the executable Cucumber scenarios separately with:

```powershell
.\mvnw.cmd -B -ntp '-Dtest=CucumberTest' test
```

Each scenario uses an isolated in-memory store and exercises the service and
repositories, rather than HTTP. The test eligibility policy can change during a
scenario to exercise promotion revalidation; the application fixture is static.
Concurrency scenarios release requests together through two service instances
sharing the same test store.

For the focused concurrency and rollback regression tests:

```powershell
.\mvnw.cmd -B -ntp '-Dtest=EnrollmentConcurrencyTest,EnrollmentServiceTest,EnrollmentApiIntegrationTest,CucumberTest' test
```

The five focused concurrency cases run five times each against isolated stores.
See the [concurrency review](docs/architecture.md#concurrency-review)
for the tested locking behavior and its limitations.

The [architecture](docs/architecture.md) describes the API, invariants, error
responses, and trade-offs. The [requirements traceability matrix](docs/requirements-traceability.md)
maps assessment criteria to implementation and verification. [AI usage](docs/ai-usage.md)
records the prompts, decisions, and verification.

## Engineering documentation

- [API documentation](docs/api.md) and [data/storage design](docs/database-design.md)
- [Test scenarios](docs/test-scenarios.md) and [execution evidence](memory-bank/test-execution-evidence.md)
- [Requirements analysis](memory-bank/requirements-analysis.md), [design decisions](memory-bank/design-decisions.md)
  and [implementation status](memory-bank/implementation-plan.md)
- [Security review](memory-bank/security-review.md), [code review](memory-bank/code-review.md)
  and [limitations](memory-bank/limitations-and-risks.md)
- [AI prompt history](memory-bank/ai-prompt-history.md) and [assessment readiness](memory-bank/assessment-readiness.md)
- [Project overview](memory-bank/project-overview.md), [architecture summary](memory-bank/architecture.md)
  and [test strategy](memory-bank/test-strategy.md)
- [Final assessment-readiness review](docs/final-assessment-readiness-review.md)

There is no authentication or authorization: keep the server bound to localhost.
Separate processes have separate in-memory stores. There is no durable storage,
learner directory, or real eligibility integration. API error responses are not
a replacement for a production security and operational review.
