package org.example.rentathingproba.repository;

import org.example.rentathingproba.entities.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByListingIdAndBuyerId(Long listingId, Long buyerId);

    @Query("""
        SELECT DISTINCT c FROM Conversation c
        JOIN FETCH c.listing l
        JOIN FETCH l.things t
        JOIN FETCH c.buyer b
        JOIN FETCH l.user owner
        WHERE b.id = :userId OR owner.id = :userId
        ORDER BY c.createdAt DESC
    """)
    List<Conversation> findAllForUser(@Param("userId") Long userId);
}