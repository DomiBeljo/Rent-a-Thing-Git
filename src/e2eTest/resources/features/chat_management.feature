Feature: Chat and Conversations
  As an authenticated user
  I want to start and participate in conversations about listings
  So that I can communicate with owners and renters

  Background:
    Given the application is running
    And a listing owner is registered and logged in
    And a renter is registered and logged in

  Scenario: Renter opens a conversation for a listing
    Given the renter is authenticated
    When the renter opens a conversation for the listing
    Then the conversation response status is 200
    And the response contains a conversation ID

  Scenario: Opening the same conversation twice returns the same conversation
    Given the renter is authenticated
    When the renter opens a conversation for the listing
    And the renter opens a conversation for the listing again
    Then both conversation IDs are the same

  Scenario: Renter sends a message in a conversation
    Given the renter has an open conversation for the listing
    And the renter is authenticated
    When the renter sends the message "Is this available next week?"
    Then the conversation response status is 200
    And the message content is "Is this available next week?"

  Scenario: Owner replies in a conversation
    Given the renter has an open conversation for the listing
    And the renter sends the message "Is this available?"
    And the owner is authenticated
    When the owner sends the message "Yes, it is available!"
    Then the conversation response status is 200
    And the message content is "Yes, it is available!"

  Scenario: User retrieves messages in a conversation
    Given the renter has an open conversation for the listing
    And the renter sends the message "Hello"
    And the renter is authenticated
    When the renter requests messages in the conversation
    Then the conversation response status is 200
    And the response contains at least one message

  Scenario: User retrieves their conversation list
    Given the renter has an open conversation for the listing
    And the renter is authenticated
    When the renter requests their conversations
    Then the conversation response status is 200
    And the response contains at least one conversation

  Scenario: Non-participant cannot read messages
    Given the renter has an open conversation for the listing
    And a third user is registered and authenticated
    When the third user tries to read messages in that conversation
    Then the conversation response status is 403

  Scenario: Owner cannot start a conversation on their own listing
    Given the owner is authenticated
    When the owner tries to open a conversation for their own listing
    Then the conversation response status is 403

  Scenario: Mark messages as read
    Given the renter has an open conversation for the listing
    And the renter sends the message "New message"
    And the owner is authenticated
    When the owner marks the conversation as read
    Then the mark-read response status is 204
