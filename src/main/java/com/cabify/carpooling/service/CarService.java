package com.cabify.carpooling.service;

import com.cabify.carpooling.model.Car;
import com.cabify.carpooling.repository.CarRepository;
import com.cabify.carpooling.repository.GroupRepository;
import com.cabify.carpooling.repository.JourneyRepository;
import com.cabify.carpooling.util.MapHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Service for managing cars and their availability.
 */
@Service
public class CarService {
    
    private static final Logger log = LoggerFactory.getLogger(CarService.class);

    private final CarRepository carRepository;
    private final GroupRepository groupRepository;
    private final JourneyRepository journeyRepository;

    public CarService(CarRepository carRepository, 
                     GroupRepository groupRepository,
                     JourneyRepository journeyRepository) {
        this.carRepository = carRepository;
        this.groupRepository = groupRepository;
        this.journeyRepository = journeyRepository;
    }

    /**
     * Retrieve a car by its identifier.
     */
    public Car get(int carId) {
        return carRepository.get(carId);
    }

    /**
     * Reset the application state and load the incoming list of cars.
     */
    public void load(List<Car> cars) {
        long startTime = System.currentTimeMillis();
        int carCount = cars.size();

        carRepository.flush();
        groupRepository.flush();
        journeyRepository.flush();

        carRepository.replace(cars);

        long duration = System.currentTimeMillis() - startTime;

        log.info("Cars loaded - application state reset. cars_count={}, duration_ms={}", carCount, duration);
    }

    /**
     * Locate the best fitting car for the requested seats using binary search.
     */
    public Integer findCar(int seats) {
        long startTime = System.currentTimeMillis();
        LinkedHashMap<Integer, Integer> availability = carRepository.getAvailabilityMap();
        int availableCarsCount = availability.size();

        if (availability.isEmpty()) {
            long duration = System.currentTimeMillis() - startTime;
            log.debug("No car found - availability map is empty. requested_seats={}, duration_ms={}", seats, duration);
            return null;
        }

        // Get the first value (highest available seats)
        Integer maxAvailableSeats = availability.values().iterator().next();
        
        if (maxAvailableSeats < seats) {
            long duration = System.currentTimeMillis() - startTime;
            log.debug("No car found for requested seats. requested_seats={}, max_available_seats={}, available_cars_count={}, duration_ms={}", 
                    seats, maxAvailableSeats, availableCarsCount, duration);
            return null;
        }

        Integer carId = MapHelper.binarySearchOrNext(availability, seats);
        long duration = System.currentTimeMillis() - startTime;

        log.debug("Car found for journey. requested_seats={}, car_id={}, available_seats={}, available_cars_count={}, duration_ms={}", 
                seats, carId, availability.get(carId), availableCarsCount, duration);

        return carId;
    }

    /**
     * Get the number of free seats for a car.
     */
    public int getAvailableSeats(int carId) {
        LinkedHashMap<Integer, Integer> availability = carRepository.getAvailabilityMap();
        return availability.getOrDefault(carId, 0);
    }

    /**
     * Update the free seats counter for a car and keep the map ordered.
     */
    public void updateAvailableSeats(int carId, int increment) {
        LinkedHashMap<Integer, Integer> availability = carRepository.getAvailabilityMap();

        if (!availability.containsKey(carId)) {
            return;
        }

        availability.put(carId, availability.get(carId) + increment);
        availability = MapHelper.reorderMapElement(availability, carId);

        carRepository.updateAvailabilityMap(availability);
    }
}
