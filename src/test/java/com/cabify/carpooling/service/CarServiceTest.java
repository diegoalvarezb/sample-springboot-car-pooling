package com.cabify.carpooling.service;

import com.cabify.carpooling.model.Car;
import com.cabify.carpooling.repository.CarRepository;
import com.cabify.carpooling.repository.GroupRepository;
import com.cabify.carpooling.repository.JourneyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CarService.
 */
@SpringBootTest
class CarServiceTest {

    @Autowired
    private CarService carService;

    @Autowired
    private CarRepository carRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private JourneyRepository journeyRepository;

    @BeforeEach
    void setUp() {
        carRepository.flush();
        groupRepository.flush();
        journeyRepository.flush();
    }

    @Test
    void testFindAndReserveCar_ExactMatch() {
        List<Car> cars = Arrays.asList(
                new Car(1, 4),
                new Car(2, 5),
                new Car(3, 6));
        carService.load(cars);

        Integer carId = carService.findAndReserveCar(4);
        assertNotNull(carId);
        assertEquals(1, carId);

        // Verify that seats were reserved
        assertEquals(0, carService.getAvailableSeats(1));
    }

    @Test
    void testFindAndReserveCar_BestFit() {
        List<Car> cars = Arrays.asList(
                new Car(1, 4),
                new Car(2, 5),
                new Car(3, 6));
        carService.load(cars);

        Integer carId = carService.findAndReserveCar(3);
        assertNotNull(carId);
        assertEquals(1, carId); // Should return car with 4 seats (best fit)

        // Verify that seats were reserved (4 - 3 = 1 remaining)
        assertEquals(1, carService.getAvailableSeats(1));
    }

    @Test
    void testFindAndReserveCar_NoAvailableCar() {
        List<Car> cars = Arrays.asList(
                new Car(1, 4),
                new Car(2, 5));
        carService.load(cars);

        Integer carId = carService.findAndReserveCar(6);
        assertNull(carId);
    }

    @Test
    void testFindAndReserveCar_EmptyList() {
        carService.load(Arrays.asList());

        Integer carId = carService.findAndReserveCar(1);
        assertNull(carId);
    }

    @Test
    void testReleaseSeats() {
        List<Car> cars = Arrays.asList(
                new Car(1, 6),
                new Car(2, 4));
        carService.load(cars);

        // Reserve 4 seats - best-fit algorithm will use car 2 (exactly 4 seats)
        Integer carId = carService.findAndReserveCar(4);
        assertEquals(2, carId); // Car 2 should be selected (best-fit)
        assertEquals(6, carService.getAvailableSeats(1)); // Car 1 untouched
        assertEquals(0, carService.getAvailableSeats(2)); // Car 2 fully reserved

        // Release 4 seats back to car 2
        carService.releaseSeats(2, 4);
        assertEquals(6, carService.getAvailableSeats(1)); // Car 1 still untouched
        assertEquals(4, carService.getAvailableSeats(2)); // Car 2 fully available
    }

    @Test
    void testLoad_ResetsState() {
        // Load initial cars
        List<Car> initialCars = Arrays.asList(new Car(1, 4));
        carService.load(initialCars);

        // Add some groups and journeys
        groupRepository.save(100, 2);
        journeyRepository.save(100, 1);

        // Load new cars
        List<Car> newCars = Arrays.asList(new Car(2, 6));
        carService.load(newCars);

        // Verify state is reset
        assertNull(groupRepository.getPeople(100));
        assertNull(journeyRepository.getCar(100));
        assertNull(carRepository.get(1)); // Old car should not exist
        assertNotNull(carRepository.get(2)); // New car should exist
    }
}
