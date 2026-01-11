package com.cabify.carpooling.repository.inmemory;

import com.cabify.carpooling.model.Car;
import com.cabify.carpooling.repository.CarRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of CarRepository.
 */
@Repository
public class InMemoryCarRepository implements CarRepository {

    private static final int MAX_SEATS = 6;

    // Cars indexed by their ID - thread-safe
    private final Map<Integer, Car> cars = new ConcurrentHashMap<>();

    // Buckets indexed by number of available seats (0-6)
    // Each bucket contains a LinkedHashSet of car IDs with that many available
    // seats
    // LinkedHashSet maintains insertion order (FIFO) for fair allocation
    // Synchronized to ensure thread safety
    private final List<Set<Integer>> seatBuckets = new ArrayList<>();

    public InMemoryCarRepository() {
        for (int i = 0; i <= MAX_SEATS; i++) {
            seatBuckets.add(Collections.synchronizedSet(new LinkedHashSet<>()));
        }
    }

    @Override
    public Car get(int carId) {
        Car car = cars.get(carId);
        if (car == null) {
            return null;
        }

        return new Car(car.getId(), car.getSeats());
    }

    @Override
    public synchronized void replaceAll(List<Car> newCars) {
        cars.clear();
        clearBuckets();

        for (Car car : newCars) {
            Car newCar = new Car(car.getId(), car.getSeats());
            cars.put(newCar.getId(), newCar);

            int availableSeats = newCar.getAvailableSeats();

            if (availableSeats >= 0 && availableSeats <= MAX_SEATS) {
                seatBuckets.get(availableSeats).add(newCar.getId());
            }
        }
    }

    @Override
    public synchronized void flush() {
        cars.clear();
        clearBuckets();
    }

    /**
     * Clear all seat buckets.
     */
    private void clearBuckets() {
        for (Set<Integer> bucket : seatBuckets) {
            bucket.clear();
        }
    }

    /**
     * Adjust the available seats for a car and update its bucket position.
     */
    private int adjustAvailableSeats(int carId, int deltaSeats) {
        Car car = cars.get(carId);
        if (car == null) {
            return 0;
        }

        int currentSeats = car.getAvailableSeats();
        int newSeats = Math.max(0, Math.min(car.getSeats(), currentSeats + deltaSeats));


        moveBetweenBuckets(carId, currentSeats, newSeats);
        car.setAvailableSeats(newSeats);

        return newSeats;
    }

    @Override
    public synchronized Integer findAndReserveCar(int seats) {
        Integer carId = findCarInBuckets(seats);

        if (carId != null) {
            adjustAvailableSeats(carId, -seats);
        }

        return carId;
    }

    @Override
    public Integer getAvailableSeats(int carId) {
        Car car = cars.get(carId);

        return car != null ? car.getAvailableSeats() : 0;
    }

    @Override
    public synchronized Integer releaseSeats(int carId, int seats) {
        return adjustAvailableSeats(carId, seats);
    }

    @Override
    public synchronized boolean tryReserveSeats(int carId, int seats) {
        Car car = cars.get(carId);
        if (car != null && car.getAvailableSeats() >= seats) {
            adjustAvailableSeats(carId, -seats);
            return true;
        }

        return false;
    }

    /**
     * Find a car in buckets with enough available seats.
     * Searches from the exact seat count upwards to find the best fit.
     */
    private Integer findCarInBuckets(int seats) {
        for (int i = seats; i <= MAX_SEATS; i++) {
            Set<Integer> bucket = seatBuckets.get(i);

            synchronized (bucket) {
                if (!bucket.isEmpty()) {
                    // Return the first car (FIFO - fair allocation)
                    return bucket.iterator().next();
                }
            }
        }

        return null;
    }

    /**
     * Move a car from one bucket to another when its available seats change.
     */
    private void moveBetweenBuckets(int carId, int fromSeats, int toSeats) {
        // Remove from old bucket
        if (fromSeats >= 0 && fromSeats <= MAX_SEATS) {
            Set<Integer> fromBucket = seatBuckets.get(fromSeats);

            synchronized (fromBucket) {
                fromBucket.remove(carId);
            }
        }

        // Add to new bucket
        if (toSeats >= 0 && toSeats <= MAX_SEATS) {
            Set<Integer> toBucket = seatBuckets.get(toSeats);

            synchronized (toBucket) {
                toBucket.add(carId);
            }
        }
    }
}
