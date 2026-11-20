# Requirements analysis

## Assessment requirements

For one offering, enroll eligible learners while seats remain; otherwise
waitlist them once. A drop must promote the earliest eligible waiter. Define
and apply a policy for ineligible waiters. Invalid requests and duplicate
drops must not corrupt state. Supply focused tests, runnable instructions,
a proportionate design, limitations and a candid AI-use record.

Do not add a frontend, catalog, payments, identity integration, multi-term
registration, deployment, a policy engine or a new database without a starter.
The assessment suggests a 7–8 hour timebox, not a measured result.

## Approved project assumptions

- Eligibility is a configured demo set, not a transcript or identity lookup.
- An ineligible waiter is removed from the active queue and marked
  `INELIGIBLE`; the next eligible waiter is considered.
- A dropped learner may register again. Active duplicates return the existing
  state; a new waitlist entry goes to the tail.
- One configured offering; no persistent history or waitlist cancellation.

The separate assessment scorecard was not supplied, so its weights and scoring
details are unknown. See the [requirements matrix](../docs/requirements-traceability.md)
for implementation and verification mapping.
