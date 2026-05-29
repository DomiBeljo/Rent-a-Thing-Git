package org.example.rentathingproba.service.application;

import org.example.rentathingproba.dto.ListingDTO;
import org.example.rentathingproba.entities.Listing;
import org.example.rentathingproba.entities.Thing;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.exceptions.ListingNotFoundException;
import org.example.rentathingproba.exceptions.ListingOwnershipException;
import org.example.rentathingproba.exceptions.ThingNotFoundException;
import org.example.rentathingproba.exceptions.ThingOwnershipException;
import org.example.rentathingproba.mapper.ListingMapper;
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
    private final ListingMapper listingMapper;

    public ListingService(ListingRepository listingRepository, ThingRepository thingRepository, ListingMapper listingmapper) {
        this.listingRepository = listingRepository;
        this.thingRepository = thingRepository;
        this.listingMapper = listingmapper;
    }

    public ListingResponseDTO createListing(ListingDTO dto, User owner) {
        log.info("Creating listing - thingId={}, owner={}, price={}, location={}, deposit={}",
                dto.getThingId(), owner.getUsername(), dto.getPrice(), dto.getLocation(), dto.getSecurityDeposit());

        Thing thing = thingRepository.findById(dto.getThingId())
                .orElseThrow(() -> new ThingNotFoundException(dto.getThingId()));

        if(!thing.getUser().getId().equals(owner.getId())){
            log.warn("Ownership violation on createListing: thingId={}, requester={}", dto.getThingId(), owner.getId());
            throw new ThingOwnershipException();
        }

        Listing listing = listingMapper.toEntity(dto, owner, thing);
        Listing saved = listingRepository.save(listing);

        log.info("Listing saved succesfully: id='{}'", saved.getId());
        return listingMapper.toResponse(saved);
    }

    public ListingResponseDTO updateListing(Long listingId, ListingDTO dto, User requestingUser) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException(listingId));

        if(!listing.getUser().getId().equals(requestingUser.getId())){
            log.warn("Ownership violation on updateListing: listingId={}, requester={}", listingId, requestingUser.getId());
            throw new ListingOwnershipException();
        }

        listingMapper.updateEntity(listing, dto);
        return listingMapper.toResponse(listingRepository.save(listing));
    }

    public void isItAvailable(Long listingId, User requestingUser) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException(listingId));

        if(!listing.getUser().getId().equals(requestingUser.getId())){
            log.warn("Ownership violation on isItAvailable: listingId={}, requester={}", listingId,  requestingUser.getId());
            throw new ListingOwnershipException();
        }

        listing.setIsAvailable(!listing.getIsAvailable());
        listingRepository.save(listing);
        log.info("Listing availability toggled: id={}, isAvailable={}", listing.getId(),  listing.getIsAvailable());
    }

    @Transactional(readOnly = true)
    public List<ListingResponseDTO> searchListing(String query) {
        log.info("Searching listings: query='{}'", query);
        List<Listing> results = listingRepository.findByTitle(query);
        log.info("Search returned: {} results for query='{}'", results.size(), query);
        return results.stream().map(listingMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ListingResponseDTO> getRecommended(){
        log.info("Fetching recommended listings");
        List<Listing> results = listingRepository.findRecommended();
        log.info("Recommended listings fetched: count={}", results.size());
        return results.stream().map(listingMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ListingResponseDTO> getUserListing(Long userId) {
        return listingRepository.findByUserId(userId).stream()
                .map(listingMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ListingResponseDTO getListingById(Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException(listingId));
        return listingMapper.toResponse(listing);
    }

}