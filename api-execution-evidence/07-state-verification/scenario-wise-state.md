# Scenario-wise observed state

The configured capacity is 2. Counts and order below are derived from individual GET responses for the four eligible fixture IDs, not an atomic internal-store query. Full per-operation observations and their individual HTTP exchanges are in each linked execution file.

| Case | Before: enrolled / queue | After: enrolled / queue |
|---|---|---|
| [TC01](../01-enrollment/TC01-successful-enrollment/execution.json) | 0 / empty | 1 / empty |
| [TC02](../01-enrollment/TC02-course-full-waitlist/execution.json) | 0 / empty | 2 / learner-1003 |
| [TC03](../01-enrollment/TC03-duplicate-enrollment/execution.json) | 0 / empty | 2 / learner-1003 |
| [TC04](../01-enrollment/TC04-ineligible-learner/execution.json) | 0 / empty | 0 / empty |
| [TC05](../01-enrollment/TC05-invalid-course/execution.json) | 0 / empty | 0 / empty |
| [TC06](../01-enrollment/TC06-invalid-learner/execution.json) | 0 / empty | 0 / empty |
| [TC07](../02-course-dropping/TC07-successful-drop/execution.json) | 0 / empty | 0 / empty |
| [TC08](../02-course-dropping/TC08-invalid-drop/execution.json) | 0 / empty | 0 / empty |
| [TC09](../02-course-dropping/TC09-repeated-drop/execution.json) | 0 / empty | 0 / empty |
| [TC10](../02-course-dropping/TC10-drop-with-empty-waitlist/execution.json) | 0 / empty | 1 / empty |
| [TC11](../03-waitlist-promotion/TC11-fifo-promotion/execution.json) | 0 / empty | 2 / learner-1042 |
| TC12 | Not executed | Not executed | 
| [TC13](../03-waitlist-promotion/TC13-multiple-waitlisted-learners/execution.json) | 0 / empty | 2 / learner-1003, learner-1042 |
| [TC14](../03-waitlist-promotion/TC14-promotion-after-drop/execution.json) | 0 / empty | 2 / empty |
| [TC15](../04-concurrency/TC15-simultaneous-enrollment/execution.json) | 0 / empty | 2 / learner-1002, learner-1042 |
| [TC16](../04-concurrency/TC16-duplicate-concurrent-enrollment/execution.json) | 0 / empty | 2 / empty |
| [TC17](../04-concurrency/TC17-simultaneous-drops/execution.json) | 0 / empty | 0 / empty |
| [TC18](../04-concurrency/TC18-concurrent-promotion/execution.json) | 0 / empty | 2 / empty |
| [TC19](../05-validation-and-errors/TC19-missing-fields/execution.json) | 0 / empty | 0 / empty |
| [TC20](../05-validation-and-errors/TC20-invalid-request/execution.json) | 0 / empty | 0 / empty |
| [TC21](../05-validation-and-errors/TC21-invalid-identifiers/execution.json) | 0 / empty | 0 / empty |
| [TC22](../05-validation-and-errors/TC22-unsupported-method/execution.json) | 0 / empty | 0 / empty |
