package org.example.rentathingproba.service.application;

import org.example.rentathingproba.dto.ListingDTO;
import org.example.rentathingproba.email.central.ListingAction;
import org.example.rentathingproba.email.central.ListingEventPublisher;
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
import org.example.rentathingproba.responses.MapMarkerDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

// PROMJENA: maknuto @Transactional s razine klase.
// Read metode imaju @Transactional(readOnly = true) — Hibernate ne prati
// promjene entiteta (dirty checking OFF), connection se ranije vraća u pool.
// Write metode imaju @Transactional — eksplicitno i jasno.
@Service
public class ListingService {

    private static final Logger log = LoggerFactory.getLogger(ListingService.class);

    private final ListingRepository listingRepository;
    private final ThingRepository thingRepository;
    private final ListingMapper listingMapper;
    private final ListingEventPublisher listingEventPublisher;

    public ListingService(ListingRepository listingRepository,
                          ThingRepository thingRepository,
                          ListingMapper listingMapper,
                          ListingEventPublisher listingEventPublisher) {
        this.listingRepository = listingRepository;
        this.thingRepository = thingRepository;
        this.listingMapper = listingMapper;
        this.listingEventPublisher = listingEventPublisher;
    }

    // ── Write operacije ────────────────────────────────────────────────────

    @Transactional
    public ListingResponseDTO createListing(ListingDTO dto, User owner) {
        log.info("Creating listing - thingId={}, owner={}, price={}, location={}, deposit={}",
                dto.getThingId(), owner.getUsername(), dto.getPrice(), dto.getLocation(), dto.getSecurityDeposit());

        Thing thing = thingRepository.findByIdWithImages(dto.getThingId())
                .orElseThrow(() -> new ThingNotFoundException(dto.getThingId()));

        if (!thing.getUser().getId().equals(owner.getId())) {
            log.warn("Ownership violation on createListing: thingId={}, requester={}", dto.getThingId(), owner.getId());
            throw new ThingOwnershipException();
        }

        Listing listing = listingMapper.toEntity(dto, owner, thing);
        Listing saved = listingRepository.save(listing);

        log.info("Listing saved successfully: id='{}'", saved.getId());
        listingEventPublisher.publish(this, saved.getId(), owner.getEmail(), owner.getUsername(), ListingAction.CREATE);

        return listingMapper.toResponse(saved);
    }

    @Transactional
    public ListingResponseDTO updateListing(Long listingId, ListingDTO dto, User requestingUser) {
        // findByIdWithDetails fetch-a sve što mapper treba — nema lazy load
        Listing listing = listingRepository.findByIdWithDetails(listingId)
                .orElseThrow(() -> new ListingNotFoundException(listingId));

        if (!listing.getUser().getId().equals(requestingUser.getId())) {
            log.warn("Ownership violation on updateListing: listingId={}, requester={}", listingId, requestingUser.getId());
            throw new ListingOwnershipException();
        }

        listingMapper.updateEntity(listing, dto);
        Listing saved = listingRepository.save(listing);

        listingEventPublisher.publish(this, saved.getId(), requestingUser.getEmail(), requestingUser.getUsername(), ListingAction.UPDATE);

        return listingMapper.toResponse(saved);
    }

    @Transactional
    public void deleteListing(Long listingId, User requestingUser) {
        Listing listing = listingRepository.findByIdWithDetails(listingId)
                .orElseThrow(() -> new ListingNotFoundException(listingId));

        if (!listing.getUser().getId().equals(requestingUser.getId())) {
            log.warn("Ownership violation on deleteListing: listingId={}, requester={}", listingId, requestingUser.getId());
            throw new ListingOwnershipException();
        }

        listingRepository.delete(listing);
        log.info("Listing deleted successfully: id='{}'", listingId);
        listingEventPublisher.publish(this, listingId, requestingUser.getEmail(), requestingUser.getUsername(), ListingAction.DELETE);
    }

    @Transactional
    public void isItAvailable(Long listingId, User requestingUser) {
        // Za toggle treba samo id, user i isAvailable — findById je ok ovdje
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException(listingId));

        if (!listing.getUser().getId().equals(requestingUser.getId())) {
            log.warn("Ownership violation on isItAvailable: listingId={}, requester={}", listingId, requestingUser.getId());
            throw new ListingOwnershipException();
        }

        listing.setIsAvailable(!listing.getIsAvailable());
        listingRepository.save(listing);
        log.info("Listing availability toggled: id={}, isAvailable={}", listing.getId(), listing.getIsAvailable());
    }

    // ── Read operacije ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ListingResponseDTO getListingById(Long listingId) {
        // findByIdWithDetails umjesto findById — mapper ne okida lazy load
        return listingMapper.toResponse(
                listingRepository.findByIdWithDetails(listingId)
                        .orElseThrow(() -> new ListingNotFoundException(listingId))
        );
    }

    @Transactional(readOnly = true)
    public List<ListingResponseDTO> searchListing(String query) {
        log.info("Searching listings: query='{}'", query);
        List<Listing> results = listingRepository.findByTitle(query);
        log.info("Search returned: {} results for query='{}'", results.size(), query);
        return results.stream().map(listingMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ListingResponseDTO> getRecommended() {
        log.info("Fetching recommended listings");

        // Staro: findRecommended() → native query bez JOIN FETCH →
        //        svaki listingMapper.toResponse() radio 3+ lazy load querya
        //
        // Novo: 2 querya ukupno:
        //   1) native RANDOM() → dohvati 3 ID-ja
        //   2) findByIdsWithDetails() → JOIN FETCH sve u jednom queryu
        List<Listing> randomListings = listingRepository.findRecommendedNative();
        if (randomListings.isEmpty()) {
            return List.of();
        }
        List<Long> ids = randomListings.stream().map(Listing::getId).collect(Collectors.toList());
        List<Listing> withDetails = listingRepository.findByIdsWithDetails(ids);

        log.info("Recommended listings fetched: count={}", withDetails.size());
        return withDetails.stream().map(listingMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ListingResponseDTO> getUserListing(Long userId) {
        // findByUserId sada ima JOIN FETCH — nema N+1 lazy load
        return listingRepository.findByUserId(userId).stream()
                .map(listingMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MapMarkerDTO> getMapMarkers(String category, User currentUser) {
        List<Listing> listings = (category != null && !category.isBlank())
                ? listingRepository.findAvailableByCategory(category)
                : listingRepository.findAllAvailableWithThings();

        return listings.stream()
                .filter(l -> currentUser == null || !l.getUser().getId().equals(currentUser.getId()))
                .map(this::toMapMarker)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ListingResponseDTO> getAllAvailableListingDTOs() {
        return listingRepository.findAllAvailableWithThings().stream()
                .map(listingMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ListingResponseDTO> getListings(int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        return listingRepository.findAll(pageable)
                .map(listingMapper::toResponse);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private MapMarkerDTO toMapMarker(Listing listing) {
        String thumbnail = null;

        if (listing.getImages() != null && !listing.getImages().isEmpty()) {
            thumbnail = listing.getImages().get(0).getUrl();
        } else if (listing.getThings() != null
                && listing.getThings().getImages() != null
                && !listing.getThings().getImages().isEmpty()) {
            thumbnail = listing.getThings().getImages().get(0).getUrl();
        }

        User owner = listing.getUser();
        double avgRating = owner.getRatingCount() == 0 ? 0.0
                : Math.round((owner.getRatingSum() / owner.getRatingCount()) * 10.0) / 10.0;

        return new MapMarkerDTO(
                listing.getId(),
                listing.getLocation(),
                listing.getThings().getName(),
                listing.getThings().getCategory(),
                listing.getPrice(),
                thumbnail,
                listing.getIsAvailable(),
                owner.getId(),
                owner.getDisplayName(),
                avgRating,
                listing.getLatitude(),
                listing.getLongitude()
        );
    }
}