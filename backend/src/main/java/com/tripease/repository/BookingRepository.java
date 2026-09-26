package com.tripease.repository;

import com.tripease.model.Booking;
import com.tripease.model.BookingStatus;
import com.tripease.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserOrderByCreatedAtDesc(User user);

    @Query("select coalesce(sum(b.quantity), 0) from Booking b " +
            "where b.trip.id = :tripId and b.travelDate = :date and b.status = :status")
    Long seatsBooked(@Param("tripId") Long tripId, @Param("date") LocalDate date, @Param("status") BookingStatus status);

    @Query("select coalesce(sum(b.quantity), 0) from Booking b " +
            "where b.hotel.id = :hotelId and b.status = :status " +
            "and b.travelDate < :checkOut and b.checkOutDate > :checkIn")
    Long roomsBooked(@Param("hotelId") Long hotelId, @Param("checkIn") LocalDate checkIn,
                     @Param("checkOut") LocalDate checkOut, @Param("status") BookingStatus status);
}
