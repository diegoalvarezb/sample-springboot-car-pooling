package com.cabify.carpooling.repository.inmemory;

import com.cabify.carpooling.repository.JourneyRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of JourneyRepository.
 *
 * Thread-safe using ConcurrentHashMap.
 */
@Repository
public class InMemoryJourneyRepository implements JourneyRepository {

    private final Map<Integer, Integer> journeys = new ConcurrentHashMap<>();

    @Override
    public Integer getCar(int groupId) {
        return journeys.get(groupId);
    }

    @Override
    public void save(int groupId, int carId) {
        journeys.put(groupId, carId);
    }

    @Override
    public void remove(int groupId) {
        journeys.remove(groupId);
    }

    @Override
    public void flush() {
        journeys.clear();
    }
}
