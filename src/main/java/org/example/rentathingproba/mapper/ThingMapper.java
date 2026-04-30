package org.example.rentathingproba.mapper;

import org.example.rentathingproba.dto.ThingDTO;
import org.example.rentathingproba.entities.Thing;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.responses.ThingResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class ThingMapper {
    public Thing toEntity(ThingDTO dto, User owner) {
        Thing thing = new Thing();
        thing.setUser(owner);
        thing.setName(dto.getName());
        thing.setCategory(dto.getCategory());
        thing.setDescription(dto.getDescription());
        thing.setImageUrls(dto.getImageUrls() != null ? dto.getImageUrls() : "");
        return thing;
    }

    public void updateEntity(Thing thing, ThingDTO dto) {
        thing.setName(dto.getName());
        thing.setCategory(dto.getCategory());
        thing.setDescription(dto.getDescription());
        if (dto.getImageUrls() != null) {
            thing.setImageUrls(dto.getImageUrls());
        }
    }

    public ThingResponseDTO toResponse(Thing t) {
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
