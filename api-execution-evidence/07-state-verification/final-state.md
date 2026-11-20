# Final observed state

This is a read-only HTTP observation of the four configured eligible learners, not a direct internal-store query.

Observed: 2026-11-19T23:19:41.465307500+05:30

```json
{
    "label":  "after",
    "timestamp":  "2026-11-19T23:19:41.465307500+05:30",
    "configuredCapacity":  2,
    "enrolledCount":  0,
    "remainingCapacity":  2,
    "waitlistOrder":  [

                      ],
    "learners":  [
                     {
                         "learnerId":  "learner-1001",
                         "status":  "DROPPED",
                         "waitlistPosition":  null
                     },
                     {
                         "learnerId":  "learner-1002",
                         "status":  "DROPPED",
                         "waitlistPosition":  null
                     },
                     {
                         "learnerId":  "learner-1003",
                         "status":  "DROPPED",
                         "waitlistPosition":  null
                     },
                     {
                         "learnerId":  "learner-1042",
                         "status":  "DROPPED",
                         "waitlistPosition":  null
                     }
                 ],
    "limitation":  "HTTP-observable fixture registrations; not a direct internal-store or database query."
}
```
