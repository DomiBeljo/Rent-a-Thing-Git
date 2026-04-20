package org.example.rentathingproba.service.infrastrucure;

import org.example.rentathingproba.dto.ListingDTO;
import org.example.rentathingproba.entities.Listing;
import org.example.rentathingproba.entities.Thing;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.repository.ListingRepository;
import org.example.rentathingproba.repository.ThingRepository;
import org.example.rentathingproba.responses.ListingResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ListingService {
    private final ListingRepository listingRepository;
    private final ThingRepository thingRepository;

    public ListingService(ListingRepository listingRepository, ThingRepository thingRepository) {
        this.listingRepository = listingRepository;
        this.thingRepository = thingRepository;
    }

    public ListingResponseDTO createListing(ListingDTO dto, User owner) {
        Thing thing = thingRepository.findById(dto.getThingId())
                .orElseThrow(() -> new RuntimeException("Stvar nije pronadena!"));

        if(!thing.getUser().getId().equals(owner.getId())){
            throw new RuntimeException("Smijete objavljivati oglase iskljulivo za vlastite stvari!");
        }

        Listing listing = new Listing();
        listing.setThings(thing);
        listing.setUser(owner);
        listing.setPrice(dto.getPrice());
        listing.setSecurityDeposit(dto.getSecurityDeposit());
        listing.setLocation(dto.getLocation());
        listing.setIsAvailable(true);
        listing.setImageUrls("");
        listing = listingRepository.save(listing);

        return toResponseDTO(listing);
    }

    public ListingResponseDTO updateListing(Long listingId, ListingDTO dto, User requestingUser) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Oglas nije pronaden!"));

        if(!listing.getUser().getId().equals(requestingUser.getId())){
            throw new RuntimeException("Niste autorizirani mijenjati ovaj oglas.");
        }

        listing.setPrice(dto.getPrice());
        listing.setSecurityDeposit(dto.getSecurityDeposit());
        listing.setLocation(dto.getLocation());
        listing =  listingRepository.save(listing);

        return toResponseDTO(listing);
    }

    public void isItAvailable(Long listingId, User requestingUser) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new RuntimeException("Oglas nije pronaden!"));

        if(!listing.getUser().getId().equals(requestingUser.getId())){
            throw new RuntimeException("Niste autorizirani za ovu radnju!");
        }
        listing.setIsAvailable(!listing.getIsAvailable());
        listingRepository.save(listing);
    }

    public List<ListingResponseDTO> searchListing(String query) {
        return listingRepository.findByTitle(query)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<ListingResponseDTO> getRecommended(){
        return listingRepository.findRecommended()
                .stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    public List<ListingResponseDTO> getUserListing(Long userId) {
        return listingRepository.findByUserId(userId)
                .stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    //mapper
    private ListingResponseDTO toResponseDTO(Listing l) {
        return new ListingResponseDTO(
                l.getId(),
                l.getPrice(),
                l.getLocation(),
                l.getIsAvailable(),
                l.getCreatedAt(),
                l.getSecurityDeposit(),
                l.getThings().getId(),
                l.getThings().getName(),
                l.getThings().getCategory(),
                l.getThings().getDescription(),
                l.getThings().getImageUrls(),
                l.getUser().getId(),
                l.getUser().getUsername()
        );
    }
}
