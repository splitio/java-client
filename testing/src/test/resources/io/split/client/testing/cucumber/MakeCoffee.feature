# This tag is inherited by all the scenarios, setting the "cappuccino" split feature to "off" by default.
@split[cappuccino:off]
Feature: Make Coffee
  The scenarios in this feature describes how the coffee machine works.

  Scenario: Empty machine
    Given the machine is empty
    Then no drinks should be available

  Scenario: Display available drinks
    Given the machine is not empty
    Then the following drinks should be available:
      | name          | price |
      | filter coffee |  0.80 |

  # The tags on this scenario will be ["@split[cappuccino:off]", "@split[cappuccino:on]"]
  # The @split tags are processed sequentially, so the cappuccino split feature will be set to "off"
  # and then immediately overwritten to "on".
  @split[cappuccino:on]
  Scenario: Display available drinks (including the new experimental cappuccino)
    Given the machine is not empty
    Then the following drinks should be available:
      | name          | price |
      | filter coffee |  0.80 |
      | cappuccino    |  1.10 |
