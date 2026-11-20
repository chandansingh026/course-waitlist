# Limitations and risks

- State is volatile and process-local; restart loses registrations and separate
  processes do not coordinate.
- Authentication, authorization and production transport protection are absent.
  Keep the prototype local; localhost is not an authentication mechanism.
- Eligibility is a fixed demo set, not an external prerequisite system; there is
  no learner directory or audit history.
- One monitor serializes operations, including reads; map copies and waitlist
  sorting scale poorly with growth. Simultaneous request arrival order is not
  guaranteed to match FIFO sequence allocation.
- A terminal registration is replaced on re-registration; waitlist cancellation
  and offering management are not implemented.
- Cucumber concurrency and repeated tests exercise bounded interleavings, not
  every schedule. A human independent review and security assessment are not
  evidenced by the recorded AI review.
- Mockito reports a non-failing dynamic-agent warning on the recorded Java 21
  test run. The separate assessment scorecard was not supplied.

See [security review](security-review.md) and [assessment readiness](assessment-readiness.md).
