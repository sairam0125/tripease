package com.tripease.repository;

import com.tripease.model.Trip;
import com.tripease.model.TransportMode;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByModeAndOriginIgnoreCaseAndDestinationIgnoreCase(TransportMode mode, String origin, String destination);

    List<Trip> findByOriginIgnoreCase(String origin);

    /** Row lock used while booking so two users cannot take the last seat at the same time. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Trip t where t.id = :id")
    Optional<Trip> lockById(@Param("id") Long id);
}
