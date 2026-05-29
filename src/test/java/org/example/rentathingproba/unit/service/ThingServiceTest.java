package org.example.rentathingproba.unit.service;

import org.example.rentathingproba.dto.ThingDTO;
import org.example.rentathingproba.entities.Thing;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.exceptions.ThingNotFoundException;
import org.example.rentathingproba.exceptions.ThingOwnershipException;
import org.example.rentathingproba.mapper.ThingMapper;
import org.example.rentathingproba.repository.ThingRepository;
import org.example.rentathingproba.responses.ThingResponseDTO;
import org.example.rentathingproba.service.application.ThingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ThingSercice Unit Tests")
class ThingServiceTest {

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
    private ThingResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        owner = User.builder().id(1L).username("owner").email("owner@example.com").build();
        otherUser = User.builder().id(2L).username("other").email("other@example.com").build();

        thing = new Thing();
        thing.setId(10L);
        thing.setUser(owner);
        thing.setName("Drill");
        thing.setCategory("Tools");
        thing.setDescription("A powerful drill");
        thing.setCreatedAt(LocalDateTime.now());

        dto = new ThingDTO();
        dto.setName("Drill");
        dto.setCategory("Tools");
        dto.setDescription("A powerful drill");

        responseDTO = new ThingResponseDTO(10L, "Drill", "Tools", "A powerful drill",
                List.of(), LocalDateTime.now(), 1L, "owner");
    }

    //Create thing
    @Test
    @DisplayName("createThing: maps DTO, saves, and returns response")
    void createThing_savesAndReturnsResponse() {
        when(thingMapper.toEntity(dto, owner)).thenReturn(thing);
        when(thingRepository.save(thing)).thenReturn(thing);
        when(thingMapper.toResponse(thing)).thenReturn(responseDTO);

        ThingResponseDTO result = thingService.createThing(dto, owner);

        assertThat(result).isEqualTo(responseDTO);
        verify(thingMapper).toEntity(dto, owner);
        verify(thingRepository).save(thing);
        verify(thingMapper).toResponse(thing);
    }

    //Update thing
    @Test
    @DisplayName("updateThing: updates entity and returns new response when owner matches")
    void updateThing_updatesAndReturnsResponseForOwner() {
        when(thingRepository.findById(10L)).thenReturn(Optional.of(thing));
        when(thingRepository.save(thing)).thenReturn(thing);
        when(thingMapper.toResponse(thing)).thenReturn(responseDTO);

        ThingResponseDTO result = thingService.updateThing(10L, dto, owner);

        assertThat(result).isEqualTo(responseDTO);
        verify(thingMapper).updateEntity(thing, dto);
        verify(thingRepository).save(thing);
    }

    @Test
    @DisplayName("updateThing: throws ThingNotFoundException when thing does not exist")
    void updateThing_throwsThingNotFound() {
        when(thingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> thingService.updateThing(99L, dto, owner))
                .isInstanceOf(ThingNotFoundException.class);
    }

    @Test
    @DisplayName("updateThing: throws ThingOwnershipException when user is not the owner")
    void updateThing_throwsOwnershipExceptionForNonOwner() {
        when(thingRepository.findById(10L)).thenReturn(Optional.of(thing));

        assertThatThrownBy(() -> thingService.updateThing(10L, dto, otherUser))
                .isInstanceOf(ThingOwnershipException.class);
        verify(thingRepository, never()).save(any());
    }

    //Get thing by ID
    @Test
    @DisplayName("getThingById: returns response when thing exists")
    void getThingById_returnsResponseForExistingThing() {
        when(thingRepository.findById(10L)).thenReturn(Optional.of(thing));
        when(thingMapper.toResponse(thing)).thenReturn(responseDTO);

        ThingResponseDTO result = thingService.getThingById(10L);

        assertThat(result).isEqualTo(responseDTO);
    }

    @Test
    @DisplayName("getThingById: throws ThingNotFoundException when thing does not exist")
    void getThingById_throwsThingNotFound() {
        when(thingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> thingService.getThingById(99L))
                .isInstanceOf(ThingNotFoundException.class);
    }

    //Get thing by user
    @Test
    @DisplayName("getThingByUser: returns list of responses for given userId")
    void getThingByUser_returnsMappedList() {
        when(thingRepository.findByUserId(1L)).thenReturn(List.of(thing));
        when(thingMapper.toResponse(thing)).thenReturn(responseDTO);

        List<ThingResponseDTO> results = thingService.getThingByUser(1L);

        assertThat(results).hasSize(1).containsExactly(responseDTO);
    }

    @Test
    @DisplayName("getThingByUser: returns empty list when user has no things")
    void getThingByUser_returnsEmptyListWhenNoThings() {
        when(thingRepository.findByUserId(1L)).thenReturn(List.of());

        List<ThingResponseDTO> results = thingService.getThingByUser(1L);

        assertThat(results).isEmpty();
    }

    //Delete thing
    @Test
    @DisplayName("deleteThing: deletes thing when owner matches")
    void deleteThing_deletesForOwner() {
        when(thingRepository.findById(10L)).thenReturn(Optional.of(thing));

        thingService.deleteThing(10L, owner);

        verify(thingRepository).delete(thing);
    }

    @Test
    @DisplayName("deleteThing: throws ThingNotFoundException when thing does not exist")
    void deleteThing_throwsThingNotFound() {
        when(thingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> thingService.deleteThing(99L, owner))
                .isInstanceOf(ThingNotFoundException.class);
        verify(thingRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteThing: throws ThingOwnershipException when user is not the owner")
    void deleteThing_throwsOwnershipExceptionForNonOwner() {
        when(thingRepository.findById(10L)).thenReturn(Optional.of(thing));

        assertThatThrownBy(() -> thingService.deleteThing(10L, otherUser))
                .isInstanceOf(ThingOwnershipException.class);
        verify(thingRepository, never()).delete(any());
    }
}
