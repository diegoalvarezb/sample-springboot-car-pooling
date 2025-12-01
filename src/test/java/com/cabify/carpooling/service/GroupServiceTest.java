package com.cabify.carpooling.service;

import com.cabify.carpooling.repository.GroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GroupService.
 */
@SpringBootTest
class GroupServiceTest {

    @Autowired
    private GroupService groupService;

    @Autowired
    private GroupRepository groupRepository;

    @BeforeEach
    void setUp() {
        groupRepository.flush();
    }

    @Test
    void testSelectGroupsToAllocate_FitsMultipleGroups() {
        groupService.add(1, 1);
        groupService.add(2, 2);
        groupService.add(3, 4);
        
        groupService.addToWaitingList(1, 1);
        groupService.addToWaitingList(2, 2);
        groupService.addToWaitingList(3, 4);

        LinkedHashMap<Integer, Integer> result = groupService.selectGroupsToAllocate(4);

        assertEquals(2, result.size());
        assertEquals(1, result.get(1));
        assertEquals(2, result.get(2));
    }

    @Test
    void testSelectGroupsToAllocate_SkipsLargerGroups() {
        groupService.add(1, 6);
        groupService.add(2, 4);
        groupService.add(3, 2);
        groupService.add(4, 1);
        
        groupService.addToWaitingList(1, 6);
        groupService.addToWaitingList(2, 4);
        groupService.addToWaitingList(3, 2);
        groupService.addToWaitingList(4, 1);

        LinkedHashMap<Integer, Integer> result = groupService.selectGroupsToAllocate(5);

        assertEquals(2, result.size());
        assertEquals(4, result.get(2));
        assertEquals(1, result.get(4));
    }

    @Test
    void testSelectGroupsToAllocate_NoMatchingGroups() {
        groupService.add(1, 4);
        groupService.add(2, 5);
        groupService.add(3, 6);
        
        groupService.addToWaitingList(1, 4);
        groupService.addToWaitingList(2, 5);
        groupService.addToWaitingList(3, 6);

        LinkedHashMap<Integer, Integer> result = groupService.selectGroupsToAllocate(3);

        assertTrue(result.isEmpty());
    }

    @Test
    void testAddAndRemoveFromWaitingList() {
        groupService.add(1, 2);
        groupService.addToWaitingList(1, 2);

        assertTrue(groupRepository.areThereGroupsForAllocation(2));

        groupService.removeFromWaitingList(1);

        assertFalse(groupRepository.areThereGroupsForAllocation(2));
    }
}
