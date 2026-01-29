@db @cache #@disabled
Feature: Cacheable methods test

  Scenario: Calling a non-cacheable method
    When a non-cacheable method is called to generate string1
    And a non-cacheable method is called to generate string2
    Then string1 is not equal to string2

  Scenario: Calling a cacheable method
    When a cacheable method is called to generate string1
    And a cacheable method is called to generate string2
    Then string1 is equal to string2
