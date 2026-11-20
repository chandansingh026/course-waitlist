# Project overview

This is a local Spring Boot service for one offering's enrollment, drop and
FIFO waitlist promotion. The original assessment permits a minimal in-memory
service when no starter exists; no starter was supplied. The REST interface,
Spring Boot and OpenAPI are approved project choices, not assessment mandates.

The current implementation uses an in-memory map, not a database. Registration
state is lost on restart and not shared across processes. The default fixture
and commands are in the [README](../README.md); the detailed design is in
[architecture](../docs/architecture.md).

Key directories: `src/main/java/com/example/coursewaitlist` for application code,
`src/test/java` for automated tests, `src/test/resources/features` for Gherkin,
and `docs` for the canonical design and traceability documents.
