package com.cabify.carpooling.service;

import com.cabify.carpooling.exception.ExistingGroupException;
import com.cabify.carpooling.exception.GroupNotFoundException;
import com.cabify.carpooling.model.Car;
import com.cabify.carpooling.repository.CarRepository;
import com.cabify.carpooling.repository.GroupRepository;
import com.cabify.carpooling.repository.JourneyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Service for managing journey assignments and car allocations.
 */
@Service
public class CarPoolingService {

    private static final Logger log = LoggerFactory.getLogger(CarPoolingService.class);

    private final CarRepository carRepository;
    private final GroupRepository groupRepository;
    private final JourneyRepository journeyRepository;

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();

    public CarPoolingService(
            CarRepository carRepository,
            GroupRepository groupRepository,
            JourneyRepository journeyRepository) {
        this.carRepository = carRepository;
        this.groupRepository = groupRepository;
        this.journeyRepository = journeyRepository;
    }

    /**
     * Reset the application state and load the incoming list of cars.
     */
    public synchronized void loadCars(List<Car> cars) {
        writeLock.lock();
        try {
            long startTime = System.currentTimeMillis();
            int carCount = cars.size();

            carRepository.flush();
            groupRepository.flush();
            journeyRepository.flush();

            carRepository.replaceAll(cars);

            long duration = System.currentTimeMillis() - startTime;

            log.info("Cars loaded - application state reset. cars_count={}, duration_ms={}", carCount, duration);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Request a journey for a group, allocating a car or queueing it.
     */
    public synchronized void requestJourney(int groupId, int people) {
        writeLock.lock();
        try {
            long startTime = System.currentTimeMillis();

            // Check if group exists
            if (groupRepository.getPeople(groupId) != null) {
                throw new ExistingGroupException();
            }

            groupRepository.save(groupId, people);

            if (journeyRepository.getCar(groupId) != null) {
                log.warn("Journey request for group already assigned. group_id={}, people={}", groupId, people);
                return;
            }

            // Find car
            Integer carId = carRepository.findAndReserveCar(people);

            if (carId != null) {
                journeyRepository.save(groupId, carId);

                long duration = System.currentTimeMillis() - startTime;
                log.info("Journey assigned successfully. group_id={}, people={}, car_id={}, duration_ms={}",
                        groupId, people, carId, duration);
            } else {
                groupRepository.enqueue(groupId, people);

                long duration = System.currentTimeMillis() - startTime;
                log.info("Journey queued - no available car. group_id={}, people={}, duration_ms={}",
                        groupId, people, duration);
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Process a dropoff for a group.
     */
    public synchronized void dropoff(int groupId) {
        writeLock.lock();
        try {
            long startTime = System.currentTimeMillis();

            // Check if group exists
            Integer people = groupRepository.getPeople(groupId);
            if (people == null) {
                throw new GroupNotFoundException();
            }

            // Check if group has traveled
            Integer carId = journeyRepository.getCar(groupId);

            if (carId != null) {
                journeyRepository.remove(groupId);

                updateCarAllocation(carId, people);

                long duration = System.currentTimeMillis() - startTime;
                log.info("Dropoff processed for traveling group. group_id={}, people={}, car_id={}, duration_ms={}",
                        groupId, people, carId, duration);
            } else {
                long duration = System.currentTimeMillis() - startTime;
                log.info("Dropoff processed for waiting group. group_id={}, people={}, duration_ms={}",
                        groupId, people, duration);
            }

            groupRepository.remove(groupId);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Locate the car assigned to a group.
     */
    public synchronized Car locate(int groupId) {
        readLock.lock();
        try {
            Integer people = groupRepository.getPeople(groupId);
            if (people == null) {
                throw new GroupNotFoundException();
            }

            Integer carId = journeyRepository.getCar(groupId);
            if (carId == null) {
                return null;
            }

            return carRepository.get(carId);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * Reassign free seats of a car to waiting groups after a dropoff.
     */
    private void updateCarAllocation(int carId, int newFreeSeats) {
        long startTime = System.currentTimeMillis();

        // Atomic operation: release seats and get the new total available seats
        int totalFreeSeats = carRepository.releaseSeats(carId, newFreeSeats);

        log.debug("Seats released atomically. car_id={}, seats_released={}, total_available={}",
                carId, newFreeSeats, totalFreeSeats);

        // Select groups that can fit in the available seats
        LinkedHashMap<Integer, Integer> groups = selectGroupsToAllocate(totalFreeSeats);
        StringBuilder assignedGroups = new StringBuilder();
        int successfullyAssigned = 0;

        // Try to assign each group atomically
        for (Map.Entry<Integer, Integer> entry : groups.entrySet()) {
            int groupId = entry.getKey();
            int people = entry.getValue();

            // Atomic operation: try to reserve seats for this group
            boolean reserved = carRepository.tryReserveSeats(carId, people);

            if (reserved) {
                log.debug("Seats reserved successfully. car_id={}, seats_reserved={}", carId, people);

                journeyRepository.save(groupId, carId);
                groupRepository.dequeue(groupId);

                successfullyAssigned += people;
                if (assignedGroups.length() > 0) {
                    assignedGroups.append(", ");
                }
                assignedGroups.append(String.format("group_id=%d,people=%d", groupId, people));
            } else {
                log.warn("Could not reserve seats for group during reallocation (race condition). " +
                        "car_id={}, group_id={}, people={}", carId, groupId, people);

                break;
            }
        }

        long duration = System.currentTimeMillis() - startTime;
        int remainingSeats = carRepository.getAvailableSeats(carId);

        log.info(
                "Car allocation updated after dropoff. car_id={}, new_free_seats={}, total_free_seats={}, " +
                        "groups_attempted={}, groups_assigned={}, total_people_assigned={}, remaining_seats={}, " +
                        "assigned_groups=[{}], duration_ms={}",
                carId, newFreeSeats, totalFreeSeats, groups.size(),
                (assignedGroups.length() > 0 ? groups.size() : 0),
                successfullyAssigned, remainingSeats, assignedGroups.toString(), duration);
    }

    /**
     * Select the optimal set of groups that fits into the provided seats.
     * Returns a map of groupId -> people count.
     */
    private LinkedHashMap<Integer, Integer> selectGroupsToAllocate(int seats) {
        long startTime = System.currentTimeMillis();

        LinkedHashMap<Integer, Integer> result = new LinkedHashMap<>();
        LinkedHashMap<Integer, Integer> groups = groupRepository.getWaitingQueue();

        int queueSize = groups.size();
        int pendingSeats = seats;

        for (Map.Entry<Integer, Integer> entry : groups.entrySet()) {
            int groupId = entry.getKey();
            int people = entry.getValue();

            if (pendingSeats <= 0) {
                break;
            }

            if (!groupRepository.areThereGroupsForAllocation(pendingSeats)) {
                break;
            }

            if (people <= pendingSeats) {
                result.put(groupId, people);
                pendingSeats -= people;
            }
        }

        long duration = System.currentTimeMillis() - startTime;

        if (!result.isEmpty()) {
            int totalPeopleAllocated = result.values().stream().mapToInt(Integer::intValue).sum();

            log.debug(
                    "Groups selected for allocation. available_seats={}, selected_groups_count={}, queue_size={}, total_people_allocated={}, remaining_seats={}, duration_ms={}",
                    seats, result.size(), queueSize, totalPeopleAllocated, pendingSeats, duration);
        }

        return result;
    }
}
