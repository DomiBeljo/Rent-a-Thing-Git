Feature: Booking Management
  As an authenticated user
  I want to request, confirm, and manage bookings for listings
  So that I can rent items from other users

  Background:
    Given the application is running
    And a listing owner is registered and logged in
    And a renter is registered and logged in

  Scenario: Renter creates a booking request
    Given the renter is authenticated
    When the renter requests a booking for the listing from tomorrow for 3 days
    Then the booking response status is 200
    And the booking status is "PENDING"

  Scenario: Owner confirms a booking request
    Given the renter has a pending booking for the listing
    And the owner is authenticated
    When the owner confirms the booking
    Then the booking response status is 200
    And the booking status is "CONFIRMED"

  Scenario: Owner declines a booking request
    Given the renter has a pending booking for the listing
    And the owner is authenticated
    When the owner declines the booking
    Then the booking response status is 200
    And the booking status is "DECLINED"

  Scenario: Renter cancels a pending booking
    Given the renter has a pending booking for the listing
    And the renter is authenticated
    When the renter cancels the booking
    Then the booking response status is 200
    And the booking status is "CANCELLED"

  Scenario: Renter confirms pickup with correct PIN
    Given the renter has a confirmed booking
    When the renter confirms pickup with the correct PIN
    Then the booking response status is 200
    And the booking status is "ACTIVE"

  Scenario: Owner confirms item return
    Given there is an active booking
    And the owner is authenticated
    When the owner confirms the return
    Then the booking response status is 200
    And the booking status is "COMPLETED"

  Scenario: Owner cannot book their own listing
    Given the owner is authenticated
    When the owner tries to book their own listing
    Then the booking response status is 403

  Scenario: Booking fails when dates overlap existing booking
    Given the renter has a confirmed booking for dates 5 to 10 days from now
    And the renter is authenticated
    When the renter requests another booking overlapping those dates
    Then the booking response status is 409

  Scenario: Renter retrieves their own bookings
    Given the renter has a pending booking for the listing
    And the renter is authenticated
    When the renter requests their bookings
    Then the booking response status is 200
    And the response contains at least one booking

  Scenario: Owner retrieves bookings for their listing
    Given the renter has a pending booking for the listing
    And the owner is authenticated
    When the owner requests bookings for their listing
    Then the booking response status is 200
    And the response contains at least one booking

  Scenario: Anyone can check blocked periods for a listing
    Given the renter has a pending booking for the listing
    When anyone requests blocked periods for the listing
    Then the booking response status is 200
    And the response contains at least one blocked period
