package org.example.rentathingproba.unit.mapper;

import org.example.rentathingproba.dto.ThingDTO;
import org.example.rentathingproba.entities.Thing;
import org.example.rentathingproba.entities.ThingImage;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.mapper.ThingMapper;
import org.example.rentathingproba.responses.ThingResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;


@DisplayName("ThingMapper Unit Tests")
class ThingMapperTest {

    private ThingMapper thingMapper;
    private User owner;

    @BeforeEach
    void setUp() {
        thingMapper = new ThingMapper();
        owner = User.builder().id(1L).username("domTorretto").email("domTorretto@gmail.com").build();
    }

    private ThingDTO buildDto(String name, String category, String description, List<String> urls) {
        ThingDTO dto = new ThingDTO();
        dto.setName(name);
        dto.setCategory(category);
        dto.setDescription(description);
        dto.setImageUrls(urls);
        return dto;
    }

    //To entity
    @Test
    @DisplayName("toEntity: maps all fields from DTO and owner correctly")
    void toEntity_mapsAllFields() {
        ThingDTO dto = buildDto("Drill", "Tools", "A drill", List.of("url1", "url2"));

        Thing result = thingMapper.toEntity(dto, owner);

        assertThat(result.getName()).isEqualTo("Drill");
        assertThat(result.getCategory()).isEqualTo("Tools");
        assertThat(result.getDescription()).isEqualTo("A drill");
        assertThat(result.getUser()).isEqualTo(owner);
        assertThat(result.getImages()).hasSize(2);
        assertThat(result.getImages().get(0).getUrl()).isEqualTo("url1");
        assertThat(result.getImages().get(1).getUrl()).isEqualTo("url2");
    }

    @Test
    @DisplayName("toEntity: produces empty images list when imageUrls is null")
    void toEntity_noImagesWhenUrlsNull() {
        ThingDTO dto = buildDto("Bike", "Vehicles", "A bike", null);

        Thing result = thingMapper.toEntity(dto, owner);

        assertThat(result.getImages()).isEmpty();
    }


    @Test
    @DisplayName("toEntity: produces empty images list when imageUrls is empty")
    void toEntity_noImagesWhenUrlsEmpty() {
        ThingDTO dto = buildDto("Bike", "Vehicles", "A bike", List.of());

        Thing result = thingMapper.toEntity(dto, owner);

        assertThat(result.getImages()).isEmpty();
    }

    @Test
    @DisplayName("toEntity: assigns correct sortOrder to images")
    void toEntity_assignsCorrectSortOrder() {
        ThingDTO dto = buildDto("Camera", "Electronics", "A camera", List.of("a", "b", "c"));

        Thing result = thingMapper.toEntity(dto, owner);

        List<ThingImage> images = result.getImages();
        assertThat(images.get(0).getSortOrder()).isEqualTo(0);
        assertThat(images.get(1).getSortOrder()).isEqualTo(1);
        assertThat(images.get(2).getSortOrder()).isEqualTo(2);
    }

    //Update entity
    @Test
    @DisplayName("updateEntity: updates scalar fields and replaces images when new URLs provided")
    void updateEntity_updatesFieldsAndImages() {
        Thing thing = thingMapper.toEntity(buildDto("OldName", "OldCat", "OldDesc", List.of("old")), owner);
        ThingDTO updateDto = buildDto("NewName", "NewCat", "NewDesc", List.of("new1", "new2"));

        thingMapper.updateEntity(thing, updateDto);

        assertThat(thing.getName()).isEqualTo("NewName");
        assertThat(thing.getCategory()).isEqualTo("NewCat");
        assertThat(thing.getDescription()).isEqualTo("NewDesc");
        assertThat(thing.getImages()).hasSize(2);
        assertThat(thing.getImages().get(0).getUrl()).isEqualTo("new1");
    }

    @Test
    @DisplayName("updateEntity: keeps existing images when new imageUrls is null")
    void updateEntity_keepsImagesWhenUrlsNull() {
        Thing thing = thingMapper.toEntity(buildDto("Drill", "Tools", "Desc", List.of("existing")), owner);
        ThingDTO updateDto = buildDto("DrillUpdated", "Tools", "Updated", null);

        thingMapper.updateEntity(thing, updateDto);

        assertThat(thing.getImages()).hasSize(1);
        assertThat(thing.getImages().get(0).getUrl()).isEqualTo("existing");
    }

    //To response
    @Test
    @DisplayName("toResponse: maps all fields from entity to DTO correctly")
    void toResponse_mapsAllFields() {
        ThingDTO dto = buildDto("Drill", "Tools", "A drill", List.of("img1"));
        Thing thing = thingMapper.toEntity(dto, owner);
        thing.setId(42L);
        thing.setCreatedAt(LocalDateTime.of(2024, 1, 1, 12, 0));

        ThingResponseDTO response = thingMapper.toResponse(thing);

        assertThat(response.getThingId()).isEqualTo(42L);
        assertThat(response.getName()).isEqualTo("Drill");
        assertThat(response.getCategory()).isEqualTo("Tools");
        assertThat(response.getDescription()).isEqualTo("A drill");
        assertThat(response.getImageUrls()).containsExactly("img1");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getOwnerUsername()).isEqualTo("domTorretto@gmail.com");
    }

    @Test
    @DisplayName("toResponse: returns empty imageUrls when thing has no images")
    void toResponse_returnsEmptyUrlsWhenNoImages() {
        Thing thing = thingMapper.toEntity(buildDto("Drone", "Tech", "Desc", null), owner);
        thing.setId(5L);
        thing.setCreatedAt(LocalDateTime.now());

        ThingResponseDTO response = thingMapper.toResponse(thing);

        assertThat(response.getImageUrls()).isEmpty();
    }
}
