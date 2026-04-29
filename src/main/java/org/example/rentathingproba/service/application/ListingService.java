package org.example.rentathingproba.service.application;

import org.example.rentathingproba.dto.ListingDTO;
import org.example.rentathingproba.entities.Listing;
import org.example.rentathingproba.entities.Thing;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.repository.ListingRepository;
import org.example.rentathingproba.repository.ThingRepository;
import org.example.rentathingproba.responses.ListingResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ListingService {
    private static final Logger log = LoggerFactory.getLogger(ListingService.class);

    private final ListingRepository listingRepository;
    private final ThingRepository thingRepository;

    public ListingService(ListingRepository listingRepository, ThingRepository thingRepository) {
        this.listingRepository = listingRepository;
        this.thingRepository = thingRepository;
    }

    public ListingResponseDTO createListing(ListingDTO dto, User owner) {
        log.info("CREATE LISTING - thingId={}, owner={}, price={}, location={}, deposit={}",
                dto.getThingId(), owner.getUsername(), dto.getPrice(), dto.getLocation(), dto.getSecurityDeposit());

        Thing thing = thingRepository.findById(dto.getThingId())
                .orElseThrow(() -> {
                    log.error("CREATE LISTING FAILED - Thing not found: id={}", dto.getThingId());
                    return new RuntimeException("Stvar nije pronadena!");
                });

        log.info("CREATE LISTING - Found thing: name={}, owner={}, imageUrls='{}'",
                thing.getName(), thing.getUser().getId(), thing.getImageUrls());

        if(!thing.getUser().getId().equals(owner.getId())){
            log.error("CREATE LISTING FAILED - Ownership mismatch: thing owner={}, requester={}", thing.getUser().getId(), owner.getId());
            throw new RuntimeException("Smijete objavljivati oglase iskljulivo za vlastite stvari!");
        }

        Listing listing = new Listing();
        listing.setThings(thing);
        listing.setUser(owner);
        listing.setPrice(dto.getPrice());
        listing.setSecurityDeposit(dto.getSecurityDeposit());
        listing.setLocation(dto.getLocation());
        listing.setIsAvailable(true);
        String imageUrls = thing.getImageUrls() != null ? thing.getImageUrls() : "";
        listing.setImageUrls(imageUrls);

        log.info("CREATE LISTING - About to save listing with imageUrls='{}'", imageUrls);
        try {
            listing = listingRepository.save(listing);
            log.info("CREATE LISTING - Saved successfully with id={}", listing.getId());
        } catch (Exception e) {
            log.error("CREATE LISTING FAILED - DB save threw exception: {}", e.getMessage(), e);
            throw e;
        }

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
        log.info("SEARCH - query='{}'", query);
        List<Listing> results = listingRepository.findByTitle(query);
        log.info("SEARCH - found {} results for query='{}'", results.size(), query);
        return results.stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    public List<ListingResponseDTO> getRecommended(){
        log.info("RECOMMENDED - fetching recommended listings");
        try {
            List<Listing> results = listingRepository.findRecommended();
            log.info("RECOMMENDED - found {} listings", results.size());
            return results.stream().map(this::toResponseDTO).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("RECOMMENDED FAILED - {}", e.getMessage(), e);
            throw e;
        }
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
                l.getImageUrls() != null ? l.getImageUrls() : "",
                l.getUser().getId(),
                l.getUser().getUsername()
        );
    }
}