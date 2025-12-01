package com.cabify.carpooling.util;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MapHelper utility class.
 */
class MapHelperTest {

    @Test
    void testBinarySearchOrNext_ExactMatch() {
        LinkedHashMap<Integer, Integer> map = new LinkedHashMap<>();
        map.put(1, 6);
        map.put(2, 4);
        map.put(3, 2);

        Integer result = MapHelper.binarySearchOrNext(map, 4);
        assertEquals(2, result);
    }

    @Test
    void testBinarySearchOrNext_NextBigger() {
        LinkedHashMap<Integer, Integer> map = new LinkedHashMap<>();
        map.put(1, 6);
        map.put(2, 4);
        map.put(3, 2);

        Integer result = MapHelper.binarySearchOrNext(map, 3);
        assertEquals(2, result);
    }

    @Test
    void testBinarySearchOrNext_NoMatch() {
        LinkedHashMap<Integer, Integer> map = new LinkedHashMap<>();
        map.put(1, 4);
        map.put(2, 3);
        map.put(3, 2);

        Integer result = MapHelper.binarySearchOrNext(map, 5);
        assertNull(result);
    }

    @Test
    void testBinarySearchOrNext_EmptyMap() {
        LinkedHashMap<Integer, Integer> map = new LinkedHashMap<>();

        Integer result = MapHelper.binarySearchOrNext(map, 3);
        assertNull(result);
    }

    @Test
    void testSortByValueDescending() {
        LinkedHashMap<Integer, Integer> map = new LinkedHashMap<>();
        map.put(1, 2);
        map.put(2, 5);
        map.put(3, 1);
        map.put(4, 4);

        LinkedHashMap<Integer, Integer> sorted = MapHelper.sortByValueDescending(map);

        assertEquals(4, sorted.size());
        Integer[] keys = sorted.keySet().toArray(new Integer[0]);
        assertEquals(2, keys[0]); // 5
        assertEquals(4, keys[1]); // 4
        assertEquals(1, keys[2]); // 2
        assertEquals(3, keys[3]); // 1
    }

    @Test
    void testReorderMapElement() {
        LinkedHashMap<Integer, Integer> map = new LinkedHashMap<>();
        map.put(1, 6);
        map.put(2, 4);
        map.put(3, 2);

        // Update element 2 to have value 1
        map.put(2, 1);
        LinkedHashMap<Integer, Integer> reordered = MapHelper.reorderMapElement(map, 2);

        Integer[] values = reordered.values().toArray(new Integer[0]);
        assertEquals(6, values[0]);
        assertEquals(2, values[1]);
        assertEquals(1, values[2]);
    }
}
