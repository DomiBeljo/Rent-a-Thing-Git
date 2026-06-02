package org.example.rentathingproba;

import org.example.rentathingproba.dto.ThingDTO;
import org.example.rentathingproba.entities.Thing;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.mapper.ThingMapper;
import org.example.rentathingproba.repository.ThingRepository;
import org.example.rentathingproba.responses.ThingResponseDTO;
import org.example.rentathingproba.service.ThingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentAThingProbaApplicationTests {

    @Mock
    private ThingRepository thingRepository;

    @Mock
    private ThingMapper thingMapper;

    @InjectMocks
    private ThingService thingService;

    private User owner;
    private User otherUser;
    private Thing thing;
    private ThingDTO dto;

    @BeforeEach
    void setUp() {
        owner = new User();
        setId(owner, 1L);
        owner.setEmail("owner@gmail.com");

        otherUser = new User();
        setId(otherUser, 2L);
        otherUser.setEmail("otherUser@gmail.com");

        thing = new Thing();
        setId(thing, 10L);
        thing.setUser(owner);
        thing.setName("Drill");
        thing.setCategory("Tools");
        thing.setDescription("DeWalt Drill");

        dto = new ThingDTO();
        dto.setName("Drill");
        dto.setCategory("Tools");
        dto.setDescription("DeWalt Drill");
        dto.setImageUrls(List.of("http://img1.com", "http://img2.com"));
    }

    //Create Thing
    @Test
    @DisplayName("createThing - Saves a thing and returns DTO")
    void createThing_savesAndReturnsDTO() {
        ThingResponseDTO expected = mockResponse();
        when(thingMapper.toEntity(dto, owner)).thenReturn(thing);
        when(thingRepository.save(thing)).thenReturn(thing);
        when(thingMapper.toResponse(thing)).thenReturn(expected);

        ThingResponseDTO result = thingService.createThing(dto, owner);

        assertThat(result).isEqualTo(expected);
        verify(thingRepository).save(thing);
    }

    private void setId(Object entity, Long id) {
        try {
            var field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ThingResponseDTO mockResponse() {
        return new ThingResponseDTO(
                10L, "Bicikl", "Sport", "Trek 3500",
                List.of("http://img1.com"), LocalDateTime.now(), 1L, "owner@test.com"
        );
    }
}
