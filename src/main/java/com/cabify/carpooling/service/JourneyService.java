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
     * Request a journey for a group, allocating a car or queueing it.
     */
    public void request(int groupId, int people) {
        long startTime = System.currentTimeMillis();

        if (getCar(groupId) != null) {
            log.warn("Journey request for group already assigned. group_id={}, people={}", groupId, people);
            return;
        }

        Integer carId = carService.findCar(people);

        if (carId != null) {
            journeyRepository.save(groupId, carId);
            groupService.removeFromWaitingList(groupId);
            carService.updateAvailableSeats(carId, people * -1);

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
     * Reassign free seats of a car to waiting groups.
     */
    public void updateCarAllocation(int carId, int newFreeSeats) {
        long startTime = System.currentTimeMillis();
        int totalFreeSeats = newFreeSeats + carService.getAvailableSeats(carId);
        int newAssignedSeats = 0;

        LinkedHashMap<Integer, Integer> groups = groupService.selectGroupsToAllocate(totalFreeSeats);
        StringBuilder assignedGroups = new StringBuilder();

        for (Map.Entry<Integer, Integer> entry : groups.entrySet()) {
            int groupId = entry.getKey();
            int people = entry.getValue();

            journeyRepository.save(groupId, carId);
            groupService.removeFromWaitingList(groupId);

            newAssignedSeats += people;
            if (assignedGroups.length() > 0) {
                assignedGroups.append(", ");
            }
            assignedGroups.append(String.format("group_id=%d,people=%d", groupId, people));
        }

        carService.updateAvailableSeats(carId, newFreeSeats - newAssignedSeats);

        long duration = System.currentTimeMillis() - startTime;

        log.info(
                "Car allocation updated after dropoff. car_id={}, new_free_seats={}, total_free_seats={}, groups_count={}, total_people_assigned={}, remaining_seats={}, assigned_groups=[{}], duration_ms={}",
                carId, newFreeSeats, totalFreeSeats, groups.size(), newAssignedSeats,
                totalFreeSeats - newAssignedSeats, assignedGroups.toString(), duration);
    }
}
