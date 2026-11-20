# Design decisions

| Decision | Rationale and trade-off |
|---|---|
| Java 21, Spring Boot 4.1.2, Maven Wrapper | Approved stack and familiar service tooling; the assessment does not require these choices. |
| Single in-memory store, no database | Restores the assessment fallback after the user requested H2 removal; restart and multi-process sharing are unsupported. |
| Monitor plus staged copy | Serializes compound changes and discards them on failure; copying and locking limit throughput. |
| Eligible FIFO with skip/remove | Recheck at promotion; mark ineligible waiters `INELIGIBLE` and continue. |
| Idempotent active registration | Prevents duplicate rows and preserves position; a dropped learner may re-register. |
| Local REST and `ProblemDetail` | Makes outcomes inspectable; no authentication is provided for this localhost prototype. |
| JUnit Jupiter 6, Mockito and executable Cucumber | Approved test stack; JUnit 5 was requested later but the user chose to keep Jupiter 6. |

The fixture policy is a demo approximation of the unspecified prerequisite. See
[AI usage](../docs/ai-usage.md) for historical suggestions and dispositions.
