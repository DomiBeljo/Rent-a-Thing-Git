package org.example.rentathingproba.repository;

import org.example.rentathingproba.entities.Listing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ListingRepository extends JpaRepository<Listing,Long> {
    @Query("SELECT l FROM Listing l JOIN l.things t WHERE LOWER(t.name) LIKE LOWER(CONCAT ('%', :query, '%')) AND l.isAvailable = true")
    List<Listing> findByTitle(String title);

    List<Listing> findByUserId(Long userId);

    List<Listing> findByUserIdAndIsAvailableTrue(Long userId);

    //Treba mi par random querya za kraj
    @Query(value = "SELECT * FROM listings WHERE is_available = true ORDER BY RANDOM() LIMIT 3", nativeQuery = true)
    List<Listing> findRecommended();
}
