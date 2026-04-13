package org.example.rentathingproba.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Entity
@Table(name = "listings")
@Getter
@Setter
public class Listing {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "things_id", nullable = false)
    private Thing things;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "daily_rent_price", nullable = false)
    private BigDecimal dailyRentPrice;

    @Column(nullable = false)
    private String location;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void  onCreate()
    {
        this.createdAt = LocalDateTime.now();
    }

    @Column(name = "image_urls", nullable = false)
    private String imageUrls;

    @Transient
    public String getImageUrl() {
        if (imageUrls == null || imageUrls.isBlank()) {
            return null;
        }
        return imageUrls.split(",")[0];
    }
}

//Jedan tool moze imat vise listinga