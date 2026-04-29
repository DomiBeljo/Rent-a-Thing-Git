package org.example.rentathingproba.service.application;

import org.example.rentathingproba.dto.ThingDTO;
import org.example.rentathingproba.entities.Thing;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.exceptions.ThingNotFoundException;
import org.example.rentathingproba.exceptions.UnauthorizedException;
import org.example.rentathingproba.repository.ThingRepository;
import org.example.rentathingproba.responses.ThingResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ThingService {
    private static final Logger log = LoggerFactory.getLogger(ThingService.class);

    private final ThingRepository thingRepository;

    public ThingService(ThingRepository thingRepository) {
        this.thingRepository = thingRepository;
    }

    //Create one thing
    public ThingResponseDTO createThing(ThingDTO dto, User owner) {
        log.info("CREATE THING - name='{}', category='{}', owner={}, imageUrls='{}'",
                dto.getName(), dto.getCategory(), owner.getUsername(), dto.getImageUrls());
        Thing thing = new Thing();
        thing.setUser(owner);
        thing.setName(dto.getName());
        thing.setCategory(dto.getCategory());
        thing.setDescription(dto.getDescription());
        thing.setImageUrls(dto.getImageUrls() != null ? dto.getImageUrls() : "");
        try {
            ThingResponseDTO result = toResponseDTO(thingRepository.save(thing));
            log.info("CREATE THING - Saved successfully with id={}", result.getThingId());
            return result;
        } catch (Exception e) {
            log.error("CREATE THING FAILED - {}", e.getMessage(), e);
            throw e;
        }
        //is available true in ListingService
    }

    public ThingResponseDTO updateThing(Long thingId, ThingDTO dto, User requestingUser) {
        Thing thing = thingRepository.findById(thingId)
                .orElseThrow(() -> new ThingNotFoundException(thingId));

        if (!thing.getUser().getId().equals(requestingUser.getId())) {
            throw new UnauthorizedException(" mijenjanje ovih atributa.");
        }

        thing.setName(dto.getName());
        thing.setCategory(dto.getCategory());
        thing.setDescription(dto.getDescription());
        if (dto.getImageUrls() != null) {
            thing.setImageUrls(dto.getImageUrls());
        }
        return toResponseDTO(thingRepository.save(thing));
    }

    public ThingResponseDTO getThingById(Long thingId) {
        return toResponseDTO(thingRepository.findById(thingId)
                .orElseThrow(() -> new ThingNotFoundException(thingId)));
    }

    public List<ThingResponseDTO> getThingByUser(Long userId) {
        return thingRepository.findByUserId(userId)
                .stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    public void deleteThing(Long thingId, User requestingUser) {
        Thing thing = thingRepository.findById(thingId)
                .orElseThrow(() -> new ThingNotFoundException(thingId));

        if (!requestingUser.getId().equals(thing.getUser().getId())) {
            throw new UnauthorizedException(" brisanje ovih atributa.");
        }
        thingRepository.delete(thing);
    }

    //mapper 2
    private ThingResponseDTO toResponseDTO(Thing t) {
        return new ThingResponseDTO(
                t.getId(),
                t.getName(),
                t.getCategory(),
                t.getDescription(),
                t.getImageUrls(),
                t.getCreatedAt(),
                t.getUser().getId(),
                t.getUser().getUsername()
        );
    }
}