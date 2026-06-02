package org.example.rentathingproba.unit.mapper;

import org.example.rentathingproba.dto.ListingDTO;
import org.example.rentathingproba.entities.Listing;
import org.example.rentathingproba.entities.Thing;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.mapper.ListingMapper;
import org.example.rentathingproba.mapper.ThingMapper;
import org.example.rentathingproba.responses.ListingResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ListingMapper Unit Tests")
class ListingMapperTest {

    private ListingMapper listingMapper;
    private ThingMapper thingMapper;

    private User owner;
    private Thing thing;

    @BeforeEach
    void setUp() {
        listingMapper = new ListingMapper();
        thingMapper = new ThingMapper();

        owner = User.builder().id(1L).username("domTorretto").email("domTorretto@gmail.com").build();

        var thingDto = new org.example.rentathingproba.dto.ThingDTO();
        thingDto.setName("Drill");
        thingDto.setCategory("Tools");
        thingDto.setDescription("A drill");
        thingDto.setImageUrls(List.of("img1", "img2"));
        thing = thingMapper.toEntity(thingDto, owner);
        thing.setId(10L);
    }

    private ListingDTO buildDto(Long thingId, double price, String location, double deposit) {
        ListingDTO dto = new ListingDTO();
        dto.setThingId(thingId);
        dto.setPrice(BigDecimal.valueOf(price));
        dto.setLocation(location);
        dto.setSecurityDeposit(BigDecimal.valueOf(deposit));
        return dto;
    }

    //To entity
    @Test
    @DisplayName("toEntity: price, location, deposit, isAvailable")
    void toEntity_mapsAllFieldsAndDefaultsAvailableToTrue() {
        ListingDTO dto = buildDto(10L, 15.0, "Zagreb", 50.0);

        Listing result = listingMapper.toEntity(dto, owner, thing);

        assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(15.0));
        assertThat(result.getLocation()).isEqualTo("Zagreb");
        assertThat(result.getSecurityDeposit()).isEqualByComparingTo(BigDecimal.valueOf(50.0));
        assertThat(result.getIsAvailable()).isTrue();
        assertThat(result.getUser()).isEqualTo(owner);
        assertThat(result.getThings()).isEqualTo(thing);
    }

    @Test
    @DisplayName("toEntity: copies images from Thing into Listing")
    void toEntity_copiesImagesFromThing() {
        ListingDTO dto = buildDto(10L, 10.0, "Split", 0);

        Listing result = listingMapper.toEntity(dto, owner, thing);

        assertThat(result.getImages()).hasSize(2);
        assertThat(result.getImages().get(0).getUrl()).isEqualTo("img1");
        assertThat(result.getImages().get(1).getUrl()).isEqualTo("img2");
    }

    @Test
    @DisplayName("toEntity: produces empty images list when Thing has no images")
    void toEntity_emptyImagesWhenThingHasNoImages() {
        var thingDto = new org.example.rentathingproba.dto.ThingDTO();
        thingDto.setName("Box");
        thingDto.setCategory("Storage");
        thingDto.setDescription("A box");
        thingDto.setImageUrls(null);
        Thing emptyThing = thingMapper.toEntity(thingDto, owner);
        emptyThing.setId(20L);

        ListingDTO dto = buildDto(20L, 5.0, "Rijeka", 0);
        Listing result = listingMapper.toEntity(dto, owner, emptyThing);

        assertThat(result.getImages()).isEmpty();
    }

    //Update entity
    @Test
    @DisplayName("updateEntity: updates price, location, and securityDeposit")
    void updateEntity_updatesScalarFields() {
        ListingDTO dto = buildDto(10L, 15.0, "Zagreb", 50.0);
        Listing listing = listingMapper.toEntity(dto, owner, thing);
        listing.setId(1L);
        listing.setCreatedAt(LocalDateTime.now());

        ListingDTO updateDto = buildDto(10L, 20.0, "Split", 100.0);
        listingMapper.updateEntity(listing, updateDto);

        assertThat(listing.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(20.0));
        assertThat(listing.getLocation()).isEqualTo("Split");
        assertThat(listing.getSecurityDeposit()).isEqualByComparingTo(BigDecimal.valueOf(100.0));
    }

    @Test
    @DisplayName("updateEntity: does not change isAvailable when updating")
    void updateEntity_doesNotChangeAvailability() {
        ListingDTO dto = buildDto(10L, 15.0, "Zagreb", 50.0);
        Listing listing = listingMapper.toEntity(dto, owner, thing);
        listing.setIsAvailable(false); // manually disabled

        listingMapper.updateEntity(listing, buildDto(10L, 25.0, "Osijek", 0));

        assertThat(listing.getIsAvailable()).isFalse();
    }

    //To response
    @Test
    @DisplayName("toResponse: maps all listing fields into response DTO correctly")
    void toResponse_mapsAllFields() {
        ListingDTO dto = buildDto(10L, 15.0, "Zagreb", 50.0);
        Listing listing = listingMapper.toEntity(dto, owner, thing);
        listing.setId(100L);
        listing.setCreatedAt(LocalDateTime.of(2024, 6, 1, 10, 0));

        ListingResponseDTO response = listingMapper.toResponse(listing);

        assertThat(response.getListingId()).isEqualTo(100L);
        assertThat(response.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(15.0));
        assertThat(response.getLocation()).isEqualTo("Zagreb");
        assertThat(response.getSecurityDeposit()).isEqualByComparingTo(BigDecimal.valueOf(50.0));
        assertThat(response.getThingId()).isEqualTo(10L);
        assertThat(response.getName()).isEqualTo("Drill");
        assertThat(response.getCategory()).isEqualTo("Tools");
        assertThat(response.getDescription()).isEqualTo("A drill");
        assertThat(response.getImageUrls()).containsExactly("img1", "img2");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getUserName()).isEqualTo("domTorretto");
    }
}
