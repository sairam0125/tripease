package com.tripease.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;

/**
 * One recurring daily service (a flight, train or bus). The price and seat
 * availability for a specific travel date are calculated on demand.
 */
@Entity
@Table(name = "trips", indexes = @Index(name = "idx_trip_route", columnList = "transport_mode,origin,destination"))
@Getter
@Setter
@NoArgsConstructor
public class Trip {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_mode", nullable = false, length = 10)
    private TransportMode mode;

    @Column(nullable = false, length = 60)
    private String operator;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(length = 40)
    private String travelClass;

    @Column(nullable = false, length = 60)
    private String origin;

    @Column(nullable = false, length = 5)
    private String originCode;

    @Column(nullable = false, length = 60)
    private String destination;

    @Column(nullable = false, length = 5)
    private String destinationCode;

    @Column(nullable = false)
    private LocalTime departureTime;

    private int durationMinutes;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    private int totalSeats;
}
