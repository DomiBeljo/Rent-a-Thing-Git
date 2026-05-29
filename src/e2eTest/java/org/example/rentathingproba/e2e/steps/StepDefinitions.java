package org.example.rentathingproba.e2e.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class StepDefinitions {

    private static final String BASE_URL = "http://localhost:8080";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ResponseEntity<String> lastResponse;
    private HttpClientErrorException lastError;
    private String authToken;
    private Long lastThingId;
    private Long lastListingId;
    private Long otherUserThingId;
    private Long otherUserListingId;

    @Before
    public void resetState() {
        lastResponse = null;
        lastError = null;
    }

    //Helpers
    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = jsonHeaders();
        if (authToken != null) {
            headers.setBearerAuth(authToken);
        }
        return headers;
    }

    private void doPost(String path, Map<String, Object> body) {
        doPost(path, body, jsonHeaders());
    }

    private void doPost(String path, Map<String, Object> body, HttpHeaders headers) {
        try {
            lastError = null;
            lastResponse = restTemplate.exchange(
                    BASE_URL + path, HttpMethod.POST,
                    new HttpEntity<>(body, headers), String.class);
        } catch (HttpClientErrorException ex) {
            lastError = ex;
            lastResponse = null;
        }
    }

    private void doPut(String path, Map<String, Object> body) {
        try {
            lastError = null;
            lastResponse = restTemplate.exchange(
                    BASE_URL + path, HttpMethod.PUT,
                    new HttpEntity<>(body, authHeaders()), String.class);
        } catch (HttpClientErrorException ex) {
            lastError = ex;
            lastResponse = null;
        }
    }

    private void doDelete(String path) {
        try {
            lastError = null;
            lastResponse = restTemplate.exchange(
                    BASE_URL + path, HttpMethod.DELETE,
                    new HttpEntity<>(authHeaders()), String.class);
        } catch (HttpClientErrorException ex) {
            lastError = ex;
            lastResponse = null;
        }
    }

    private void doGet(String path) {
        try {
            lastError = null;
            lastResponse = restTemplate.exchange(
                    BASE_URL + path, HttpMethod.GET,
                    new HttpEntity<>(authHeaders()), String.class);
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

    private void loginAs(String email, String password) throws Exception {
        Map<String, Object> creds = Map.of("email", email, "password", password);
        ResponseEntity<String> resp = restTemplate.postForEntity(
                BASE_URL + "/auth/login", new HttpEntity<>(creds, jsonHeaders()), String.class);
        JsonNode node = objectMapper.readTree(resp.getBody());
        authToken = node.get("token").asText();
    }

    //Background

    @Given("the application is running")
    public void theApplicationIsRunning() {
    }

    @Given("I am logged in as {string} with password {string}")
    public void iAmLoggedIn(String email, String password) throws Exception {
        try {
            loginAs(email, password);
        } catch (Exception e) {
            Map<String, Object> reg = new HashMap<>();
            reg.put("email", email);
            reg.put("password", password);
            reg.put("username", email.split("@")[0]);
            restTemplate.postForEntity(BASE_URL + "/auth/signup", new HttpEntity<>(reg, jsonHeaders()), String.class);
            loginAs(email, password);
        }
    }

    //Authentication steps
    @When("I register with username {string}, email {string}, and password {string}")
    public void iRegister(String username, String email, String password) {
        Map<String, Object> body = Map.of("username", username, "email", email, "password", password);
        doPost("/auth/signup", body);
    }

    @Then("the registration response status is {int}")
    public void theRegistrationResponseStatusIs(int status) {
        assertThat(currentStatusCode()).isEqualTo(status);
    }

    @Then("the response contains username {string}")
    public void theResponseContainsUsername(String username) throws Exception {
        assertThat(currentBody().get("username").asText()).isEqualTo(username);
    }

    @Given("a user exists with email {string} and is not verified")
    public void aUserExistsAndIsNotVerified(String email) {
        Map<String, Object> body = Map.of("username", email.split("@")[0], "email", email, "password", "Password1!");
        try {
            doPost("/auth/signup", body);
        } catch (Exception ignored) { /* already exists */ }
    }

    @When("I login with email {string} and password {string}")
    public void iLogin(String email, String password) {
        doPost("/auth/login", Map.of("email", email, "password", password));
    }

    @Then("the response status is {int}")
    public void theResponseStatusIs(int status) {
        assertThat(currentStatusCode()).isEqualTo(status);
    }

    @Then("the error code is {string}")
    public void theErrorCodeIs(String code) throws Exception {
        assertThat(currentBody().get("errorCode").asText()).isEqualTo(code);
    }

    @When("I request a verification code resend for {string}")
    public void iResendVerificationCode(String email) {
        doPost("/auth/resend", Map.of("email", email));
    }

    //Thing management steps

    @Given("I have a thing available to list")
    public void iHaveAThingAvailableToList() throws Exception {
        Map<String, Object> body = Map.of(
                "name", "ListingThing",
                "category", "Tools",
                "description", "A thing for listing tests",
                "imageUrls", java.util.List.of()
        );
        doPost("/things", body, authHeaders());
        lastThingId = currentBody().get("thingId").asLong();
    }

    @When("I create a thing with name {string}, category {string}, and description {string}")
    public void iCreateAThing(String name, String category, String description) throws Exception {
        Map<String, Object> body = Map.of(
                "name", name, "category", category, "description", description,
                "imageUrls", java.util.List.of()
        );
        doPost("/things", body, authHeaders());
        if (lastResponse != null) {
            lastThingId = currentBody().get("thingId").asLong();
        }
    }

    @Then("the thing response contains name {string} and category {string}")
    public void theThingResponseContainsNameAndCategory(String name, String category) throws Exception {
        JsonNode body = currentBody();
        assertThat(body.get("name").asText()).isEqualTo(name);
        assertThat(body.get("category").asText()).isEqualTo(category);
    }

    @Given("I have created a thing with name {string}")
    public void iHaveCreatedAThingWithName(String name) throws Exception {
        iCreateAThing(name, "TestCategory", "Test description");
    }

    @When("I request the thing by its ID")
    public void iRequestTheThingById() {
        doGet("/things/" + lastThingId);
    }

    @Then("the thing response contains name {string}")
    public void theThingResponseContainsName(String name) throws Exception {
        assertThat(currentBody().get("name").asText()).isEqualTo(name);
    }

    @When("I update the thing name to {string}")
    public void iUpdateTheThingName(String newName) {
        doPut("/things/" + lastThingId,
                Map.of("name", newName, "category", "Tools", "description", "Updated", "imageUrls", java.util.List.of()));
    }

    @When("I delete the thing")
    public void iDeleteTheThing() {
        doDelete("/things/" + lastThingId);
    }

    @Given("another user owns a thing")
    public void anotherUserOwnsAThing() throws Exception {
        String otherEmail = "another_thing_owner@example.com";
        String savedToken = authToken;
        authToken = null;
        try { loginAs(otherEmail, "Password1!"); } catch (Exception e) {
            Map<String, Object> reg = Map.of("username", "another_thing_owner", "email", otherEmail, "password", "Password1!");
            restTemplate.postForEntity(BASE_URL + "/auth/signup", new HttpEntity<>(reg, jsonHeaders()), String.class);
            loginAs(otherEmail, "Password1!");
        }
        iCreateAThing("OtherThing", "OtherCat", "Other desc");
        otherUserThingId = lastThingId;
        authToken = savedToken; // switch back to original user
    }

    @When("I try to delete that thing")
    public void iTryToDeleteThatThing() {
        doDelete("/things/" + otherUserThingId);
    }

    //Listing management steps
    @When("I create a listing with price {double}, location {string}, and deposit {double}")
    public void iCreateAListing(double price, String location, double deposit) throws Exception {
        Map<String, Object> body = Map.of(
                "thingId", lastThingId,
                "price", price,
                "location", location,
                "securityDeposit", deposit
        );
        doPost("/listings", body, authHeaders());
        if (lastResponse != null) {
            lastListingId = currentBody().get("listingId").asLong();
        }
    }

    @Then("the listing response contains location {string} and isAvailable {word}")
    public void theListingResponseContainsLocationAndAvailability(String location, String available) throws Exception {
        JsonNode body = currentBody();
        assertThat(body.get("location").asText()).isEqualTo(location);
        assertThat(body.get("available").asBoolean()).isEqualTo(Boolean.parseBoolean(available));
    }

    @Given("I have an existing listing")
    public void iHaveAnExistingListing() throws Exception {
        iCreateAListing(10.0, "Zagreb", 25.0);
    }

    @Given("I have an existing listing that is available")
    public void iHaveAnExistingListingThatIsAvailable() throws Exception {
        iHaveAnExistingListing();
    }

    @When("I update the listing price to {double} and location to {string}")
    public void iUpdateTheListing(double price, String location) {
        doPut("/listings/" + lastListingId,
                Map.of("thingId", lastThingId, "price", price, "location", location, "securityDeposit", 0.0));
    }

    @Then("the listing response contains price {double} and location {string}")
    public void theListingResponseContainsPriceAndLocation(double price, String location) throws Exception {
        JsonNode body = currentBody();
        assertThat(body.get("price").asDouble()).isEqualTo(price);
        assertThat(body.get("location").asText()).isEqualTo(location);
    }

    @When("I toggle the listing availability")
    public void iToggleListingAvailability() {
        doPut("/listings/" + lastListingId + "/availability", Map.of());
    }

    @Then("the listing is now unavailable")
    public void theListingIsNowUnavailable() {
        assertThat(currentStatusCode()).isEqualTo(200);
    }

    @Given("I have an existing listing for a thing named {string}")
    public void iHaveAnExistingListingForAThing(String thingName) throws Exception {
        iCreateAThing(thingName, "Tools", "A " + thingName);
        iCreateAListing(10.0, "Zagreb", 25.0);
    }

    @When("I search listings with query {string}")
    public void iSearchListings(String query) {
        doGet("/listings/search?query=" + query);
    }

    @Then("the search results contain a listing for {string}")
    public void theSearchResultsContain(String thingName) throws Exception {
        JsonNode results = currentBody();
        assertThat(results.isArray()).isTrue();
        boolean found = false;
        for (JsonNode node : results) {
            if (node.has("name") && node.get("name").asText().equalsIgnoreCase(thingName)) {
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();
    }

    @Given("there are available listings in the system")
    public void thereAreAvailableListings() throws Exception {
        iHaveAnExistingListing();
    }

    @When("I request recommended listings")
    public void iRequestRecommendedListings() {
        doGet("/listings/recommended");
    }

    @Given("another user owns a listing")
    public void anotherUserOwnsAListing() throws Exception {
        String otherEmail = "another_listing_owner@example.com";
        String savedToken = authToken;
        authToken = null;
        try { loginAs(otherEmail, "Password1!"); } catch (Exception e) {
            Map<String, Object> reg = Map.of("username", "another_listing_owner", "email", otherEmail, "password", "Password1!");
            restTemplate.postForEntity(BASE_URL + "/auth/signup", new HttpEntity<>(reg, jsonHeaders()), String.class);
            loginAs(otherEmail, "Password1!");
        }
        iHaveAThingAvailableToList();
        iCreateAListing(5.0, "Osijek", 10.0);
        otherUserListingId = lastListingId;
        authToken = savedToken;
    }

    @When("I try to update that listing")
    public void iTryToUpdateThatListing() {
        doPut("/listings/" + otherUserListingId,
                Map.of("thingId", 1L, "price", 99.0, "location", "Hack", "securityDeposit", 0.0));
    }
}