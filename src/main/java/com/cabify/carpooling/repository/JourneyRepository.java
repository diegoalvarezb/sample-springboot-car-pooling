package com.cabify.carpooling.repository;

/**
 * Repository interface for managing journey assignments (group -> car mapping).
 */
public interface JourneyRepository {

    /**
     * Get the car ID assigned to a group.
     *
     * @param groupId The group ID
     * @return The car ID, or null if not assigned
     */
    Integer getCar(int groupId);

    /**
     * Save a journey assignment (assign a group to a car).
     *
     * @param groupId The group ID
     * @param carId   The car ID
     */
    void save(int groupId, int carId);

    /**
     * Remove a journey assignment.
     *
     * @param groupId The group ID
     */
    void remove(int groupId);

    /**
     * Flush all journey data.
     */
    void flush();
}
