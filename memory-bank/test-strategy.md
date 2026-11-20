# Test strategy

- JUnit Jupiter 6 and Mockito isolate service decisions and domain validation.
- Real in-memory-store tests check state and failure atomicity; some service
  tests mock eligibility or inject a controlled failure after a staged save.
- Full Spring MVC tests exercise validation, HTTP outcomes and actual store
  state; a separate random-port test sends a real HTTP request.
- Executable Cucumber scenarios exercise the service and store, not HTTP.
- Latch-coordinated tests exercise competing registrations and drops, FIFO and
  failed staged operations. Five concurrency cases are repeated five times.

This is not a database integration strategy: the current implementation has
no database. Tests do not establish exhaustive scheduling or production
multi-instance behavior. See [scenario mapping](../docs/test-scenarios.md)
and [execution evidence](test-execution-evidence.md).
