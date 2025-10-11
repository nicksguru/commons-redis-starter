@db @cache #@disabled
Feature: Cacheable methods test

  Scenario: Calling a non-cacheable method
    When a non-cacheable method is called to generate UUID1
    And a non-cacheable method is called to generate UUID2
    Then UUID1 is not equal to UUID2

  Scenario: Calling a cacheable method
    When a cacheable method is called to generate UUID1
    And a cacheable method is called to generate UUID2
    Then UUID1 is equal to UUID2
