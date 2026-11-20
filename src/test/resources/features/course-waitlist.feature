Feature: Single-offering enrollment and waitlist

  @F1 @AC1
  Scenario: An eligible learner enrolls when a seat is available
    Given an offering with capacity 1
    And "alex" satisfies the prerequisite
    When "alex" requests registration
    Then the result status is "ENROLLED"
    And there are 1 enrolled learners
    And there are 0 waitlisted learners

  @F2 @AC2
  Scenario: An eligible learner joins the waitlist when the course is full
    Given an offering with capacity 1
    And "alex" satisfies the prerequisite
    And "ben" satisfies the prerequisite
    When "alex" requests registration
    And "ben" requests registration
    Then the result status is "WAITLISTED"
    And "ben" has waitlist position 1
    And there are 1 enrolled learners
    And there are 1 waitlisted learners

  @F2 @AC2
  Scenario: Duplicate enrollment and waitlist requests preserve the existing registrations
    Given an offering with capacity 1
    And "alex" satisfies the prerequisite
    And "ben" satisfies the prerequisite
    When "alex" requests registration
    And "alex" requests registration
    Then the request returns the existing registration
    And the result status is "ENROLLED"
    When "ben" requests registration
    And "ben" requests registration
    Then the request returns the existing registration
    And "ben" has waitlist position 1
    And there are 1 enrolled learners
    And there are 1 waitlisted learners

  @F4 @AC4
  Scenario: An ineligible learner cannot enroll
    Given an offering with capacity 1
    When "ben" requests an ineligible registration
    Then the request fails with "INELIGIBLE"
    And there are 0 enrolled learners
    And there are 0 waitlisted learners

  @F3 @AC3
  Scenario: Dropping an enrolled learner releases the seat for a waiting learner
    Given an offering with capacity 1
    And "alex" satisfies the prerequisite
    And "ben" satisfies the prerequisite
    When "alex" requests registration
    And "ben" requests registration
    And "alex" is dropped
    Then "alex" has status "DROPPED"
    And "ben" is promoted
    And there are 1 enrolled learners
    And there are 0 waitlisted learners

  @F3 @AC3
  Scenario: A drop promotes the earliest learner in FIFO order
    Given an offering with capacity 1
    And "alex" satisfies the prerequisite
    And "ben" satisfies the prerequisite
    And "chris" satisfies the prerequisite
    When "alex" requests registration
    And "ben" requests registration
    And "chris" requests registration
    And "alex" is dropped
    Then "ben" is promoted
    And "chris" has waitlist position 1
    When "ben" is dropped
    Then "chris" is promoted
    And there are 1 enrolled learners

  @F4 @F5 @AC4 @AC5
  Scenario: Skip an ineligible queue head and reject a repeated drop
    Given an offering with capacity 1
    And "alex" satisfies the prerequisite
    And "ben" satisfies the prerequisite
    And "chris" satisfies the prerequisite
    When "alex" requests registration
    And "ben" requests registration
    And "chris" requests registration
    Given "ben" no longer satisfies the prerequisite
    When "alex" is dropped
    Then "ben" is removed as ineligible
    And "chris" is promoted
    When "alex" is dropped again
    Then the request fails with "CONFLICT"
    And there are 1 enrolled learners
    And there are 0 waitlisted learners

  @F3 @AC3
  Scenario: Dropping with an empty waitlist leaves a seat available
    Given an offering with capacity 1
    And "alex" satisfies the prerequisite
    When "alex" requests registration
    And "alex" is dropped
    Then "alex" has status "DROPPED"
    And no learner is promoted
    And remaining capacity is 1
    And there are 0 enrolled learners

  @F2 @F6 @N2
  Scenario: Concurrent enrollment attempts respect capacity and prevent duplicates
    Given an offering with capacity 1
    And "alex" satisfies the prerequisite
    And "ben" satisfies the prerequisite
    When "alex" and "ben" each make 2 concurrent registration requests
    Then all concurrent registration requests succeed
    And exactly 2 concurrent registrations are created
    And there are 1 enrolled learners
    And there are 1 waitlisted learners

  @F3 @F5 @F6 @N2
  Scenario: Concurrent drop requests promote the first waiter only once
    Given an offering with capacity 1
    And "alex" satisfies the prerequisite
    And "ben" satisfies the prerequisite
    And "chris" satisfies the prerequisite
    When "alex" requests registration
    And "ben" requests registration
    And "chris" requests registration
    And 2 requests concurrently drop "alex"
    Then exactly 1 concurrent drop succeeds and 1 fails with "CONFLICT"
    And "alex" has status "DROPPED"
    And "ben" is promoted
    And "chris" has waitlist position 1
    And there are 1 enrolled learners
    And there are 1 waitlisted learners
