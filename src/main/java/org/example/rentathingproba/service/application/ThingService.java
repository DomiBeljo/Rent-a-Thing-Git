package org.example.rentathingproba.service.application;

import org.example.rentathingproba.dto.ThingDTO;
import org.example.rentathingproba.entities.Thing;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.exceptions.ThingNotFoundException;
import org.example.rentathingproba.exceptions.ThingOwnershipException;
import org.example.rentathingproba.exceptions.UnauthorizedException;
import org.example.rentathingproba.mapper.ThingMapper;
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
    private final ThingMapper thingMapper;

    public ThingService(ThingRepository thingRepository, ThingMapper thingMapper) {
        this.thingRepository = thingRepository;
        this.thingMapper = thingMapper;
    }

    //Create one thing
    public ThingResponseDTO createThing(ThingDTO dto, User owner) {
        log.info("Creating thing: name='{}', category='{}', owner='{}'", dto.getName(), dto.getCategory(), owner.getUsername());

        Thing thing = thingMapper.toEntity(dto, owner);
        Thing saved = thingRepository.save(thing);

        log.info("Thing created successfully: id='{}'", saved.getId());
        return thingMapper.toResponse(saved);
    }

    public ThingResponseDTO updateThing(Long thingId, ThingDTO dto, User requestingUser) {
        Thing thing = thingRepository.findById(thingId)
                .orElseThrow(() -> new ThingNotFoundException(thingId));

        if (!thing.getUser().getId().equals(requestingUser.getId())) {
            log.warn("Ownership violation on updateThing: thingId={}, requestingUser={}", thingId, requestingUser.getId());
            throw new ThingOwnershipException();
        }

        thingMapper.updateEntity(thing, dto);
        return thingMapper.toResponse(thingRepository.save(thing));
    }

    @Transactional(readOnly = true)
    public ThingResponseDTO getThingById(Long thingId) {
        return thingMapper.toResponse(
                thingRepository.findById(thingId)
                        .orElseThrow(() -> new ThingNotFoundException(thingId))
        );
    }

    @Transactional(readOnly = true)
    public List<ThingResponseDTO> getThingByUser(Long userId) {
        return thingRepository.findByUserId(userId).stream()
                .map(thingMapper::toResponse)
                .collect(Collectors.toList());
    }

    public void deleteThing(Long thingId, User requestingUser) {
        Thing thing = thingRepository.findById(thingId)
                .orElseThrow(() -> new ThingNotFoundException(thingId));

        if (!requestingUser.getId().equals(thing.getUser().getId())) {
            log.warn("Ownership violation on delteThing: thingId={}, requestingUser={}", thingId, requestingUser.getId());
            throw new ThingOwnershipException();
        }
        thingRepository.delete(thing);
        log.info("Thing deleted successfully: id='{}'", thingId);
    }

}