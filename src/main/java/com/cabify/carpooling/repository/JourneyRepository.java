package com.cabify.carpooling.repository;

/**
 * Repository interface for managing journey assignments (group -> car mapping).
 */
public interface JourneyRepository {

    /**
     * Get the car ID assigned to a group.
     */
    Integer getCar(int groupId);

    /**
     * Save a journey assignment (assign a group to a car).
     */
    void save(int groupId, int carId);

    /**
     * Remove a journey assignment.
     */
    void remove(int groupId);

    /**
     * Flush all journey data.
     */
    void flush();
}
