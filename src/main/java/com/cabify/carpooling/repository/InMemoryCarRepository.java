package com.cabify.carpooling.repository;

import com.cabify.carpooling.model.Car;
import com.cabify.carpooling.util.MapHelper;
import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of CarRepository.
 * Thread-safe using ConcurrentHashMap.
 */
@Repository
public class InMemoryCarRepository implements CarRepository {

    private final Map<Integer, Car> cars = new ConcurrentHashMap<>();

    @Override
    public Car get(int carId) {
        Car car = cars.get(carId);
        if (car == null) {
            return null;
        }
        // Return a copy with only id and seats (not availableSeats)
        return new Car(car.getId(), car.getSeats());
    }

    @Override
    public void replace(List<Car> newCars) {
        flush();
        for (Car car : newCars) {
            cars.put(car.getId(), new Car(car.getId(), car.getSeats()));
        }
    }

    @Override
    public void flush() {
        cars.clear();
    }

    @Override
    public LinkedHashMap<Integer, Integer> getAvailabilityMap() {
        Map<Integer, Integer> availabilityMap = new ConcurrentHashMap<>();
        
        for (Car car : cars.values()) {
            availabilityMap.put(car.getId(), car.getAvailableSeats());
        }

        // Sort by available seats in descending order
        return MapHelper.sortByValueDescending(availabilityMap);
    }

    @Override
    public void updateAvailabilityMap(LinkedHashMap<Integer, Integer> availabilityMap) {
        for (Map.Entry<Integer, Integer> entry : availabilityMap.entrySet()) {
            int carId = entry.getKey();
            int availableSeats = entry.getValue();
            
            Car car = cars.get(carId);
            if (car != null) {
                car.setAvailableSeats(availableSeats);
            }
        }
    }
}
