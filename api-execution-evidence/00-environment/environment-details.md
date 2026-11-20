# Environment and application details

- OS: Windows 11; Java: OpenJDK 21; Maven: 3.9.12.
- Framework: Spring Boot 4.1.2; Maven build from `pom.xml`.
- Configured server: `127.0.0.1:8080`; offering: `CS101-FALL`, capacity 2.
- Eligible fixture IDs: `learner-1001`, `learner-1002`, `learner-1003`,
  `learner-1042`. The eligibility set is immutable while the application runs.
- Storage: one process-local in-memory store.
- Build and existing tests: `mvn -B -ntp clean package`. See
  [build log](maven-clean-package.log) and [Surefire reports](automated-test-reports/).
- Application command: `java -jar target\course-waitlist-0.0.1-SNAPSHOT.jar`.
  The packaged application was started with no configuration overrides.
- Test client: `LiveApiEvidence.java`, Java 21 sockets and locally available
  Jackson libraries. Requests were sent directly to the running application.

The REST endpoints discovered from source are:

| Method | Path | Request / response |
|---|---|---|
| POST | `/api/v1/offerings/{offeringId}/registrations` | JSON `{"learnerId":"..."}`; registration view, 201 or 200. |
| GET | `/api/v1/offerings/{offeringId}/registrations/{learnerId}` | No request body; latest registration view. |
| DELETE | `/api/v1/offerings/{offeringId}/enrollments/{learnerId}` | No request body; drop and promotion result. |

Error responses normally use `application/problem+json`. The known generated
OpenAPI schema inaccuracies are documented in the project's API documentation.
