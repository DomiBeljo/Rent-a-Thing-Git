Feature: Listing Management
  As an authenticated user
  I want to create and manage listings for my things
  So that other users can find and rent them

  Background:
    Given the application is running
    And I am logged in as "listinguser@example.com" with password "Password1!"
    And I have a thing available to list

  Scenario: Create a listing for my thing
    When I create a listing with price 15.00, location "Zagreb", and deposit 50.00
    Then the response status is 200
    And the listing response contains location "Zagreb" and isAvailable true

  Scenario: Update a listing
    Given I have an existing listing
    When I update the listing price to 25.00 and location to "Split"
    Then the response status is 200
    And the listing response contains price 25.00 and location "Split"

  Scenario: Toggle listing availability off
    Given I have an existing listing that is available
    When I toggle the listing availability
    Then the response status is 200
    And the listing is now unavailable

  Scenario: Search for listings by thing name
    Given I have an existing listing for a thing named "Drill"
    When I search listings with query "dri"
    Then the response status is 200
    And the search results contain a listing for "Drill"

  Scenario: Get recommended listings
    Given there are available listings in the system
    When I request recommended listings
    Then the response status is 200

  Scenario: Non-owner cannot update another user's listing
    Given another user owns a listing
    When I try to update that listing
    Then the response status is 403
    And the error code is "LISTING_OWNERSHIP_REQUIRED"
