package com.tripease.seed;

import com.tripease.model.City;
import com.tripease.model.Hotel;
import com.tripease.model.HotelType;
import com.tripease.model.TransportMode;
import com.tripease.model.Trip;
import com.tripease.repository.CityRepository;
import com.tripease.repository.HotelRepository;
import com.tripease.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fills an empty database with DEMO data: 12 Indian cities, daily flights/trains/buses between them
 * and 5 hotels/resorts per city. Distances are computed with the haversine formula and used to derive
 * durations and fares, so the numbers look plausible. Schedules are fictional.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final CityRepository cityRepo;
    private final TripRepository tripRepo;
    private final HotelRepository hotelRepo;

    private record Tpl(String pattern, HotelType type, int stars, int basePrice, int rooms, String amenities, String description) {}

    private static final Map<String, Double> CITY_COST = Map.ofEntries(
            Map.entry("Mumbai", 1.35), Map.entry("Delhi", 1.20), Map.entry("Goa", 1.25), Map.entry("Bengaluru", 1.10),
            Map.entry("Hyderabad", 1.00), Map.entry("Chennai", 0.95), Map.entry("Kolkata", 0.90), Map.entry("Pune", 1.00),
            Map.entry("Jaipur", 1.00), Map.entry("Kochi", 1.00), Map.entry("Ahmedabad", 0.85), Map.entry("Visakhapatnam", 0.85));

    private static final List<Tpl> TEMPLATES = List.of(
            new Tpl("%s Grand Palace", HotelType.HOTEL, 5, 7800, 120,
                    "Free WiFi,Swimming Pool,Spa,Gym,Fine Dining,Airport Shuttle",
                    "A landmark five-star hotel in the city centre with a rooftop pool and 24-hour room service."),
            new Tpl("The %s Residency", HotelType.HOTEL, 4, 4300, 90,
                    "Free WiFi,Restaurant,Gym,Breakfast Included",
                    "Comfortable business hotel close to the main markets and the railway station."),
            new Tpl("%s Budget Inn", HotelType.HOTEL, 3, 1700, 60,
                    "Free WiFi,AC Rooms,24h Front Desk",
                    "Clean, simple rooms at a friendly price. Ideal for short stays and solo travellers."),
            new Tpl("%s Paradise Resort", HotelType.RESORT, 5, 9800, 70,
                    "Swimming Pool,Spa,Free WiFi,Bar,Kids Club,Sunset Views",
                    "A five-star resort with landscaped gardens, a spa and a large pool, made for slow holidays."),
            new Tpl("%s Garden Retreat", HotelType.RESORT, 4, 6200, 50,
                    "Swimming Pool,Free WiFi,Yoga Deck,Restaurant,Nature Trails",
                    "A quiet garden resort with villas, yoga mornings and nature trails at the edge of the city."));

    @Override
    @Transactional
    public void run(String... args) {
        if (cityRepo.count() == 0) seedCities();
        if (tripRepo.count() == 0) seedTrips();
        if (hotelRepo.count() == 0) seedHotels();
    }

    // ------------------------------------------------------------------ cities

    private void seedCities() {
        Object[][] rows = {
                {"Ahmedabad", "AMD", 23.0225, 72.5714}, {"Bengaluru", "BLR", 12.9716, 77.5946},
                {"Chennai", "MAA", 13.0827, 80.2707}, {"Delhi", "DEL", 28.6139, 77.2090},
                {"Goa", "GOI", 15.2993, 74.1240}, {"Hyderabad", "HYD", 17.3850, 78.4867},
                {"Jaipur", "JAI", 26.9124, 75.7873}, {"Kochi", "COK", 9.9312, 76.2673},
                {"Kolkata", "CCU", 22.5726, 88.3639}, {"Mumbai", "BOM", 19.0760, 72.8777},
                {"Pune", "PNQ", 18.5204, 73.8567}, {"Visakhapatnam", "VTZ", 17.6868, 83.2185}};
        List<City> list = new ArrayList<>();
        for (Object[] r : rows) {
            City c = new City();
            c.setName((String) r[0]);
            c.setCode((String) r[1]);
            c.setLatitude((Double) r[2]);
            c.setLongitude((Double) r[3]);
            list.add(c);
        }
        cityRepo.saveAll(list);
        log.info("Seeded {} cities", list.size());
    }

    // ------------------------------------------------------------------ transport

    private void seedTrips() {
        List<City> cities = cityRepo.findAll();
        List<Trip> out = new ArrayList<>();

        String[][] airlines = {{"IndiGo", "6E", "1.00"}, {"Air India", "AI", "1.15"}, {"Akasa Air", "QP", "0.95"}};
        int[][] flightSlots = {{6, 15}, {13, 40}, {20, 5}};
        String[] trainClass = {"3A AC 3-Tier", "SL Sleeper"};
        double[] trainMult = {1.0, 0.45};
        int[] trainSeats = {320, 520};
        int[][] trainDep = {{17, 30}, {21, 45}};
        String[] busOperators = {"Sunrise Travels", "GreenLine Express"};
        String[] busClass = {"AC Sleeper", "Non-AC Seater"};
        double[] busMult = {1.0, 0.55};
        int[] busSeats = {36, 44};
        int[][] busDep = {{20, 30}, {22, 0}};

        for (City a : cities) {
            for (City b : cities) {
                if (a == b) continue;
                double km = distanceKm(a, b);
                int seed = Math.abs((a.getName() + "|" + b.getName()).hashCode());

                // flights: 3 per day
                for (int i = 0; i < 3; i++) {
                    String[] al = airlines[(seed + i) % 3];
                    int minute = (flightSlots[i][1] + (seed % 6) * 5) % 60;
                    int duration = (int) Math.round(km / 13.0 + 40);
                    double price = (1900 + km * 3.6) * Double.parseDouble(al[2]);
                    out.add(trip(TransportMode.FLIGHT, al[0], al[1] + " " + (100 + (seed + i * 37) % 800), a, b,
                            LocalTime.of(flightSlots[i][0], minute), duration, price, al[0].equals("Air India") ? 160 : 180, "Economy"));
                }
                // trains: 2 per day on routes under 1800 km
                if (km < 1800) {
                    for (int i = 0; i < 2; i++) {
                        String name = (i == 0 ? a.getName() + " Superfast" : b.getName() + " Express");
                        int duration = (int) Math.round(km * 1.25 + 20);
                        double price = (350 + km * 1.35) * trainMult[i];
                        out.add(trip(TransportMode.TRAIN, name, String.valueOf(12000 + (seed + i * 11) % 8000), a, b,
                                LocalTime.of(trainDep[i][0], trainDep[i][1]), duration, price, trainSeats[i], trainClass[i]));
                    }
                }
                // buses: 2 per day on routes under 900 km
                if (km < 900) {
                    for (int i = 0; i < 2; i++) {
                        int duration = (int) Math.round(km * 1.56 + 25);
                        double price = (250 + km * 1.7) * busMult[i];
                        out.add(trip(TransportMode.BUS, busOperators[i], "B" + (100 + (seed + i * 13) % 900), a, b,
                                LocalTime.of(busDep[i][0], busDep[i][1]), duration, price, busSeats[i], busClass[i]));
                    }
                }
            }
        }
        tripRepo.saveAll(out);
        log.info("Seeded {} trips", out.size());
    }

    private Trip trip(TransportMode mode, String operator, String code, City a, City b, LocalTime dep,
                      int durationMinutes, double price, int seats, String travelClass) {
        Trip t = new Trip();
        t.setMode(mode);
        t.setOperator(operator);
        t.setCode(code);
        t.setTravelClass(travelClass);
        t.setOrigin(a.getName());
        t.setOriginCode(a.getCode());
        t.setDestination(b.getName());
        t.setDestinationCode(b.getCode());
        t.setDepartureTime(dep);
        t.setDurationMinutes(durationMinutes);
        t.setBasePrice(BigDecimal.valueOf(Math.round(price / 10.0) * 10L));
        t.setTotalSeats(seats);
        return t;
    }

    private static double distanceKm(City a, City b) {
        double r = 6371;
        double dLat = Math.toRadians(b.getLatitude() - a.getLatitude());
        double dLon = Math.toRadians(b.getLongitude() - a.getLongitude());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(a.getLatitude())) * Math.cos(Math.toRadians(b.getLatitude()))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * r * Math.asin(Math.sqrt(h));
    }

    // ------------------------------------------------------------------ stays

    private void seedHotels() {
        List<City> cities = cityRepo.findAll();
        List<Hotel> out = new ArrayList<>();
        for (int ci = 0; ci < cities.size(); ci++) {
            City c = cities.get(ci);
            double cost = CITY_COST.getOrDefault(c.getName(), 1.0);
            for (int ti = 0; ti < TEMPLATES.size(); ti++) {
                Tpl t = TEMPLATES.get(ti);
                Hotel h = new Hotel();
                h.setName(String.format(t.pattern(), c.getName()));
                h.setCity(c.getName());
                h.setType(t.type());
                h.setStars(t.stars());
                h.setRating(Math.round((3.9 + ((ci * 3 + ti * 2) % 9) * 0.1) * 10) / 10.0);
                h.setBasePrice(BigDecimal.valueOf(Math.round(t.basePrice() * cost / 50.0) * 50L));
                h.setRoomsTotal(t.rooms());
                h.setAmenities(t.amenities());
                h.setDescription(t.description());
                out.add(h);
            }
        }
        hotelRepo.saveAll(out);
        log.info("Seeded {} hotels and resorts", out.size());
    }
}
