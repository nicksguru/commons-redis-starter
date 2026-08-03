#@disabled
Feature: Blocked Access Token Service

  Scenario Outline: Check if an access token is blocked
    Given the access token is <isBlocked>
    When 'isBlocked' is called for the access token
    Then no exception should be thrown
    And the result should be <isBlocked>
    Examples:
      | isBlocked |
      | true      |
      | false     |

  Scenario: Block a valid JWT
    Given a valid access token with an expiration of 60 seconds
    When 'blockJwt' is called for the access token
    Then no exception should be thrown
    And the access token should be blocked with a TTL greater than 60 seconds

  Scenario Outline: Check if an access token belongs to a user
    Given a valid access token with subject "<subject>" and an expiration of 60 seconds
    And the user ID is "<userId>"
    When 'ifBelongsToUser' is called for the access token
    Then no exception should be thrown
    And the mapper should have been called with the access token
    Examples:
      | subject   | userId    |
      | test-user | test-user |
      | user-123  | user-123  |

  Scenario Outline: Attempt to check ownership of a token that does not belong to the user
    Given a valid access token with subject "<subject>" and an expiration of 60 seconds
    And the user ID is "<userId>"
    When 'ifBelongsToUser' is called for the access token
    Then an exception should be thrown
    And the exception should be of type "ForbiddenException"
    Examples:
      | subject   | userId      |
      | test-user | other-user  |
      | user-123  | user-456    |
