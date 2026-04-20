package org.example.rentathingproba.service.infrastrucure;

import org.example.rentathingproba.dto.ThingDTO;
import org.example.rentathingproba.entities.Thing;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.repository.ThingRepository;
import org.example.rentathingproba.responses.ThingResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
@Transactional
public class ThingService {
    private final ThingRepository thingRepository;

    public ThingService(ThingRepository thingRepository) {
        this.thingRepository = thingRepository;
    }

    //Kreacija jedne stvari
    public ThingResponseDTO createThing(ThingDTO dto, User owner) {
        Thing thing = new Thing();
        thing.setUser(owner);
        thing.setName(dto.getName());
        thing.setCategory(dto.getCategory());
        thing.setDescription(dto.getDescription());
        return toResponseDTO( thingRepository.save(thing));
        //is available automatski true u ListingServiceu
    }

    public ThingResponseDTO updateThing(Long thingId, ThingDTO dto, User requestingUser) {
        Thing thing = thingRepository.findById(thingId)
                .orElseThrow(() -> new RuntimeException("Stvar nije pronađena!"));

        if (!thing.getUser().getId().equals(requestingUser.getId())) {
            throw new RuntimeException("Ne smijete mijenjati atribute ove stvari?");
        }

        thing.setName(dto.getName());
        thing.setCategory(dto.getCategory());
        thing.setDescription(dto.getDescription());
        return toResponseDTO(thingRepository.save(thing));
    }

    public ThingResponseDTO getThingById(Long thingId) {
        return toResponseDTO(thingRepository.findById(thingId)
                .orElseThrow(() -> new RuntimeException("Stvar nije pronadena.")));
    }

    public List<ThingResponseDTO> getThingByUser(Long userId) {
        return thingRepository.findByUserId(userId)
                .stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    public void deleteThing(Long thingId, User requestingUser) {
        Thing thing = thingRepository.findById(thingId)
                .orElseThrow(() -> new RuntimeException("Stvar nije pronadena."));

        if (!requestingUser.getId().equals(thing.getUser().getId())) {
            throw new RuntimeException("Niste autorizirani za brisanje odabrane stvari.");
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
