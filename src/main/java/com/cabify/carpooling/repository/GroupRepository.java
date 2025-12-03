package com.cabify.carpooling.repository;

import java.util.LinkedHashMap;

/**
 * Repository interface for managing group data.
 */
public interface GroupRepository {

    /**
     * Get the number of people in a group.
     *
     * @param groupId The group ID
     * @return The number of people, or null if not found
     */
    Integer getPeople(int groupId);

    /**
     * Save a group.
     *
     * @param groupId The group ID
     * @param people  The number of people
     */
    void save(int groupId, int people);

    /**
     * Remove a group.
     *
     * @param groupId The group ID
     */
    void remove(int groupId);

    /**
     * Get the waiting queue (groupId -> people).
     *
     * @return A LinkedHashMap representing the queue in FIFO order
     */
    LinkedHashMap<Integer, Integer> getQueue();

    /**
     * Check if there are groups waiting for allocation with the given number of seats or less.
     *
     * @param seats The number of available seats
     * @return true if there are groups that could be allocated
     */
    boolean areThereGroupsForAllocation(int seats);

    /**
     * Replace the waiting queue.
     *
     * @param queue The new queue
     */
    void replaceQueue(LinkedHashMap<Integer, Integer> queue);

    /**
     * Add a group to the waiting queue.
     *
     * @param groupId The group ID
     * @param people  The number of people
     */
    void enqueue(int groupId, int people);

    /**
     * Remove a group from the waiting queue.
     *
     * @param groupId The group ID
     */
    void dequeue(int groupId);

    /**
     * Flush all group data.
     */
    void flush();
}
