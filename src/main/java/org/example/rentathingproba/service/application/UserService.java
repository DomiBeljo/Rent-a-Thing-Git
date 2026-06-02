package org.example.rentathingproba.service.application;

import org.example.rentathingproba.entities.Listing;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.entities.UserFavourite;
import org.example.rentathingproba.exceptions.ListingNotFoundException;
import org.example.rentathingproba.mapper.ListingMapper;
import org.example.rentathingproba.repository.ListingRepository;
import org.example.rentathingproba.repository.UserFavouriteRepository;
import org.example.rentathingproba.repository.UserRepository;
import org.example.rentathingproba.responses.ListingResponseDTO;
import org.example.rentathingproba.responses.UserResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserFavouriteRepository favouriteRepository;
    private final ListingRepository listingRepository;
    private final ListingMapper listingMapper;

    public UserService(UserRepository userRepository,
                       UserFavouriteRepository favouriteRepository,
                       ListingRepository listingRepository,
                       ListingMapper listingMapper) {
        this.userRepository = userRepository;
        this.favouriteRepository = favouriteRepository;
        this.listingRepository = listingRepository;
        this.listingMapper = listingMapper;
    }

    // ── Profile ────────────────────────────────────────────────────────────

    // Staro toDTO():
    //   favouriteRepository.findByUser(u).size()        → SELECT sve favourite redove
    //   listingRepository.findByUserId(u.getId()).size() → SELECT sve listinge
    //   = 2 querya po useru, za findAllUsers() s N usera = 2N+1 querya
    //
    // Novo: 2 COUNT querya po useru — ne materijalizira liste
    private UserResponseDTO toDTO(User u) {
        double avg = u.getRatingCount() == 0 ? 0.0
                : Math.round((u.getRatingSum() / u.getRatingCount()) * 10.0) / 10.0;

        int favCount     = userRepository.countFavouritesByUserId(u.getId());
        int listingCount = userRepository.countListingsByUserId(u.getId());

        return new UserResponseDTO(
                u.getId(), u.getDisplayName(), u.getEmail(),
                avg, u.getRatingCount(), favCount, listingCount
        );
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAllUsers() {
        return StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponseDTO getProfile(User user) {
        return toDTO(user);
    }

    // ── Favourites ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ListingResponseDTO> getFavourites(User user) {
        // Staro: findByUser(user) → lazy load svakog fav.getListing() → N querya
        // Novo: jedan JOIN FETCH query koji dohvaća listinge s njihovim asocijacijama
        return listingRepository.findFavouritesByUserId(user.getId()).stream()
                .map(listingMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void addFavourite(User user, Long listingId) {
        // Provjera exists-a po ID-jevima — ne treba učitavati Listing entitet
        if (!favouriteRepository.existsByUserIdAndListingId(user.getId(), listingId)) {
            Listing listing = listingRepository.findById(listingId)
                    .orElseThrow(() -> new ListingNotFoundException(listingId));
            favouriteRepository.save(new UserFavourite(user, listing));
        }
    }

    @Transactional
    public void removeFavourite(User user, Long listingId) {
        // Staro: findById(listingId) + deleteByUserAndListing = 2 querya (SELECT + DELETE)
        // Novo: direktan DELETE u jednom queryu
        favouriteRepository.deleteByUserIdAndListingId(user.getId(), listingId);
    }

    @Transactional(readOnly = true)
    public boolean isFavourite(User user, Long listingId) {
        // Staro: findById(listingId) + existsByUserAndListing = 2 querya
        // Novo: jedan COUNT query
        return favouriteRepository.existsByUserIdAndListingId(user.getId(), listingId);
    }

    // ── Rating ─────────────────────────────────────────────────────────────

    @Transactional
    public void rateUser(Long targetUserId, double score) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new org.example.rentathingproba.exceptions.UserNotFoundException(String.valueOf(targetUserId)));
        target.setRatingSum(target.getRatingSum() + score);
        target.setRatingCount(target.getRatingCount() + 1);
        userRepository.save(target);
    }
}