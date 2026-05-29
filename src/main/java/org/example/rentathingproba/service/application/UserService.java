package org.example.rentathingproba.service.application;

import org.example.rentathingproba.entities.Listing;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.entities.UserFavourite;
import org.example.rentathingproba.exceptions.ListingNotFoundException;
import org.example.rentathingproba.repository.ListingRepository;
import org.example.rentathingproba.repository.UserFavouriteRepository;
import org.example.rentathingproba.repository.UserRepository;
import org.example.rentathingproba.responses.ListingResponseDTO;
import org.example.rentathingproba.responses.UserResponseDTO;
import org.example.rentathingproba.mapper.ListingMapper;
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

    private UserResponseDTO toDTO(User u) {
        double avg = u.getRatingCount() == 0 ? 0.0
                : Math.round((u.getRatingSum() / u.getRatingCount()) * 10.0) / 10.0;
        int favCount = favouriteRepository.findByUser(u).size();
        int listingCount = listingRepository.findByUserId(u.getId()).size();
        return new UserResponseDTO(u.getId(), u.getDisplayName(), u.getEmail(),
                avg, u.getRatingCount(), favCount, listingCount);
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

    // Favourites

    @Transactional(readOnly = true)
    public List<ListingResponseDTO> getFavourites(User user) {
        return favouriteRepository.findByUser(user).stream()
                .map(fav -> listingMapper.toResponse(fav.getListing()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void addFavourite(User user, Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException(listingId));
        if (!favouriteRepository.existsByUserAndListing(user, listing)) {
            favouriteRepository.save(new UserFavourite(user, listing));
        }
    }

    @Transactional
    public void removeFavourite(User user, Long listingId) {
        Listing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException(listingId));
        favouriteRepository.deleteByUserAndListing(user, listing);
    }

    @Transactional(readOnly = true)
    public boolean isFavourite(User user, Long listingId) {
        return listingRepository.findById(listingId)
                .map(listing -> favouriteRepository.existsByUserAndListing(user, listing))
                .orElse(false);
    }

    //Rating

    @Transactional
    public void rateUser(Long targetUserId, double score) {
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new org.example.rentathingproba.exceptions.UserNotFoundException(String.valueOf(targetUserId)));
        target.setRatingSum(target.getRatingSum() + score);
        target.setRatingCount(target.getRatingCount() + 1);
        userRepository.save(target);
    }
}
