package com.cabify.carpooling.repository;

import org.springframework.stereotype.Repository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of GroupRepository.
 * Thread-safe using ConcurrentHashMap.
 */
@Repository
public class InMemoryGroupRepository implements GroupRepository {

    private final Map<Integer, Integer> groups = new ConcurrentHashMap<>();
    private final LinkedHashMap<Integer, Integer> waitingQueue = new LinkedHashMap<>();
    private final Map<Integer, Integer> peopleCounter = new ConcurrentHashMap<>();

    @Override
    public synchronized Integer getPeople(int groupId) {
        return groups.get(groupId);
    }

    @Override
    public synchronized void save(int groupId, int people) {
        groups.put(groupId, people);
    }

    @Override
    public synchronized void remove(int groupId) {
        Integer people = groups.remove(groupId);
        waitingQueue.remove(groupId);
        
        if (people != null) {
            decrementPeopleCounter(people);
        }
    }

    @Override
    public synchronized LinkedHashMap<Integer, Integer> getQueue() {
        return new LinkedHashMap<>(waitingQueue);
    }

    @Override
    public synchronized boolean areThereGroupsForAllocation(int seats) {
        for (int people = seats; people > 0; people--) {
            Integer count = peopleCounter.get(people);
            if (count != null && count > 0) {
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized void replaceQueue(LinkedHashMap<Integer, Integer> queue) {
        waitingQueue.clear();
        peopleCounter.clear();
        
        for (Map.Entry<Integer, Integer> entry : queue.entrySet()) {
            int groupId = entry.getKey();
            int people = entry.getValue();
            
            waitingQueue.put(groupId, people);
            
            // Update counter
            peopleCounter.put(people, peopleCounter.getOrDefault(people, 0) + 1);
        }
    }

    @Override
    public synchronized void enqueue(int groupId, int people) {
        waitingQueue.put(groupId, people);
        peopleCounter.put(people, peopleCounter.getOrDefault(people, 0) + 1);
    }

    @Override
    public synchronized void dequeue(int groupId) {
        Integer people = waitingQueue.remove(groupId);
        if (people != null) {
            decrementPeopleCounter(people);
        }
    }

    @Override
    public synchronized void flush() {
        groups.clear();
        waitingQueue.clear();
        peopleCounter.clear();
    }

    private void decrementPeopleCounter(int people) {
        Integer count = peopleCounter.get(people);
        if (count == null || count == 0) {
            return;
        }
        
        int newCount = Math.max(0, count - 1);
        if (newCount == 0) {
            peopleCounter.remove(people);
        } else {
            peopleCounter.put(people, newCount);
        }
    }
}
