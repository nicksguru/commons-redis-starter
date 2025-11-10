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

  Scenario Outline: Attempt to block an invalid access token
    Given the following access token value
      """
      <accessToken>
      """
    When 'blockJwt' is called for the access token
    Then an exception should be thrown
    Examples:
      | accessToken |
      | not-a-jwt   |
      |             |
