# Live localhost API execution report

## Result

**21 PASS, 0 FAIL, 1 NOT EXECUTED, 0 NOT APPLICABLE** among the 22 requested
live scenarios. The client sent **643 real HTTP requests**, including setup,
cleanup and state-observation requests; all 643 received the expected HTTP
status and specified response fields. The 22 scenario statuses are listed below.

TC12 was not executed: the application's eligibility fixture cannot change
after a learner is waitlisted and there is no endpoint to change it. The
existing automated tests exercise a changing test policy, but that is not
evidence of a live TC12 execution.

## Environment and startup

- Java 21, Spring Boot 4.1.2, Maven 3.9.12, Windows 11.
- Build: `mvn -B -ntp clean package` — **BUILD SUCCESS**, 99 tests passed,
  0 failures, 0 errors, 0 skipped.
- Startup: `java -jar target\course-waitlist-0.0.1-SNAPSHOT.jar`.
  [Startup log](00-environment/application-startup.log) records the configured
  port and successful initialization.
- Host: `http://127.0.0.1:8080`; offering base:
  `http://127.0.0.1:8080/api/v1/offerings/CS101-FALL`.
- State is in memory and disappears on restart.
- The application remained running during the live suite; see
  [runtime status](00-environment/runtime-status.md).

For endpoint paths, payloads, and fixture data see
[environment details](00-environment/environment-details.md).

## Scenario outcomes

| ID | Scenario | Status |
|---|---|---|
| TC01 | Successful enrollment | PASS |
| TC02 | Full course places learner on waitlist | PASS |
| TC03 | Duplicate enrollment and waitlist requests | PASS |
| TC04 | Ineligible learner rejected | PASS |
| TC05 | Unknown offering | PASS |
| TC06 | Invalid learner ID | PASS |
| TC07 | Successful drop | PASS |
| TC08 | Drop when not enrolled | PASS |
| TC09 | Repeated drop | PASS |
| TC10 | Drop with empty waitlist | PASS |
| TC11 | FIFO promotion | PASS |
| TC12 | First waiter becomes ineligible | NOT EXECUTED |
| TC13 | Multiple waitlisted learners | PASS |
| TC14 | Multiple sequential promotions | PASS |
| TC15 | Simultaneous requests for last seat | PASS |
| TC16 | Duplicate concurrent requests | PASS |
| TC17 | Simultaneous drops | PASS |
| TC18 | Concurrent promotion opportunities | PASS |
| TC19 | Missing mandatory field | PASS |
| TC20 | Malformed JSON and non-string ID | PASS |
| TC21 | Invalid identifiers | PASS |
| TC22 | Unsupported method | PASS |

Each case directory contains `requests.json`, `responses.json`, `execution.json`
and individual `exchanges` where requests were sent. TC12 contains empty
request/response arrays and its reason. The
[scenario summary](06-all-request-response-logs/scenario-summary.json),
[all requests](06-all-request-response-logs/all-api-requests.json),
[all responses](06-all-request-response-logs/all-api-responses.json), and
[execution log](06-all-request-response-logs/complete-execution.log) are
consolidated. Request and response records include timestamps, URLs, headers,
bodies, expected status and fields, actual status and body, and result.

## Observed in-memory state

The test client observed all four configured eligible learners through the
real GET endpoint before, after, and at important intermediate steps. Those
sequential HTTP observations are not direct or atomic internal-store snapshots.

- [Initial observed state](07-state-verification/initial-state.md):
  no fixture learner had a registration, zero enrolled, capacity 2.
- [Scenario-wise observed state](07-state-verification/scenario-wise-state.md):
  enrollment counts, queue order and links to complete snapshots.
- [Final observed state](07-state-verification/final-state.md):
  all four latest records were `DROPPED`, zero enrolled, queue empty.

The known fixture bounds these observations. The API does not expose a store
dump or historical registrations; it cannot directly prove
the absence of unseen internal duplicates. An additional
[snapshot assertion audit](07-state-verification/snapshot-assertions.json)
passed for all 21 executed cases. Its first post-processing attempt failed
before producing results; the [error](07-state-verification/snapshot-audit-initial-failure.log)
was retained and the audit was rerun successfully.

## Concurrency results

Separate Java socket clients were released using a `CyclicBarrier`. Their
recorded request/response intervals overlapped:

- TC15: three contenders for the last seat yielded one enrolled and two waiting.
- TC16: eight concurrent requests from one learner yielded one 201/created
  response and seven 200/existing responses.
- TC17: both concurrent drops succeeded, with no promotions.
- TC18: two concurrent drops promoted the two previously ordered waiters once
  each; the post-operation queue was empty and capacity was not exceeded.

Sequential TC11 and TC14 verify FIFO promotion order. In TC18 the two HTTP
responses overlap, so their receipt order cannot establish the exact server
commit order; the two observed outcomes are compatible with FIFO. The snapshots
and bounded tests do not prove every possible interleaving or that capacity
could never be exceeded in unobserved executions.

## Existing automated tests

The build ran the configured Surefire suite: 99 tests passed, including unit
and Mockito-based tests, full-context in-memory integration/API tests, 10
executable Cucumber scenarios and 25 executions of five repeated concurrency
tests. The [build log](00-environment/maven-clean-package.log) and copied
[Surefire reports](00-environment/automated-test-reports/) are retained.
Mockito's dynamic-agent warning did not fail the build.

The first PowerShell script harness was blocked by the machine's script
execution policy before sending any HTTP request. Its
[console output](06-all-request-response-logs/harness-console.log) is preserved;
the successful Java client did not change that policy.

## Screenshots and limitations

The [screenshots](08-screenshots/) show the actual startup log and captured
HTTP exchange files rendered in the browser; they are screenshots of recorded
evidence, not a separate API client UI. The ineligible screenshot is TC04's
initial registration rejection, not the unexecuted TC12 promotion case.
The [final report screenshot](08-screenshots/final-report.png) is also included.

The application has only an in-memory store, a static eligibility fixture and
one configured offering. The existing project documentation also notes that
generated OpenAPI error schemas and nullability are inaccurate. No business
logic was changed for this execution.

## Final assessment readiness

The exercised live scenarios and existing automated tests passed. **Do not mark
all requested live checks complete:** TC12 was not performed, for the reason
above. Independent human review of the assessment and AI-use record remains
unverified.
