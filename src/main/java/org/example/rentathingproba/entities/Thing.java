package org.example.rentathingproba.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "things")
@Getter
@Setter
public class Thing {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String description;

    @Column(name = "image_urls", columnDefinition = "TEXT")
    private String imageUrls;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void  onCreate()
    {
        this.createdAt = LocalDateTime.now();
    }

    @Transient
    public String getImageUrl(){
        if(imageUrls == null || imageUrls.isBlank()){
            return null;
        }
        return imageUrls.split(",")[0];
    }
}

//INSERT INTO your_table (column_name) VALUES ('non-null value');
//
//Make sure that all columns with NOT NULL constraints are provided with valid, non-null values.