package com.cabify.carpooling.service;

import com.cabify.carpooling.repository.JourneyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service for managing journey assignments and car allocations.
 */
@Service
public class JourneyService {

    private static final Logger log = LoggerFactory.getLogger(JourneyService.class);

    private final CarService carService;
    private final GroupService groupService;
    private final JourneyRepository journeyRepository;

    public JourneyService(CarService carService,
            GroupService groupService,
            JourneyRepository journeyRepository) {
        this.carService = carService;
        this.groupService = groupService;
        this.journeyRepository = journeyRepository;
    }

    /**
     * Retrieve the car assigned to a group.
     */
    public Integer getCar(int groupId) {
        return journeyRepository.getCar(groupId);
    }

    /**
     * Remove the journey of a group.
     */
    public void remove(int groupId) {
        journeyRepository.remove(groupId);
    }

    /**
     * Process a dropoff for a group.
     */
    public void dropoff(int groupId, int people) {
        long startTime = System.currentTimeMillis();

        // Check if group has traveled
        Integer carId = getCar(groupId);

        if (carId != null) {
            remove(groupId);
            updateCarAllocation(carId, people);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Dropoff processed for traveling group. group_id={}, people={}, car_id={}, duration_ms={}",
                    groupId, people, carId, duration);
        } else {
            long duration = System.currentTimeMillis() - startTime;
            log.info("Dropoff processed for waiting group. group_id={}, people={}, duration_ms={}",
                    groupId, people, duration);
        }

        groupService.remove(groupId);
    }

    /**
     * Request a journey for a group, allocating a car or queueing it.
     */
    public void request(int groupId, int people) {
        long startTime = System.currentTimeMillis();

        if (getCar(groupId) != null) {
            log.warn("Journey request for group already assigned. group_id={}, people={}", groupId, people);
            return;
        }

        Integer carId = carService.findAndReserveCar(people);

        if (carId != null) {
            journeyRepository.save(groupId, carId);
            groupService.removeFromWaitingList(groupId);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Journey assigned successfully. group_id={}, people={}, car_id={}, duration_ms={}",
                    groupId, people, carId, duration);

            return;
        }

        groupService.addToWaitingList(groupId, people);

        long duration = System.currentTimeMillis() - startTime;

        log.info("Journey queued - no available car. group_id={}, people={}, duration_ms={}",
                groupId, people, duration);
    }

    /**
     * Reassign free seats of a car to waiting groups after a dropoff.
     */
    public void updateCarAllocation(int carId, int newFreeSeats) {
        long startTime = System.currentTimeMillis();

        // Atomic operation: release seats and get the new total available seats
        int totalFreeSeats = carService.releaseSeats(carId, newFreeSeats);

        // Select groups that can fit in the available seats
        LinkedHashMap<Integer, Integer> groups = groupService.selectGroupsToAllocate(totalFreeSeats);
        StringBuilder assignedGroups = new StringBuilder();
        int successfullyAssigned = 0;

        // Try to assign each group atomically
        for (Map.Entry<Integer, Integer> entry : groups.entrySet()) {
            int groupId = entry.getKey();
            int people = entry.getValue();

            // Atomic operation: try to reserve seats for this group
            boolean reserved = carService.tryReserveSeats(carId, people);

            if (reserved) {
                journeyRepository.save(groupId, carId);
                groupService.removeFromWaitingList(groupId);

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
        int remainingSeats = carService.getAvailableSeats(carId);

        log.info(
                "Car allocation updated after dropoff. car_id={}, new_free_seats={}, total_free_seats={}, " +
                        "groups_attempted={}, groups_assigned={}, total_people_assigned={}, remaining_seats={}, " +
                        "assigned_groups=[{}], duration_ms={}",
                carId, newFreeSeats, totalFreeSeats, groups.size(),
                (assignedGroups.length() > 0 ? groups.size() : 0),
                successfullyAssigned, remainingSeats, assignedGroups.toString(), duration);
    }
}
