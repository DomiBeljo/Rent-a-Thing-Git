Feature: User Authentication
  As a new or existing user of Rent-a-Thing
  I want to register, verify my account, and log in
  So that I can access the platform securely

  Background:
    Given the application is running

  Scenario: Successful user registration
    When I register with username "testuser", email "testuser@example.com", and password "Password1!"
    Then the registration response status is 200
    And the response contains username "testuser"

  Scenario: Login with unverified account is rejected
    Given a user exists with email "unverified@example.com" and is not verified
    When I login with email "unverified@example.com" and password "Password1!"
    Then the response status is 403
    And the error code is "AUTH_ACCOUNT_NOT_VERIFIED"

  Scenario: Login with non-existent email is rejected
    When I login with email "nobody@example.com" and password "Password1!"
    Then the response status is 404
    And the error code is "USER_NOT_FOUND"

  Scenario: Resend verification code for unverified account
    Given a user exists with email "pending@example.com" and is not verified
    When I request a verification code resend for "pending@example.com"
    Then the response status is 200

