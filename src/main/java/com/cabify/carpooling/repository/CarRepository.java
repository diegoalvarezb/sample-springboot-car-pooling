package com.cabify.carpooling.repository;

import com.cabify.carpooling.model.Car;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Repository interface for managing car data.
 */
public interface CarRepository {
    
    /**
     * Get a car by its ID.
     *
     * @param carId The car ID
     * @return The car, or null if not found
     */
    Car get(int carId);

    /**
     * Replace the entire list of cars.
     *
     * @param cars The new list of cars
     */
    void replace(List<Car> cars);

    /**
     * Flush all car data.
     */
    void flush();

    /**
     * Get the availability map (carId -> availableSeats) sorted in descending order by available seats.
     *
     * @return A LinkedHashMap sorted by available seats in descending order
     */
    LinkedHashMap<Integer, Integer> getAvailabilityMap();

    /**
     * Update the availability map.
     *
     * @param availabilityMap The new availability map
     */
    void updateAvailabilityMap(LinkedHashMap<Integer, Integer> availabilityMap);
}
