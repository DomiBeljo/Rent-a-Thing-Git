Feature: Thing Management
  As an authenticated user
  I want to create, read, update, and delete my things
  So that I can manage the items I want to rent out

  Background:
    Given the application is running
    And I am logged in as "thinguser@example.com" with password "Password1!"

  Scenario: Create a new thing
    When I create a thing with name "Drill", category "Tools", and description "A powerful drill"
    Then the response status is 200
    And the thing response contains name "Drill" and category "Tools"

  Scenario: Get thing by ID
    Given I have created a thing with name "Camera"
    When I request the thing by its ID
    Then the response status is 200
    And the thing response contains name "Camera"

  Scenario: Update an existing thing
    Given I have created a thing with name "OldName"
    When I update the thing name to "NewName"
    Then the response status is 200
    And the thing response contains name "NewName"

  Scenario: Delete a thing
    Given I have created a thing with name "ToDelete"
    When I delete the thing
    Then the response status is 200

  Scenario: Non-owner cannot delete another user's thing
    Given another user owns a thing
    When I try to delete that thing
    Then the response status is 403
    And the error code is "THING_OWNERSHIP_REQUIRED"
