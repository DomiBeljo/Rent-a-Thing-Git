package org.example.rentathingproba.repository;

import org.example.rentathingproba.entities.User;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByVerificationCode(String verificationCode);

    // ── Count queries za UserService.toDTO() — eliminiraju N+1 ───────────
    // Staro: favouriteRepository.findByUser(u).size() = SELECT sve favourite redove
    // Novo:  jedan COUNT query, nema materijalzacije liste
    @Query("SELECT COUNT(uf) FROM UserFavourite uf WHERE uf.user.id = :userId")
    int countFavouritesByUserId(@Param("userId") Long userId);

    // Staro: listingRepository.findByUserId(u.getId()).size() = SELECT sve listinge
    // Novo:  jedan COUNT query
    @Query("SELECT COUNT(l) FROM Listing l WHERE l.user.id = :userId")
    int countListingsByUserId(@Param("userId") Long userId);
}