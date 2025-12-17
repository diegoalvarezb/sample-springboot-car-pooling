package com.cabify.carpooling.repository;

import java.util.LinkedHashMap;

/**
 * Repository interface for managing group data.
 */
public interface GroupRepository {

    /**
     * Get the number of people in a group.
     */
    Integer getPeople(int groupId);

    /**
     * Save a group.
     */
    void save(int groupId, int people);

    /**
     * Remove a group.
     */
    void remove(int groupId);

    /**
     * Get the waiting queue (groupId -> people).
     */
    LinkedHashMap<Integer, Integer> getQueue();

    /**
     * Check if there are groups waiting for allocation with the given number of
     * seats or less.
     */
    boolean areThereGroupsForAllocation(int seats);

    /**
     * Replace the waiting queue.
     */
    void replaceQueue(LinkedHashMap<Integer, Integer> queue);

    /**
     * Add a group to the waiting queue.
     */
    void enqueue(int groupId, int people);

    /**
     * Remove a group from the waiting queue.
     */
    void dequeue(int groupId);

    /**
     * Flush all group data.
     */
    void flush();
}
