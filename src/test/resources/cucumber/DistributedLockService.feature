#@disabled
Feature: Distributed Lock Service

  Scenario Outline: Executing code with exclusive lock
    Given a distributed lock service is available
    When code is executed with lock name "<lockName>" and lock TTL of <lockTtlMs> ms
    Then the code should be executed successfully
    And no exception should be thrown
    And the lock should be released even when an exception occurs
    Examples:
      | lockName      | lockTtlMs |
      | test-lock     | 1000      |
      | another-lock  | 5000      |
      | resource-lock | 10000     |

  Scenario: Handling exceptions during code execution
    Given a distributed lock service is available
    And the code will throw an exception
    When code is executed with lock name "exception-lock" and lock TTL of 1000 ms
    Then an exception should be thrown
    And the lock should be released even when an exception occurs

  Scenario: Handling lock acquisition failures
    Given a distributed lock service is available
    And a lock that fails to be acquired
    When attempting to execute code with a failing lock
    Then an exception should be thrown
