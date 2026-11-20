# REST API

The local service exposes Springdoc at `/v3/api-docs` and Swagger UI at
`/swagger-ui.html`. The default base URL is
`http://127.0.0.1:8080/api/v1/offerings/CS101-FALL`.

The generated OpenAPI document at `/v3/api-docs` documents successful response
schemas, RFC problem responses, request constraints and nullable fields. The
contract is verified by `RegistrationControllerTest`.

## Identifiers and errors

Offering and learner IDs are case-sensitive strings of at most 100 UTF-16 code
units. They must be nonblank and have no surrounding Unicode whitespace or
space characters. A JSON `learnerId` must be a string, not a coerced number,
boolean, object or array.

Errors use Spring `ProblemDetail`, normally `application/problem+json` when the
client accepts it. The response includes `type`, `title`, `status`, `detail`
and `instance`. Unexpected exceptions return a generic 500 response; diagnostic
details are logged server-side. Content negotiation may instead produce 406
when the client rejects the problem media type.

| Condition | Status |
|---|---:|
| Invalid ID or request body | 400 |
| Unknown offering, registration or route | 404 |
| Unsupported method or content type | 405 or 415 |
| Drop when learner is not enrolled | 409 |
| Prerequisite not satisfied | 422 |
| Unexpected failure | 500 |

Example ineligible response:

```json
{"type":"urn:course-waitlist:ineligible","title":"Unprocessable Content","status":422,"detail":"Prerequisite is not satisfied","instance":"/api/v1/offerings/CS101-FALL/registrations"}
```

## Register

`POST /registrations`, `Content-Type: application/json`

```json
{"learnerId":"learner-1001"}
```

New enrollment example, HTTP 201:

```json
{"offeringId":"CS101-FALL","learnerId":"learner-1001","status":"ENROLLED","waitlistPosition":null,"created":true}
```

An eligible learner is `WAITLISTED` with a one-based `waitlistPosition` if
capacity is full. A repeated active registration returns HTTP 200 and
`created: false`, without changing the existing position. A dropped or
ineligible learner can make a new request and must satisfy eligibility again.

## Read

`GET /registrations/{learnerId}` returns HTTP 200 and the latest registration,
including `DROPPED` or `INELIGIBLE`, with `created: false`. A waiting learner's
position reflects the current queue. An unknown offering or registration returns
404.

## Drop

`DELETE /enrollments/{learnerId}` drops only an enrolled learner. Example HTTP
200 after the default capacity of ten is filled and `learner-1011` is first
on the waitlist:

```json
{"offeringId":"CS101-FALL","droppedLearnerId":"learner-1001","promotedLearnerId":"learner-1011","skippedIneligibleCount":0,"remainingCapacity":0}
```

`promotedLearnerId` is null when no eligible waiter is found. The earliest
eligible waiter is promoted; earlier ineligible waiters are marked
`INELIGIBLE`. Repeated or otherwise invalid drops return 409.

The [architecture](architecture.md) describes the implementation and
concurrency guarantees.
