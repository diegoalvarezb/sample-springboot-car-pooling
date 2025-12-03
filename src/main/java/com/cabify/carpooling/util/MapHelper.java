package com.cabify.carpooling.util;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for map operations, particularly for handling availability maps.
 * Implements binary search and sorting operations for car availability.
 */
public final class MapHelper {

    /**
     * Private constructor to prevent instantiation.
     */
    private MapHelper() {
        throw new AssertionError("Utility class");
    }

    /**
     * Perform a binary search in a descending sorted map and return the matching key
     * or the closest entry with available seats greater than or equal to the requested value.
     *
     * @param map A LinkedHashMap sorted by values in descending order (carId -> availableSeats)
     * @param value The number of seats required
     *
     * @return The car ID that best fits the requirement, or null if no car is found
     */
    public static Integer binarySearchOrNext(LinkedHashMap<Integer, Integer> map, int value) {
        if (map.isEmpty()) {
            return null;
        }

        List<Integer> keys = new ArrayList<>(map.keySet());
        List<Integer> values = new ArrayList<>(map.values());

        int left = 0;
        int right = map.size() - 1;
        Integer resultKey = null;

        while (left <= right) {
            int middle = (left + right) / 2;
            int middleValue = values.get(middle);
            int middleKey = keys.get(middle);

            if (middleValue == value) {
                return middleKey;
            }

            if (middleValue > value) {
                resultKey = middleKey;
                left = middle + 1;
            } else {
                right = middle - 1;
            }
        }

        return resultKey;
    }

    /**
     * Reorder a descending map to keep the collection sorted after updating one element.
     *
     * @param map The map to reorder (carId -> availableSeats)
     * @param key The key that was updated
     *
     * @return A new LinkedHashMap with elements sorted in descending order by value
     */
    public static LinkedHashMap<Integer, Integer> reorderMapElement(
            LinkedHashMap<Integer, Integer> map, Integer key) {
        if (!map.containsKey(key)) {
            return map;
        }

        // Simply re-sort the entire map in descending order by value
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new));
    }

    /**
     * Sort a map by values in descending order.
     *
     * @param map The map to sort
     *
     * @return A new LinkedHashMap sorted by values in descending order
     */
    public static LinkedHashMap<Integer, Integer> sortByValueDescending(Map<Integer, Integer> map) {
        return map.entrySet()
                .stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new));
    }
}
