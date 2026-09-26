package com.tripease.service;

import com.tripease.model.TransportMode;
import com.tripease.model.Trip;
import com.tripease.util.AppClock;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PricingServiceTest {

    private final PricingService pricing = new PricingService();

    private Trip trip(TransportMode mode) {
        Trip t = new Trip();
        t.setId(7L);
        t.setMode(mode);
        t.setBasePrice(BigDecimal.valueOf(4000));
        t.setTotalSeats(180);
        return t;
    }

    @Test
    void samePriceForSameTripAndDate() {
        LocalDate date = AppClock.today().plusDays(12);
        assertEquals(pricing.tripPrice(trip(TransportMode.FLIGHT), date), pricing.tripPrice(trip(TransportMode.FLIGHT), date));
    }

    @Test
    void lastMinuteFlightsCostMoreThanAdvanceBookings() {
        BigDecimal nearPrice = pricing.tripPrice(trip(TransportMode.FLIGHT), AppClock.today().plusDays(1));
        BigDecimal farPrice = pricing.tripPrice(trip(TransportMode.FLIGHT), AppClock.today().plusDays(45));
        assertTrue(nearPrice.compareTo(farPrice) > 0, "near=" + nearPrice + " far=" + farPrice);
    }

    @Test
    void priceStaysWithinSaneBounds() {
        Trip flight = trip(TransportMode.FLIGHT);
        for (int i = 0; i < 90; i++) {
            double p = pricing.tripPrice(flight, AppClock.today().plusDays(i)).doubleValue();
            assertTrue(p >= 4000 * 0.8 && p <= 4000 * 1.8, "price out of range: " + p);
        }
    }

    @Test
    void seatsSoldBaselineNeverExceedsCapacity() {
        Trip flight = trip(TransportMode.FLIGHT);
        for (int i = 0; i < 60; i++) {
            int sold = pricing.baselineSold(flight, AppClock.today().plusDays(i));
            assertTrue(sold >= 0 && sold < flight.getTotalSeats());
        }
    }
}
