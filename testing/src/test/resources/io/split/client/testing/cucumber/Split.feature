@split[cappuccino:off]
@split[dollars:on]
Feature: Split
  This is an example of how to use Split with Cucumber

  @split[cappuccino:on]
  @split[dollars:off]
  Scenario: with cappuccino, but without dollars
    Then split "cappuccino" should be "on"
    And split "dollars" should be "off"

  Scenario: without cappuccino, but with dollars
    Then split "cappuccino" should be "off"
    And split "dollars" should be "on"

  @split[dollars:off]
  Scenario: without cappuccino, nor dollars
    Then split "cappuccino" should be "off"
    And split "dollars" should be "off"
