package com.cabify.carpooling.service;

import com.cabify.carpooling.repository.GroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Service for managing groups and the waiting queue.
 */
@Service
public class GroupService {

    private static final Logger log = LoggerFactory.getLogger(GroupService.class);

    private final GroupRepository groupRepository;

    public GroupService(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    /**
     * Retrieve the number of people in a group.
     */
    public Integer getPeople(int groupId) {
        return groupRepository.getPeople(groupId);
    }

    /**
     * Register a new group.
     */
    public void add(int groupId, int people) {
        groupRepository.save(groupId, people);
    }

    /**
     * Remove a group.
     */
    public void remove(int groupId) {
        groupRepository.remove(groupId);
    }

    /**
     * Select the optimal set of groups that fits into the provided seats.
     * Returns a map of groupId -> people count.
     */
    public LinkedHashMap<Integer, Integer> selectGroupsToAllocate(int seats) {
        long startTime = System.currentTimeMillis();

        LinkedHashMap<Integer, Integer> result = new LinkedHashMap<>();
        LinkedHashMap<Integer, Integer> groups = groupRepository.getQueue();

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

    /**
     * Add a group to the waiting queue.
     */
    public void addToWaitingList(int groupId, int people) {
        groupRepository.enqueue(groupId, people);
    }

    /**
     * Remove a group from the waiting queue.
     */
    public void removeFromWaitingList(int groupId) {
        groupRepository.dequeue(groupId);
    }
}
