package org.example.rentathingproba.e2e.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class BookingChatStepDefinitions {

    private static final String BASE_URL = "http://localhost:8080";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
    private ResponseEntity<String> lastResponse;
    private HttpClientErrorException lastError;
    private String ownerToken;
    private String renterToken;
    private String thirdUserToken;
    private String activeToken;
    private Long ownerListingId;
    private Long lastBookingId;
    private Long lastConversationId;
    private Long firstConversationId;  // used for idempotency test

    @Before
    public void resetState() {
        lastResponse = null;
        lastError = null;
        lastBookingId = null;
        lastConversationId = null;
        firstConversationId = null;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders h = jsonHeaders();
        if (token != null) h.setBearerAuth(token);
        return h;
    }

    private void doPost(String path, Object body, String token) {
        try {
            lastError = null;
            lastResponse = restTemplate.exchange(
                    BASE_URL + path, HttpMethod.POST,
                    new HttpEntity<>(body, authHeaders(token)), String.class);
        } catch (HttpClientErrorException ex) {
            lastError = ex;
            lastResponse = null;
        }
    }

    private void doGet(String path, String token) {
        try {
            lastError = null;
            lastResponse = restTemplate.exchange(
                    BASE_URL + path, HttpMethod.GET,
                    new HttpEntity<>(authHeaders(token)), String.class);
        } catch (HttpClientErrorException ex) {
            lastError = ex;
            lastResponse = null;
        }
    }

    private int currentStatusCode() {
        if (lastError != null) return lastError.getStatusCode().value();
        return lastResponse != null ? lastResponse.getStatusCode().value() : -1;
    }

    private JsonNode currentBody() throws Exception {
        String body = lastError != null
                ? lastError.getResponseBodyAsString()
                : (lastResponse != null ? lastResponse.getBody() : "{}");
        return objectMapper.readTree(body);
    }

    private String registerAndLogin(String username, String email, String password) throws Exception {
        try {
            Map<String, Object> creds = Map.of("email", email, "password", password);
            ResponseEntity<String> resp = restTemplate.postForEntity(
                    BASE_URL + "/auth/login", new HttpEntity<>(creds, jsonHeaders()), String.class);
            return objectMapper.readTree(resp.getBody()).get("token").asText();
        } catch (HttpClientErrorException e) {
            Map<String, Object> reg = Map.of("username", username, "email", email, "password", password);
            restTemplate.postForEntity(BASE_URL + "/auth/signup", new HttpEntity<>(reg, jsonHeaders()), String.class);
            Map<String, Object> creds = Map.of("email", email, "password", password);
            ResponseEntity<String> resp = restTemplate.postForEntity(
                    BASE_URL + "/auth/login", new HttpEntity<>(creds, jsonHeaders()), String.class);
            return objectMapper.readTree(resp.getBody()).get("token").asText();
        }
    }

    private Long createThingAndListing(String token) throws Exception {
        Map<String, Object> thingBody = Map.of(
                "name", "E2E_BookingThing", "category", "Tools",
                "description", "E2E test thing", "imageUrls", java.util.List.of());
        ResponseEntity<String> thingResp = restTemplate.exchange(
                BASE_URL + "/things", HttpMethod.POST,
                new HttpEntity<>(thingBody, authHeaders(token)), String.class);
        Long thingId = objectMapper.readTree(thingResp.getBody()).get("thingId").asLong();

        Map<String, Object> listingBody = Map.of(
                "thingId", thingId, "price", 20.0,
                "location", "Zagreb", "securityDeposit", 50.0);
        ResponseEntity<String> listingResp = restTemplate.exchange(
                BASE_URL + "/listings", HttpMethod.POST,
                new HttpEntity<>(listingBody, authHeaders(token)), String.class);
        return objectMapper.readTree(listingResp.getBody()).get("listingId").asLong();
    }

    @Given("a listing owner is registered and logged in")
    public void ownerIsRegisteredAndLoggedIn() throws Exception {
        ownerToken = registerAndLogin("e2e_booking_owner", "e2e_booking_owner@example.com", "Password1!");
        ownerListingId = createThingAndListing(ownerToken);
    }

    @Given("a renter is registered and logged in")
    public void renterIsRegisteredAndLoggedIn() throws Exception {
        renterToken = registerAndLogin("e2e_booking_renter", "e2e_booking_renter@example.com", "Password1!");
    }

    @Given("the renter is authenticated")
    public void theRenterIsAuthenticated() {
        activeToken = renterToken;
    }

    @Given("the owner is authenticated")
    public void theOwnerIsAuthenticated() {
        activeToken = ownerToken;
    }

    @Given("a third user is registered and authenticated")
    public void thirdUserIsRegisteredAndAuthenticated() throws Exception {
        thirdUserToken = registerAndLogin("e2e_third_user", "e2e_third_user@example.com", "Password1!");
        activeToken = thirdUserToken;
    }

    @When("the renter requests a booking for the listing from tomorrow for {int} days")
    public void renterRequestsBooking(int days) {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(days - 1);
        Map<String, Object> body = Map.of(
                "listingId", ownerListingId,
                "startDate", start.toString(),
                "endDate", end.toString());
        doPost("/bookings/request", body, renterToken);
    }

    @Then("the booking response status is {int}")
    public void bookingResponseStatusIs(int expected) {
        assertThat(currentStatusCode()).isEqualTo(expected);
    }

    @Then("the booking status is {string}")
    public void bookingStatusIs(String status) throws Exception {
        assertThat(currentBody().get("status").asText()).isEqualTo(status);
    }

    @Given("the renter has a pending booking for the listing")
    public void renterHasPendingBooking() throws Exception {
        LocalDate start = LocalDate.now().plusDays(2);
        LocalDate end = start.plusDays(2);
        Map<String, Object> body = Map.of(
                "listingId", ownerListingId,
                "startDate", start.toString(),
                "endDate", end.toString());
        doPost("/bookings/request", body, renterToken);
        lastBookingId = currentBody().get("bookingId").asLong();
    }

    @Given("the renter has a confirmed booking")
    public void renterHasConfirmedBooking() throws Exception {
        renterHasPendingBooking();
        doPost("/bookings/" + lastBookingId + "/confirm", Map.of(), ownerToken);
    }

    @Given("there is an active booking")
    public void thereIsAnActiveBooking() throws Exception {
        renterHasConfirmedBooking();
        ResponseEntity<String> pinResp = restTemplate.exchange(
                BASE_URL + "/bookings/" + lastBookingId + "/pin", HttpMethod.GET,
                new HttpEntity<>(authHeaders(renterToken)), String.class);
        String pin = objectMapper.readTree(pinResp.getBody()).get("pin").asText();
        doPost("/bookings/" + lastBookingId + "/pickup", Map.of("pin", pin), renterToken);
    }

    @Given("the renter has a confirmed booking for dates {int} to {int} days from now")
    public void renterHasConfirmedBookingForDates(int startOffset, int endOffset) throws Exception {
        LocalDate start = LocalDate.now().plusDays(startOffset);
        LocalDate end = LocalDate.now().plusDays(endOffset);
        Map<String, Object> body = Map.of(
                "listingId", ownerListingId,
                "startDate", start.toString(),
                "endDate", end.toString());
        doPost("/bookings/request", body, renterToken);
        lastBookingId = currentBody().get("bookingId").asLong();
        doPost("/bookings/" + lastBookingId + "/confirm", Map.of(), ownerToken);
    }

    @When("the owner confirms the booking")
    public void ownerConfirmsBooking() {
        doPost("/bookings/" + lastBookingId + "/confirm", Map.of(), ownerToken);
    }

    @When("the owner declines the booking")
    public void ownerDeclinesBooking() {
        doPost("/bookings/" + lastBookingId + "/decline", Map.of(), ownerToken);
    }

    @When("the renter cancels the booking")
    public void renterCancelsBooking() {
        doPost("/bookings/" + lastBookingId + "/cancel", Map.of(), renterToken);
    }

    @When("the renter confirms pickup with the correct PIN")
    public void renterConfirmsPickupWithCorrectPin() throws Exception {
        ResponseEntity<String> pinResp = restTemplate.exchange(
                BASE_URL + "/bookings/" + lastBookingId + "/pin", HttpMethod.GET,
                new HttpEntity<>(authHeaders(renterToken)), String.class);
        String pin = objectMapper.readTree(pinResp.getBody()).get("pin").asText();
        doPost("/bookings/" + lastBookingId + "/pickup", Map.of("pin", pin), renterToken);
    }

    @When("the owner confirms the return")
    public void ownerConfirmsReturn() {
        doPost("/bookings/" + lastBookingId + "/return", Map.of(), ownerToken);
    }

    @When("the owner tries to book their own listing")
    public void ownerTriesToBookOwnListing() {
        LocalDate start = LocalDate.now().plusDays(1);
        LocalDate end = start.plusDays(2);
        Map<String, Object> body = Map.of(
                "listingId", ownerListingId,
                "startDate", start.toString(),
                "endDate", end.toString());
        doPost("/bookings/request", body, ownerToken);
    }

    @When("the renter requests another booking overlapping those dates")
    public void renterRequestsOverlappingBooking() {
        LocalDate start = LocalDate.now().plusDays(7);
        LocalDate end = LocalDate.now().plusDays(8);
        Map<String, Object> body = Map.of(
                "listingId", ownerListingId,
                "startDate", start.toString(),
                "endDate", end.toString());
        doPost("/bookings/request", body, renterToken);
    }

    @When("the renter requests their bookings")
    public void renterRequestsTheirBookings() {
        doGet("/bookings/my", renterToken);
    }

    @When("the owner requests bookings for their listing")
    public void ownerRequestsBookingsForListing() {
        doGet("/bookings/listing/" + ownerListingId, ownerToken);
    }

    @When("anyone requests blocked periods for the listing")
    public void anyoneRequestsBlockedPeriods() {
        doGet("/bookings/listing/" + ownerListingId + "/blocked", null);
    }

    @Then("the response contains at least one booking")
    public void responseContainsAtLeastOneBooking() throws Exception {
        JsonNode body = currentBody();
        assertThat(body.isArray()).isTrue();
        assertThat(body.size()).isGreaterThan(0);
    }

    @Then("the response contains at least one blocked period")
    public void responseContainsAtLeastOneBlockedPeriod() throws Exception {
        JsonNode body = currentBody();
        assertThat(body.isArray()).isTrue();
        assertThat(body.size()).isGreaterThan(0);
    }

    @When("the renter opens a conversation for the listing")
    public void renterOpensConversation() throws Exception {
        Map<String, Object> body = Map.of("listingId", ownerListingId);
        doPost("/conversations", body, renterToken);
        if (lastResponse != null) {
            lastConversationId = currentBody().get("conversationId").asLong();
        }
    }

    @When("the renter opens a conversation for the listing again")
    public void renterOpensConversationAgain() throws Exception {
        firstConversationId = lastConversationId;
        renterOpensConversation();
    }

    @Then("the conversation response status is {int}")
    public void conversationResponseStatusIs(int expected) {
        assertThat(currentStatusCode()).isEqualTo(expected);
    }

    @Then("the response contains a conversation ID")
    public void responseContainsConversationId() throws Exception {
        assertThat(currentBody().has("conversationId")).isTrue();
        assertThat(currentBody().get("conversationId").asLong()).isGreaterThan(0);
    }

    @Then("both conversation IDs are the same")
    public void bothConversationIdsAreTheSame() {
        assertThat(lastConversationId).isEqualTo(firstConversationId);
    }

    @Given("the renter has an open conversation for the listing")
    public void renterHasOpenConversation() throws Exception {
        renterOpensConversation();
    }

    @Given("the renter sends the message {string}")
    public void renterSendsMessage(String content) {
        doPost("/conversations/" + lastConversationId + "/messages",
                Map.of("content", content), renterToken);
    }

    @When("the renter sends the message {string}")
    public void whenRenterSendsMessage(String content) {
        doPost("/conversations/" + lastConversationId + "/messages",
                Map.of("content", content), renterToken);
    }

    @When("the owner sends the message {string}")
    public void ownerSendsMessage(String content) {
        doPost("/conversations/" + lastConversationId + "/messages",
                Map.of("content", content), ownerToken);
    }

    @Then("the message content is {string}")
    public void messageContentIs(String expected) throws Exception {
        assertThat(currentBody().get("content").asText()).isEqualTo(expected);
    }

    @When("the renter requests messages in the conversation")
    public void renterRequestsMessages() {
        doGet("/conversations/" + lastConversationId + "/messages", renterToken);
    }

    @Then("the response contains at least one message")
    public void responseContainsAtLeastOneMessage() throws Exception {
        JsonNode body = currentBody();
        assertThat(body.isArray()).isTrue();
        assertThat(body.size()).isGreaterThan(0);
    }

    @When("the renter requests their conversations")
    public void renterRequestsTheirConversations() {
        doGet("/conversations", renterToken);
    }

    @Then("the response contains at least one conversation")
    public void responseContainsAtLeastOneConversation() throws Exception {
        JsonNode body = currentBody();
        assertThat(body.isArray()).isTrue();
        assertThat(body.size()).isGreaterThan(0);
    }

    @When("the third user tries to read messages in that conversation")
    public void thirdUserTriesToReadMessages() {
        doGet("/conversations/" + lastConversationId + "/messages", thirdUserToken);
    }

    @When("the owner tries to open a conversation for their own listing")
    public void ownerTriesToOpenOwnConversation() {
        Map<String, Object> body = Map.of("listingId", ownerListingId);
        doPost("/conversations", body, ownerToken);
    }

    @When("the owner marks the conversation as read")
    public void ownerMarksConversationAsRead() {
        doPost("/conversations/" + lastConversationId + "/read", Map.of(), ownerToken);
    }

    @Then("the mark-read response status is {int}")
    public void markReadResponseStatusIs(int expected) {
        assertThat(currentStatusCode()).isEqualTo(expected);
    }
}