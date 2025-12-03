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
    void testFindCar_ExactMatch() {
        List<Car> cars = Arrays.asList(
                new Car(1, 4),
                new Car(2, 5),
                new Car(3, 6));
        carService.load(cars);

        Integer carId = carService.findCar(4);
        assertNotNull(carId);
        assertEquals(1, carId);
    }

    @Test
    void testFindCar_BestFit() {
        List<Car> cars = Arrays.asList(
                new Car(1, 4),
                new Car(2, 5),
                new Car(3, 6));
        carService.load(cars);

        Integer carId = carService.findCar(3);
        assertNotNull(carId);
        assertEquals(1, carId); // Should return car with 4 seats
    }

    @Test
    void testFindCar_NoAvailableCar() {
        List<Car> cars = Arrays.asList(
                new Car(1, 4),
                new Car(2, 5));
        carService.load(cars);

        Integer carId = carService.findCar(6);
        assertNull(carId);
    }

    @Test
    void testFindCar_EmptyList() {
        carService.load(Arrays.asList());

        Integer carId = carService.findCar(1);
        assertNull(carId);
    }

    @Test
    void testUpdateAvailableSeats() {
        List<Car> cars = Arrays.asList(
                new Car(1, 6),
                new Car(2, 4));
        carService.load(cars);

        carService.updateAvailableSeats(1, -4);

        assertEquals(2, carService.getAvailableSeats(1));
        assertEquals(4, carService.getAvailableSeats(2));
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
