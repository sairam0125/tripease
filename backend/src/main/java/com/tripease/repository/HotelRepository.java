package com.tripease.repository;

import com.tripease.model.Hotel;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HotelRepository extends JpaRepository<Hotel, Long> {

    List<Hotel> findByCityIgnoreCase(String city);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select h from Hotel h where h.id = :id")
    Optional<Hotel> lockById(@Param("id") Long id);
}
