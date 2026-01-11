package com.cabify.carpooling.service;

import com.cabify.carpooling.exception.ExistingGroupException;
import com.cabify.carpooling.exception.GroupNotFoundException;
import com.cabify.carpooling.model.Car;
import com.cabify.carpooling.repository.CarRepository;
import com.cabify.carpooling.repository.GroupRepository;
import com.cabify.carpooling.repository.JourneyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for CarPoolingService.
 */
@SpringBootTest
class CarPoolingServiceTest {

    @Autowired
    private CarPoolingService carPoolingService;

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
    void testLoadCars_ValidCars() {
        List<Car> cars = Arrays.asList(
                new Car(1, 4),
                new Car(2, 5),
                new Car(3, 6));

        carPoolingService.loadCars(cars);

        assertNotNull(carRepository.get(1));
        assertNotNull(carRepository.get(2));
        assertNotNull(carRepository.get(3));
        assertEquals(4, carRepository.get(1).getSeats());
        assertEquals(5, carRepository.get(2).getSeats());
        assertEquals(6, carRepository.get(3).getSeats());
    }

    @Test
    void testLoadCars_ResetsAllRepositories() {
        // Setup initial state
        carPoolingService.loadCars(Arrays.asList(new Car(1, 4)));
        carPoolingService.requestJourney(100, 2);

        // Load new cars
        carPoolingService.loadCars(Arrays.asList(new Car(2, 6)));

        // Verify all state is reset
        assertNull(carRepository.get(1));
        assertNull(groupRepository.getPeople(100));
        assertNull(journeyRepository.getCar(100));
        assertNotNull(carRepository.get(2));
    }

    @Test
    void testRequestJourney_AssignsCarImmediately() {
        carPoolingService.loadCars(Arrays.asList(new Car(1, 6)));

        carPoolingService.requestJourney(1, 4);

        assertNotNull(journeyRepository.getCar(1));
        assertEquals(1, journeyRepository.getCar(1));
        assertEquals(2, carRepository.getAvailableSeats(1));
    }

    @Test
    void testRequestJourney_QueuesWhenNoCarAvailable() {
        carPoolingService.loadCars(Arrays.asList(new Car(1, 4)));

        // First journey takes all seats
        carPoolingService.requestJourney(1, 4);

        // Second journey should be queued
        carPoolingService.requestJourney(2, 2);

        assertNull(journeyRepository.getCar(2));
        assertNotNull(groupRepository.getPeople(2));
        assertTrue(groupRepository.getWaitingQueue().containsKey(2));
    }

    @Test
    void testRequestJourney_ThrowsExceptionForDuplicateGroup() {
        carPoolingService.loadCars(Arrays.asList(new Car(1, 6)));

        carPoolingService.requestJourney(1, 2);

        assertThrows(ExistingGroupException.class, () -> {
            carPoolingService.requestJourney(1, 3);
        });
    }

    @Test
    void testRequestJourney_BestFitAlgorithm() {
        List<Car> cars = Arrays.asList(
                new Car(1, 4),
                new Car(2, 5),
                new Car(3, 6));
        carPoolingService.loadCars(cars);

        // Request 3 seats - should use car with 4 seats (best fit)
        carPoolingService.requestJourney(1, 3);

        assertEquals(1, journeyRepository.getCar(1));
        assertEquals(1, carRepository.getAvailableSeats(1));
    }

    @Test
    void testRequestJourney_FIFOAllocation() {
        List<Car> cars = Arrays.asList(
                new Car(1, 4),
                new Car(2, 4));
        carPoolingService.loadCars(cars);

        // Both cars have same capacity, first request should get first car
        carPoolingService.requestJourney(1, 2);
        assertEquals(1, journeyRepository.getCar(1));

        // Second request: car 1 now has 2 seats available (bucket 2), car 2 has 4 (bucket 4)
        // Algorithm picks best fit from bucket 2 (car 1) since it needs exactly 2 seats
        carPoolingService.requestJourney(2, 2);
        assertEquals(1, journeyRepository.getCar(2)); // Car 1 is selected (best fit)
    }

    @Test
    void testDropoff_RemovesGroupAndReleasesSeats() {
        carPoolingService.loadCars(Arrays.asList(new Car(1, 6)));

        carPoolingService.requestJourney(1, 4);
        assertEquals(2, carRepository.getAvailableSeats(1));

        carPoolingService.dropoff(1);

        assertNull(groupRepository.getPeople(1));
        assertNull(journeyRepository.getCar(1));
        assertEquals(6, carRepository.getAvailableSeats(1));
    }

    @Test
    void testDropoff_ReassignsQueuedGroups() {
        carPoolingService.loadCars(Arrays.asList(new Car(1, 6)));

        // First journey takes all seats
        carPoolingService.requestJourney(1, 6);

        // Queue two groups
        carPoolingService.requestJourney(2, 2);
        carPoolingService.requestJourney(3, 3);

        // Dropoff first group
        carPoolingService.dropoff(1);

        // Both queued groups should be assigned
        assertNotNull(journeyRepository.getCar(2));
        assertNotNull(journeyRepository.getCar(3));
        assertEquals(1, journeyRepository.getCar(2));
        assertEquals(1, journeyRepository.getCar(3));
    }

    @Test
    void testDropoff_ThrowsExceptionForNonExistentGroup() {
        assertThrows(GroupNotFoundException.class, () -> {
            carPoolingService.dropoff(999);
        });
    }

    @Test
    void testDropoff_WaitingGroup() {
        carPoolingService.loadCars(Arrays.asList(new Car(1, 4)));

        // Fill car
        carPoolingService.requestJourney(1, 4);

        // Queue a group
        carPoolingService.requestJourney(2, 2);

        // Dropoff waiting group
        carPoolingService.dropoff(2);

        assertNull(groupRepository.getPeople(2));
        assertNull(journeyRepository.getCar(2));
    }

    @Test
    void testLocate_ReturnsCarForAssignedGroup() {
        carPoolingService.loadCars(Arrays.asList(new Car(1, 6)));

        carPoolingService.requestJourney(1, 4);

        Car car = carPoolingService.locate(1);
        assertNotNull(car);
        assertEquals(1, car.getId());
        assertEquals(6, car.getSeats());
    }

    @Test
    void testLocate_ReturnsNullForQueuedGroup() {
        carPoolingService.loadCars(Arrays.asList(new Car(1, 4)));

        carPoolingService.requestJourney(1, 4);
        carPoolingService.requestJourney(2, 2);

        Car car = carPoolingService.locate(2);
        assertNull(car);
    }

    @Test
    void testLocate_ThrowsExceptionForNonExistentGroup() {
        assertThrows(GroupNotFoundException.class, () -> {
            carPoolingService.locate(999);
        });
    }

    @Test
    void testCompleteFlow_MultipleGroupsAndCars() {
        List<Car> cars = Arrays.asList(
                new Car(1, 6),
                new Car(2, 4),
                new Car(3, 5));
        carPoolingService.loadCars(cars);

        // Assign groups to cars
        carPoolingService.requestJourney(1, 6);
        carPoolingService.requestJourney(2, 4);
        carPoolingService.requestJourney(3, 5);

        // Verify assignments
        assertEquals(1, journeyRepository.getCar(1));
        assertEquals(2, journeyRepository.getCar(2));
        assertEquals(3, journeyRepository.getCar(3));

        // Queue some groups
        carPoolingService.requestJourney(4, 2);
        carPoolingService.requestJourney(5, 3);

        // Dropoff group 1
        carPoolingService.dropoff(1);

        // Queued groups should be assigned
        assertNotNull(journeyRepository.getCar(4));
        assertNotNull(journeyRepository.getCar(5));
    }

    @Test
    void testReallocation_OptimalGroupSelection() {
        carPoolingService.loadCars(Arrays.asList(new Car(1, 6)));

        // Fill car
        carPoolingService.requestJourney(1, 6);

        // Queue groups: 1, 2, 3, 4
        carPoolingService.requestJourney(2, 1);
        carPoolingService.requestJourney(3, 2);
        carPoolingService.requestJourney(4, 3);
        carPoolingService.requestJourney(5, 4);

        // Dropoff to free 6 seats
        carPoolingService.dropoff(1);

        // Should assign groups 2, 3, 4 (1+2+3=6) and leave group 5 queued
        assertNotNull(journeyRepository.getCar(2));
        assertNotNull(journeyRepository.getCar(3));
        assertNotNull(journeyRepository.getCar(4));
        assertNull(journeyRepository.getCar(5)); // Still queued
    }

    @Test
    void testRequestJourney_AlreadyAssignedGroup() {
        carPoolingService.loadCars(Arrays.asList(new Car(1, 6)));

        carPoolingService.requestJourney(1, 4);
        assertNotNull(journeyRepository.getCar(1));

        // Request again - should throw ExistingGroupException
        assertThrows(ExistingGroupException.class, () -> {
            carPoolingService.requestJourney(1, 4);
        });
    }

    @Test
    void testRequestJourney_EmptyCarList() {
        carPoolingService.loadCars(Arrays.asList());

        // Request journey - should be queued
        carPoolingService.requestJourney(1, 1);

        // Verify group is queued
        assertNull(journeyRepository.getCar(1));
        assertNotNull(groupRepository.getPeople(1));
    }

    @Test
    void testCarRepository_DirectOperations() {
        List<Car> cars = Arrays.asList(
                new Car(1, 6),
                new Car(2, 4));
        carPoolingService.loadCars(cars);

        // Test direct repository operations
        Car car1 = carRepository.get(1);
        assertNotNull(car1);
        assertEquals(6, car1.getSeats());
        assertEquals(6, carRepository.getAvailableSeats(1));

        // Test release seats - note: releaseSeats adds seats, can exceed max capacity
        Integer newSeats = carRepository.releaseSeats(1, 2);
        assertEquals(6, newSeats); // It should cap max seats

        // Test try reserve seats
        boolean reserved = carRepository.tryReserveSeats(1, 3);
        assertTrue(reserved);
        assertEquals(3, carRepository.getAvailableSeats(1)); // It should cap max seats

        // Test try reserve more than available
        boolean reserved2 = carRepository.tryReserveSeats(1, 6);
        assertFalse(reserved2);
        assertEquals(3, carRepository.getAvailableSeats(1)); // Should remain 3
    }

    @Test
    void testSelectGroupsToAllocate_SkipsLargerGroups() {
        carPoolingService.loadCars(Arrays.asList(new Car(1, 5)));

        // Request journeys - first one takes all seats, rest are queued
        carPoolingService.requestJourney(1, 5);
        carPoolingService.requestJourney(2, 4);
        carPoolingService.requestJourney(3, 2);
        carPoolingService.requestJourney(4, 1);

        // Groups 2, 3, 4 should be queued (group 1 is assigned)
        LinkedHashMap<Integer, Integer> queue = groupRepository.getWaitingQueue();
        assertEquals(3, queue.size());

        // Verify groups are in queue and can be allocated with 5 seats
        // (groups of 4, 2, or 1 can fit in 5 seats)
        assertTrue(groupRepository.areThereGroupsForAllocation(5));
    }

    @Test
    void testSelectGroupsToAllocate_NoMatchingGroups() {
        carPoolingService.loadCars(Arrays.asList(new Car(1, 3)));

        // Request journeys larger than available seats
        carPoolingService.requestJourney(1, 4);
        carPoolingService.requestJourney(2, 5);
        carPoolingService.requestJourney(3, 6);

        // All should be queued
        LinkedHashMap<Integer, Integer> queue = groupRepository.getWaitingQueue();
        assertEquals(3, queue.size());

        // Verify no groups can be allocated with 3 seats
        assertFalse(groupRepository.areThereGroupsForAllocation(3));
    }

    @Test
    void testGroupRepository_DirectOperations() {
        // Test save and get
        groupRepository.save(1, 3);
        assertEquals(3, groupRepository.getPeople(1));

        // Test enqueue
        groupRepository.enqueue(1, 3);
        LinkedHashMap<Integer, Integer> queue = groupRepository.getWaitingQueue();
        assertTrue(queue.containsKey(1));
        assertEquals(3, queue.get(1));

        // Test areThereGroupsForAllocation
        assertTrue(groupRepository.areThereGroupsForAllocation(3));
        assertTrue(groupRepository.areThereGroupsForAllocation(4));
        assertFalse(groupRepository.areThereGroupsForAllocation(2));

        // Test dequeue
        groupRepository.dequeue(1);
        queue = groupRepository.getWaitingQueue();
        assertFalse(queue.containsKey(1));

        // Test remove
        groupRepository.remove(1);
        assertNull(groupRepository.getPeople(1));
    }
}
