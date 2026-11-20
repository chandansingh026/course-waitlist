# AI prompt history

The [AI use record](../docs/ai-usage.md) contains the detailed historical record
and available exact prompts. The following entries are summaries, not prompt
quotations or an independent human review.

| Prompt or decision | AI suggestion / disposition | Reason and verification recorded |
|---|---|---|
| Analyze the attached assessment | Accepted bounded requirements analysis and explicit assumptions. | Matched against the supplied assessment; see [traceability](../docs/requirements-traceability.md). |
| Finalize stack | Java 25 was changed to Java 21; optional Gherkin was changed to executable Cucumber. JUnit Jupiter 6 was retained after a later JUnit 5 request. | User selected the alternatives; the exact reasons for every selection were not recorded. Maven compilation and tests were executed. |
| Design architecture | Accepted one-offering service and eligibility recheck; user confirmed re-enrollment. | User-approved assumptions; service and FIFO tests verify the resulting behavior. |
| Independent code-review request | AI review identified a registration invariant defect and scope/security/performance risks; constructor checks were added. | Regression cases failed before the fix; later full suites passed. This is not independent human review. |
| Engineering-documentation request | Documentation created at the user's request; user review of the result is not yet recorded. | Compared with source and recorded results; checked requested files and relative links, without rerunning application tests. |

An independent human review of selected code, tests, architecture, AI-use
record and deliverables was completed on 2026-11-20. Its scope and READY
decision are recorded in the [readiness review](../docs/final-assessment-readiness-review.md).
