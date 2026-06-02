package org.example.rentathingproba.repository;

import org.example.rentathingproba.entities.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {

 // ── Single fetch (getListingById) ──────────────────────────────────────
 // JOIN FETCH sve asocijacije odjednom, nema lazy loading u mapperu
 @Query("""
        SELECT DISTINCT l FROM Listing l
        JOIN FETCH l.things t
   
        LEFT JOIN FETCH l.images
        JOIN FETCH l.user
        WHERE l.id = :id
    """)
 Optional<Listing> findByIdWithDetails(@Param("id") Long id);

 // ── Search po imenu / kategoriji ───────────────────────────────────────
 // Stari query je imao lazy-load na user i images — dodani JOIN FETCH
 @Query("""
        SELECT DISTINCT l FROM Listing l
        JOIN FETCH l.things t
     
        LEFT JOIN FETCH l.images
        JOIN FETCH l.user
        WHERE (LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(t.category) LIKE LOWER(CONCAT('%', :query, '%')))
        AND l.isAvailable = true
    """)
 List<Listing> findByTitle(@Param("query") String query);

 // ── Listinzi usera (profil, my-listings) ──────────────────────────────
 // Staro: findByUserId — nije imalo JOIN FETCH, svaki mapper.toResponse()
 //        je okidao 3 lazy load querya po listingu (things, images, user)
 @Query("""
        SELECT DISTINCT l FROM Listing l
        JOIN FETCH l.things t
     
        LEFT JOIN FETCH l.images
        JOIN FETCH l.user
        WHERE l.user.id = :userId
    """)
 List<Listing> findByUserId(@Param("userId") Long userId);

 // ── Samo dostupni listinzi usera ───────────────────────────────────────
 @Query("""
        SELECT DISTINCT l FROM Listing l
        JOIN FETCH l.things t
    
        LEFT JOIN FETCH l.images
        JOIN FETCH l.user
        WHERE l.user.id = :userId
        AND l.isAvailable = true
    """)
 List<Listing> findByUserIdAndIsAvailableTrue(@Param("userId") Long userId);

 // ── Recommended ────────────────────────────────────────────────────────
 // Staro: native query bez JOIN FETCH → N lazy load querya po rezultatu
 // Novo: JPQL s JOIN FETCH + native ORDER BY RANDOM() trick
 // Subquery za random offset je kompatibilan s PostgreSQL / Supabase
 @Query(value = """
    SELECT * FROM listings
    WHERE is_available = true
    ORDER BY RANDOM()
    LIMIT 3
""", nativeQuery = true)
 List<Listing> findRecommendedNative();

 // JPQL verzija koja odmah fetch-a sve asocijacije za recommended
 // Koristimo entity graph umjesto nativeQuery jer nativeQuery ne podržava JOIN FETCH
 @Query("""
        SELECT DISTINCT l FROM Listing l
        JOIN FETCH l.things t
    
        LEFT JOIN FETCH l.images
        JOIN FETCH l.user
        WHERE l.id IN :ids
    """)
 List<Listing> findByIdsWithDetails(@Param("ids") List<Long> ids);

 // ── Map markers (sve dostupne) ─────────────────────────────────────────
 @Query("""
        SELECT DISTINCT l FROM Listing l
        JOIN FETCH l.things t
   
        LEFT JOIN FETCH l.images
        JOIN FETCH l.user
        WHERE l.isAvailable = true
    """)
 List<Listing> findAllAvailableWithThings();

 // ── Map markers po kategoriji ──────────────────────────────────────────
 @Query("""
        SELECT DISTINCT l FROM Listing l
        JOIN FETCH l.things t
   
        LEFT JOIN FETCH l.images
        JOIN FETCH l.user
        WHERE l.isAvailable = true
        AND LOWER(t.category) = LOWER(:category)
    """)
 List<Listing> findAvailableByCategory(@Param("category") String category);

 // ── Count queries (za UserService.toDTO bez N+1) ───────────────────────
 @Query("SELECT COUNT(l) FROM Listing l WHERE l.user.id = :userId")
 int countByUserId(@Param("userId") Long userId);

 // ── Favourites fetch s JOIN FETCH ──────────────────────────────────────
 @Query("""
        SELECT DISTINCT l FROM Listing l
        JOIN FETCH l.things t
 
        LEFT JOIN FETCH l.images
        JOIN FETCH l.user
        WHERE l.id IN (
            SELECT uf.listing.id FROM UserFavourite uf WHERE uf.user.id = :userId
        )
    """)
 List<Listing> findFavouritesByUserId(@Param("userId") Long userId);

 Page<Listing> findByIsAvailableTrue(Pageable pageable);
}