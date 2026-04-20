package org.example.rentathingproba.controllers;

import org.example.rentathingproba.dto.ThingDTO;
import org.example.rentathingproba.entities.Thing;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.responses.ThingResponseDTO;
import org.example.rentathingproba.service.infrastrucure.ThingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/things")
public class ThingController {
    private final ThingService thingService;

    public ThingController(ThingService thingService) {
        this.thingService = thingService;
    }

    @PostMapping
    public ResponseEntity<ThingResponseDTO> createThing(@RequestBody ThingDTO dto) {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(thingService.createThing(dto, currentUser));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ThingResponseDTO> updateThing(@PathVariable Long id, @RequestBody ThingDTO dto){
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(thingService.updateThing(id, dto, currentUser));
    }

    @GetMapping("/my")
    public ResponseEntity<List<ThingResponseDTO>> getMyThings() {
        User currentUser = getCurrentUser();
        return ResponseEntity.ok(thingService.getThingByUser(currentUser.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ThingResponseDTO> getThing(@PathVariable Long id){
        return  ResponseEntity.ok(thingService.getThingById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteThing(@PathVariable Long id){
        User currentUser = getCurrentUser();
        thingService.deleteThing(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    private User getCurrentUser(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return  (User) auth.getPrincipal();
    }
}
