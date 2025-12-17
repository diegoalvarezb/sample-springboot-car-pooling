package com.cabify.carpooling.service;

import com.cabify.carpooling.model.Car;
import com.cabify.carpooling.repository.CarRepository;
import com.cabify.carpooling.repository.GroupRepository;
import com.cabify.carpooling.repository.JourneyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for managing cars and their availability.
 * Delegates all persistence and search logic to the Repository layer.
 */
@Service
public class CarService {

    private static final Logger log = LoggerFactory.getLogger(CarService.class);

    private final CarRepository carRepository;
    private final GroupRepository groupRepository;
    private final JourneyRepository journeyRepository;

    public CarService(
            CarRepository carRepository,
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
     * Find and reserve the best fitting car for the requested seats.
     * This operation is atomic and thread-safe, delegated to the Repository.
     */
    public Integer findAndReserveCar(int seats) {
        long startTime = System.currentTimeMillis();

        Integer carId = carRepository.findAndReserveCar(seats);

        long duration = System.currentTimeMillis() - startTime;

        if (carId != null) {
            log.debug("Car found and reserved in O(1). requested_seats={}, car_id={}, duration_ms={}",
                    seats, carId, duration);
        } else {
            log.debug("No car found for requested seats. requested_seats={}, duration_ms={}",
                    seats, duration);
        }

        return carId;
    }

    /**
     * Get the number of available seats for a car.
     */
    public int getAvailableSeats(int carId) {
        return carRepository.getAvailableSeats(carId);
    }

    /**
     * Release seats and get the new total available seats atomically.
     */
    public int releaseSeats(int carId, int seats) {
        int totalAvailable = carRepository.releaseSeats(carId, seats);

        log.debug("Seats released atomically. car_id={}, seats_released={}, total_available={}",
                carId, seats, totalAvailable);

        return totalAvailable;
    }

    /**
     * Try to reserve seats from a specific car atomically.
     */
    public boolean tryReserveSeats(int carId, int seats) {
        boolean success = carRepository.tryReserveSeats(carId, seats);

        if (success) {
            log.debug("Seats reserved successfully. car_id={}, seats_reserved={}", carId, seats);
        } else {
            log.debug("Failed to reserve seats (not enough available). car_id={}, seats_requested={}",
                    carId, seats);
        }

        return success;
    }
}
