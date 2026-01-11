package com.cabify.carpooling.repository;

import com.cabify.carpooling.model.Car;
import java.util.List;

/**
 * Repository interface for managing car data.
 */
public interface CarRepository {

    /**
     * Get a car by its ID.
     */
    Car get(int carId);

    /**
     * Replace the entire list of cars.
     */
    void replaceAll(List<Car> cars);

    /**
     * Flush all car data.
     */
    void flush();

    /**
     * Find the best car that can fit the requested seats and reserve it atomically.
     */
    Integer findAndReserveCar(int seats);

    /**
     * Get the number of available seats for a specific car.
     */
    Integer getAvailableSeats(int carId);

    /**
     * Release seats and get the new total available seats atomically.
     */
    Integer releaseSeats(int carId, int seats);

    /**
     * Try to reserve seats from a specific car atomically.
     */
    boolean tryReserveSeats(int carId, int seats);
}
