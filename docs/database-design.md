# Data and storage design

There is **no database**, SQL schema, migration or persistent storage in the
current application. This document describes the in-memory data model; it does
not propose a database.

| Concept | Fields | Rules |
|---|---|---|
| Offering | ID, capacity | One configured offering; valid ID, non-negative capacity; immutable at runtime. |
| Registration | Offering ID, learner ID, status, waitlist sequence | One latest record per learner in the offering. IDs are validated; `WAITLISTED` requires a positive sequence, other statuses require null. |
| Store | Learner-ID-to-registration map, sequence counter | Stored in one process. A sequence must be allocated and cannot duplicate another active waitlist sequence. |

Statuses are `ENROLLED`, `WAITLISTED`, `DROPPED`, and `INELIGIBLE`. Enrolled
registrations can transition to `DROPPED`; waitlisted registrations can
transition to `ENROLLED` or `INELIGIBLE`. A new eligible registration after a
terminal state replaces the prior record. There is no history table or audit log.

The store monitor serializes operations on a private working copy of the map
and counter. On success it publishes the copy; on failure it discards it.
This is in-memory atomicity, not a database transaction. State disappears on
restart and is not shared across processes.

Capacity and eligibility are enforced by the service while it holds the store
lock. The map key enforces one current record per learner; the domain and
store check sequence rules. FIFO follows successfully published sequence order,
not the arrival order of simultaneous requests. See [architecture](architecture.md)
and [test scenarios](test-scenarios.md).
