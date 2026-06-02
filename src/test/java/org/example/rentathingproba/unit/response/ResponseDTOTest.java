package org.example.rentathingproba.unit.response;

import org.example.rentathingproba.exceptions.ErrorCode;
import org.example.rentathingproba.responses.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Response DTO Unit Tests")
class ResponseDTOTest {

    @Test
    @DisplayName("UserResponseDTO: constructor and getters work correctly")
    void userResponseDTO_constructorAndGetters() {
        UserResponseDTO dto = new UserResponseDTO(1L, "alice", "alice@example.com", 3.4, 4, 3, 22);
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getUsername()).isEqualTo("alice");
        assertThat(dto.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("LoginResponse: constructor and getters work correctly")
    void loginResponse_constructorAndGetters() {
        LoginResponse r = new LoginResponse("tok", 3600L, 1L, "alice", "alice@example.com");
        assertThat(r.getToken()).isEqualTo("tok");
        assertThat(r.getExpiresIn()).isEqualTo(3600L);
        assertThat(r.getUserId()).isEqualTo(1L);
        assertThat(r.getUsername()).isEqualTo("alice");
        assertThat(r.getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    @DisplayName("MessageResponse: constructor and getter work correctly")
    void messageResponse_constructorAndGetter() {
        MessageResponse r = new MessageResponse("OK");
        assertThat(r.getMessage()).isEqualTo("OK");
    }

    @Test
    @DisplayName("ErrorResponse: constructor sets all fields including timestamp")
    void errorResponse_constructorSetsFields() {
        ErrorResponse r = new ErrorResponse(404, "Not Found", ErrorCode.LISTING_NOT_FOUND, "Listing missing");
        assertThat(r.getStatus()).isEqualTo(404);
        assertThat(r.getError()).isEqualTo("Not Found");
        assertThat(r.getCode()).isEqualTo(ErrorCode.LISTING_NOT_FOUND);
        assertThat(r.getMessage()).isEqualTo("Listing missing");
        assertThat(r.getTimestamp()).isNotNull().isBefore(LocalDateTime.now().plusSeconds(1));
    }

    @Test
    @DisplayName("ListingResponseDTO: constructor and getters work correctly")
    void listingResponseDTO_constructorAndGetters() {
        LocalDateTime now = LocalDateTime.now();
        ListingResponseDTO dto = new ListingResponseDTO(
                1L, BigDecimal.TEN, "Zagreb", true, now,
                BigDecimal.valueOf(50), 2L, "Drill", "Tools", "A drill", List.of(), 3L, "alice");
        assertThat(dto.getListingId()).isEqualTo(1L);
        assertThat(dto.getLocation()).isEqualTo("Zagreb");
        assertThat(dto.getUserName()).isEqualTo("alice");
    }

    @Test
    @DisplayName("ThingResponseDTO: constructor and getters work correctly")
    void thingResponseDTO_constructorAndGetters() {
        LocalDateTime now = LocalDateTime.now();
        ThingResponseDTO dto = new ThingResponseDTO(1L, "Drill", "Tools", "A drill", List.of(), now, 2L, "alice");
        assertThat(dto.getThingId()).isEqualTo(1L);
        assertThat(dto.getName()).isEqualTo("Drill");
        assertThat(dto.getOwnerUsername()).isEqualTo("alice");
    }
}
