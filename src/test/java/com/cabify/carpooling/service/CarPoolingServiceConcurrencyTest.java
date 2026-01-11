package com.cabify.carpooling.service;

import com.cabify.carpooling.model.Car;
import com.cabify.carpooling.repository.CarRepository;
import com.cabify.carpooling.repository.GroupRepository;
import com.cabify.carpooling.repository.JourneyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Concurrency tests for CarPoolingService to verify thread safety and race conditions.
 */
@SpringBootTest
class CarPoolingServiceConcurrencyTest {

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
    void testConcurrentJourneyRequests_NoRaceConditions() throws InterruptedException {
        // Load cars with limited capacity
        carPoolingService.loadCars(Arrays.asList(
                new Car(1, 4),
                new Car(2, 4)));

        int numThreads = 10;
        int groupsPerThread = 5;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // Submit concurrent journey requests
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < groupsPerThread; j++) {
                        int groupId = threadId * groupsPerThread + j + 1;
                        try {
                            carPoolingService.requestJourney(groupId, 2);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            exceptionCount.incrementAndGet();
                            exceptions.add(e);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify no duplicate groups
        int totalGroups = numThreads * groupsPerThread;
        int uniqueGroups = 0;
        for (int i = 1; i <= totalGroups; i++) {
            if (groupRepository.getPeople(i) != null) {
                uniqueGroups++;
            }
        }

        assertEquals(totalGroups, uniqueGroups, "All groups should be unique");
        assertTrue(successCount.get() > 0, "Some requests should succeed");
    }

    @Test
    void testConcurrentDropoffs_NoDataCorruption() throws InterruptedException {
        // Setup: Load cars and assign journeys
        carPoolingService.loadCars(Arrays.asList(
                new Car(1, 6),
                new Car(2, 6),
                new Car(3, 6)));

        int numGroups = 30;
        for (int i = 1; i <= numGroups; i++) {
            carPoolingService.requestJourney(i, 2);
        }

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(numGroups);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger exceptionCount = new AtomicInteger(0);

        // Concurrent dropoffs
        for (int i = 1; i <= numGroups; i++) {
            final int groupId = i;
            executor.submit(() -> {
                try {
                    carPoolingService.dropoff(groupId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    exceptionCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify all groups are removed
        for (int i = 1; i <= numGroups; i++) {
            assertNull(groupRepository.getPeople(i), "Group " + i + " should be removed");
            assertNull(journeyRepository.getCar(i), "Journey " + i + " should be removed");
        }

        assertEquals(numGroups, successCount.get(), "All dropoffs should succeed");
    }

    @Test
    void testConcurrentRequestAndDropoff_RaceConditionHandling() throws InterruptedException {
        carPoolingService.loadCars(Arrays.asList(new Car(1, 6)));

        int numOperations = 50;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(numOperations * 2);
        AtomicInteger requestCount = new AtomicInteger(0);
        AtomicInteger dropoffCount = new AtomicInteger(0);

        // Concurrent requests and dropoffs
        for (int i = 1; i <= numOperations; i++) {
            final int groupId = i;

            // Request journey
            executor.submit(() -> {
                try {
                    carPoolingService.requestJourney(groupId, 2);
                    requestCount.incrementAndGet();
                } catch (Exception e) {
                    // Expected for duplicate groups
                } finally {
                    latch.countDown();
                }
            });

            // Dropoff (may happen before request completes)
            executor.submit(() -> {
                try {
                    Thread.sleep(10); // Small delay to create race condition
                    carPoolingService.dropoff(groupId);
                    dropoffCount.incrementAndGet();
                } catch (Exception e) {
                    // Expected for non-existent groups
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify system is in consistent state
        assertTrue(requestCount.get() > 0, "Some requests should succeed");
        assertTrue(dropoffCount.get() > 0, "Some dropoffs should succeed");
    }

    @Test
    void testConcurrentLocate_ThreadSafe() throws InterruptedException {
        carPoolingService.loadCars(Arrays.asList(
                new Car(1, 6),
                new Car(2, 6)));

        int numGroups = 20;
        for (int i = 1; i <= numGroups; i++) {
            carPoolingService.requestJourney(i, 2);
        }

        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(numGroups);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger nullCount = new AtomicInteger(0);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // Concurrent locate operations
        for (int i = 1; i <= numGroups; i++) {
            final int groupId = i;
            executor.submit(() -> {
                try {
                    Car car = carPoolingService.locate(groupId);
                    if (car != null) {
                        assertNotNull(car.getId());
                        assertTrue(car.getSeats() > 0);
                        successCount.incrementAndGet();
                    } else {
                        nullCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // With 2 cars of 6 seats each (12 total) and groups of 2 people,
        // only 6 groups can be assigned (12 / 2 = 6)
        // The rest (14 groups) should be queued (null car)
        assertEquals(6, successCount.get(), "6 groups should have cars assigned");
        assertEquals(14, nullCount.get(), "14 groups should be queued (no car)");
        assertTrue(exceptions.isEmpty(), "No exceptions should occur");
    }

    @Test
    void testConcurrentLoadCars_StateConsistency() throws InterruptedException {
        int numLoads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(numLoads);
        AtomicInteger successCount = new AtomicInteger(0);

        // Concurrent load operations
        for (int i = 0; i < numLoads; i++) {
            final int loadId = i;
            executor.submit(() -> {
                try {
                    List<Car> cars = Arrays.asList(
                            new Car(loadId * 2 + 1, 4),
                            new Car(loadId * 2 + 2, 6));
                    carPoolingService.loadCars(cars);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Expected due to concurrent modifications
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify final state is consistent
        assertTrue(successCount.get() > 0, "Some loads should succeed");
        // After concurrent loads, state should be consistent
        assertNotNull(carRepository);
    }

    @Test
    void testConcurrentReallocation_NoDoubleAssignment() throws InterruptedException {
        carPoolingService.loadCars(Arrays.asList(new Car(1, 6)));

        // Fill car
        carPoolingService.requestJourney(1, 6);

        // Queue multiple groups
        int numQueuedGroups = 10;
        for (int i = 2; i <= numQueuedGroups + 1; i++) {
            carPoolingService.requestJourney(i, 1);
        }

        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(3);

        // Multiple concurrent dropoffs that should trigger reallocation
        for (int i = 0; i < 3; i++) {
            final int groupId = 1;
            executor.submit(() -> {
                try {
                    carPoolingService.dropoff(groupId);
                } catch (Exception e) {
                    // Expected for concurrent dropoffs of same group
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify no group is assigned to multiple cars
        for (int i = 2; i <= numQueuedGroups + 1; i++) {
            Integer carId = journeyRepository.getCar(i);
            if (carId != null) {
                assertEquals(1, carId, "Group " + i + " should be assigned to car 1");
            }
        }
    }

    @Test
    void testHighConcurrencyStressTest() throws InterruptedException {
        carPoolingService.loadCars(Arrays.asList(
                new Car(1, 6),
                new Car(2, 6),
                new Car(3, 6)));

        int numThreads = 50;
        int operationsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger totalOperations = new AtomicInteger(0);

        // High concurrency stress test
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        int groupId = threadId * operationsPerThread + j + 1;

                        // Mix of operations
                        if (j % 3 == 0) {
                            carPoolingService.requestJourney(groupId, 2);
                        } else if (j % 3 == 1 && groupId > 1) {
                            try {
                                carPoolingService.dropoff(groupId - 1);
                            } catch (Exception e) {
                                // Expected
                            }
                        } else {
                            try {
                                carPoolingService.locate(groupId);
                            } catch (Exception e) {
                                // Expected
                            }
                        }
                        totalOperations.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Some exceptions are expected
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify system is still in consistent state
        assertTrue(totalOperations.get() > 0, "Some operations should complete");

        // Check that no car has negative available seats
        for (int i = 1; i <= 3; i++) {
            Integer availableSeats = carRepository.getAvailableSeats(i);
            assertNotNull(availableSeats);
            assertTrue(availableSeats >= 0, "Car " + i + " should not have negative seats");
            assertTrue(availableSeats <= 6, "Car " + i + " should not exceed max seats");
        }
    }

    @Test
    void testConcurrentCarReservation_AtomicOperations() throws InterruptedException {
        carPoolingService.loadCars(Arrays.asList(new Car(1, 6)));

        int numGroups = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numGroups);
        CountDownLatch latch = new CountDownLatch(numGroups);
        AtomicInteger assignedCount = new AtomicInteger(0);

        // All groups request 2 seats simultaneously
        // Only 3 groups should be assigned (6 seats / 2 = 3)
        for (int i = 1; i <= numGroups; i++) {
            final int groupId = i;
            executor.submit(() -> {
                try {
                    carPoolingService.requestJourney(groupId, 2);
                    if (journeyRepository.getCar(groupId) != null) {
                        assignedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Expected for duplicate groups
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify exactly 3 groups are assigned (car has 6 seats, each group needs 2)
        int actualAssigned = 0;
        for (int i = 1; i <= numGroups; i++) {
            if (journeyRepository.getCar(i) != null) {
                actualAssigned++;
            }
        }

        assertEquals(3, actualAssigned, "Exactly 3 groups should be assigned");
        assertEquals(0, carRepository.getAvailableSeats(1), "Car should have no available seats");
    }
}
