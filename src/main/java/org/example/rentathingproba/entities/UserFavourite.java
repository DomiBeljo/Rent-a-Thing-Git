package org.example.rentathingproba.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_favourites",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "listing_id"}))
@Getter
@Setter
@NoArgsConstructor
public class UserFavourite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "listing_id", nullable = false)
    private Listing listing;

    public UserFavourite(User user, Listing listing) {
        this.user = user;
        this.listing = listing;
    }
}
