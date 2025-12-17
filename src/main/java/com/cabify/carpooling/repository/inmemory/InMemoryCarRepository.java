package com.cabify.carpooling.repository.inmemory;

import com.cabify.carpooling.model.Car;
import com.cabify.carpooling.repository.CarRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * In-memory implementation of CarRepository.
 *
 * Thread-safe using ReadWriteLock for compound operations.
 *
 * Uses bucket-based indexing by available seats to achieve O(1) search
 * performance.
 */
@Repository
public class InMemoryCarRepository implements CarRepository {

    private static final int MAX_SEATS = 6;

    // Cars indexed by their ID
    private final Map<Integer, Car> cars = new ConcurrentHashMap<>();

    // Buckets indexed by number of available seats (0-6)
    // Each bucket contains a LinkedHashSet of car IDs with that many available
    // seats
    // LinkedHashSet maintains insertion order (FIFO) for fair allocation
    private final List<Set<Integer>> seatBuckets = new ArrayList<>();

    // ReadWriteLock allows multiple concurrent reads but exclusive writes
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public InMemoryCarRepository() {
        for (int i = 0; i <= MAX_SEATS; i++) {
            seatBuckets.add(new LinkedHashSet<>());
        }
    }

    @Override
    public Car get(int carId) {
        lock.readLock().lock();

        try {
            Car car = cars.get(carId);
            if (car == null) {
                return null;
            }

            return new Car(car.getId(), car.getSeats());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void replace(List<Car> newCars) {
        lock.writeLock().lock();

        try {
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
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void flush() {
        lock.writeLock().lock();

        try {
            cars.clear();
            clearBuckets();
        } finally {
            lock.writeLock().unlock();
        }
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
        int newSeats = currentSeats + deltaSeats;

        moveBetweenBuckets(carId, currentSeats, newSeats);
        car.setAvailableSeats(newSeats);

        return newSeats;
    }

    @Override
    public Integer findAndReserveCar(int seats) {
        lock.writeLock().lock();

        try {
            Integer carId = findCarInBuckets(seats);

            if (carId != null) {
                adjustAvailableSeats(carId, -seats);
            }

            return carId;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public int getAvailableSeats(int carId) {
        lock.readLock().lock();

        try {
            Car car = cars.get(carId);

            return car != null ? car.getAvailableSeats() : 0;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public int releaseSeats(int carId, int seats) {
        lock.writeLock().lock();

        try {
            return adjustAvailableSeats(carId, seats);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean tryReserveSeats(int carId, int seats) {
        lock.writeLock().lock();

        try {
            Car car = cars.get(carId);
            if (car != null && car.getAvailableSeats() >= seats) {
                adjustAvailableSeats(carId, -seats);
                return true;
            }

            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Find a car in buckets with enough available seats.
     * Searches from the exact seat count upwards to find the best fit.
     */
    private Integer findCarInBuckets(int seats) {
        for (int i = seats; i <= MAX_SEATS; i++) {
            Set<Integer> bucket = seatBuckets.get(i);

            if (!bucket.isEmpty()) {
                // Return the first car (FIFO - fair allocation)
                return bucket.iterator().next();
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
            seatBuckets.get(fromSeats).remove(carId);
        }

        // Add to new bucket
        if (toSeats >= 0 && toSeats <= MAX_SEATS) {
            seatBuckets.get(toSeats).add(carId);
        }
    }
}
