package org.example.rentathingproba.repository;

import org.example.rentathingproba.entities.Thing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThingRepository extends JpaRepository<Thing, Long> {

    // findByIdWithImages — koristi ListingMapper.copyImagesFromThing() koji
    // pristupa thing.getImages(). Bez JOIN FETCH okida lazy load.
    @Query("""
        SELECT t FROM Thing t
        LEFT JOIN FETCH t.images
        JOIN FETCH t.user
        WHERE t.id = :id
    """)
    Optional<Thing> findByIdWithImages(@Param("id") Long id);

    // findByUserId s JOIN FETCH images i user — ThingMapper.toResponse()
    // pristupa t.getImages() i t.getUser()
    @Query("""
        SELECT t FROM Thing t
        LEFT JOIN FETCH t.images
        JOIN FETCH t.user
        WHERE t.user.id = :userId
    """)
    List<Thing> findByUserId(@Param("userId") Long userId);
}