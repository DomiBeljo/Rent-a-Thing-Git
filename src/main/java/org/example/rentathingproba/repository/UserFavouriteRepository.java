package org.example.rentathingproba.repository;

import org.example.rentathingproba.entities.Listing;
import org.example.rentathingproba.entities.User;
import org.example.rentathingproba.entities.UserFavourite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserFavouriteRepository extends JpaRepository<UserFavourite, Long> {

    // getFavourites() sad koristi ListingRepository.findFavouritesByUserId()
    // koji radi JOIN FETCH — ovdje samo trebamo exists i delete

    Optional<UserFavourite> findByUserAndListing(User user, Listing listing);

    boolean existsByUserAndListing(User user, Listing listing);

    // Direktan DELETE bez prethodnog SELECT (stari deleteByUserAndListing
    // je interno radio SELECT pa DELETE — dva querya umjesto jednog)
    @Modifying
    @Query("DELETE FROM UserFavourite uf WHERE uf.user.id = :userId AND uf.listing.id = :listingId")
    void deleteByUserIdAndListingId(@Param("userId") Long userId, @Param("listingId") Long listingId);

    // Exists po ID-jevima (izbjegava učitavanje entiteta)
    @Query("SELECT COUNT(uf) > 0 FROM UserFavourite uf WHERE uf.user.id = :userId AND uf.listing.id = :listingId")
    boolean existsByUserIdAndListingId(@Param("userId") Long userId, @Param("listingId") Long listingId);
}