# Security review

This is a source-based review of the prototype, not a penetration test,
dependency vulnerability scan or independent security certification.

| Risk | Current state / handling |
|---|---|
| Unauthorized local access | No authentication or authorization. The default bind is `127.0.0.1`; localhost binding does not authenticate local clients. Do not expose publicly. |
| Internal error disclosure | [ApiExceptionHandler](../src/main/java/com/example/coursewaitlist/api/ApiExceptionHandler.java) returns a generic 500; `ApiFailureHandlingTest` checks sanitization with mocked failures. Unexpected exception details are logged and may contain sensitive diagnostics. |
| Invalid input | JSON learner IDs are strictly strings; shared validation checks blanks, length and surrounding Unicode whitespace. API tests cover malformed and invalid requests. |
| Sensitive prompt data | The assessment requires secrets and unnecessary sensitive data to be excluded from prompts and records. Review the [AI record](../docs/ai-usage.md) before submission. |
| Availability and integrity | A single store monitor, unbounded map and queue scans are appropriate only for a bounded prototype. Capacity and promotion invariants rely on the service and shared store protocol. |

No claims are made about production transport security, authorization, rate
limits, audit logging or external prerequisite verification. See
[limitations](limitations-and-risks.md).
