# Test execution evidence

## OpenAPI contract correction

On 2026-11-20, `mvn -B -ntp test` completed with **BUILD SUCCESS**:
101 tests, 0 failures, 0 errors and 0 skipped. The added generated-document
assertions verify request constraints, successful response schemas,
`application/problem+json` error schemas, required problem fields and nullable
response fields. The controller and error-handler tests also verify actual
problem response bodies. The updated OpenAPI document was generated through
MockMvc; no standalone application process was started for this change.

## Default-capacity update

Earlier, on 2026-11-19, `mvn -B -ntp test` completed with **BUILD SUCCESS**:
100 tests, 0 failures, 0 errors and 0 skipped. The added
`DefaultOfferingIntegrationTest` exercises all 20 eligible learners, the
10-seat boundary, waitlist positions and FIFO promotion using the default
configuration. Surefire reports are in `target/surefire-reports` and may be
overwritten by later runs.

## Final review

Earlier on 2026-11-19, with Java 21 and Maven 3.9.12, before the default
capacity and fixture update:

| Command | Actual result |
|---|---|
| `mvn -B -ntp clean compile` | BUILD SUCCESS; 22 main sources compiled. |
| `mvn -B -ntp test` | BUILD SUCCESS; 99 tests, 0 failures, 0 errors, 0 skipped. |
| `mvn -B -ntp verify` | BUILD SUCCESS; 99 tests, 0 failures, 0 errors, 0 skipped; Spring Boot JAR packaged. |

Both test commands used Surefire and ran the same suite. The README startup and
example requests and further live API workflows were exercised successfully.
See the [final review](../docs/final-assessment-readiness-review.md) for
details and limitations. Mockito emitted a non-failing dynamic-agent warning.

## Earlier in-memory verification

On 2026-11-19, after H2 removal:

```powershell
.\mvnw.cmd -B -ntp clean test
```

Actual result: **BUILD SUCCESS**; 99 tests, 0 failures, 0 errors, 0 skipped.
It included 25 repeated concurrency executions, 10 Cucumber scenarios, 11
service tests and 4 store tests. Mockito emitted a non-failing dynamic-agent
warning.

The generated Maven Surefire summaries are under `target/surefire-reports`
after a run; they are build output and may be removed by a later clean.

The following command also completed successfully with no matching artifacts:

```powershell
.\mvnw.cmd -B -ntp dependency:tree '-Dincludes=com.h2database:h2,org.springframework.boot:spring-boot-starter-jdbc,org.springframework:spring-jdbc'
```

That result checks those named dependencies; it is not a vulnerability scan.
Earlier H2-based results are historical and do not verify the current
implementation; see [traceability](../docs/requirements-traceability.md#historical-h2-verification).
No automated tests were rerun solely for this documentation update.
