package com.tripease.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "hotels", indexes = @Index(name = "idx_hotel_city", columnList = "city"))
@Getter
@Setter
@NoArgsConstructor
public class Hotel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 60)
    private String city;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private HotelType type;

    private int stars;
    private double rating;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    private int roomsTotal;

    @Column(length = 500)
    private String amenities;

    @Column(length = 1000)
    private String description;
}
