package com.cabify.carpooling.controller;

import com.cabify.carpooling.dto.CarDTO;
import com.cabify.carpooling.dto.JourneyDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for CarPoolingController.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CarPoolingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testStatusEndpoint() throws Exception {
        mockMvc.perform(get("/status"))
                .andExpect(status().isOk());
    }

    @Test
    void testPutCars_ValidPayload() throws Exception {
        List<CarDTO> cars = Arrays.asList(
                new CarDTO(1, 4),
                new CarDTO(2, 6));

        mockMvc.perform(put("/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cars)))
                .andExpect(status().isOk());
    }

    @Test
    void testPutCars_InvalidPayload() throws Exception {
        String invalidJson = "[{\"id\": 1}]"; // Missing seats

        mockMvc.perform(put("/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testPostJourney_ValidPayload() throws Exception {
        // First load cars
        List<CarDTO> cars = Arrays.asList(new CarDTO(1, 6));
        mockMvc.perform(put("/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cars)))
                .andExpect(status().isOk());

        // Then request journey
        JourneyDTO journey = new JourneyDTO(1, 4);
        mockMvc.perform(post("/journey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(journey)))
                .andExpect(status().isOk());
    }

    @Test
    void testPostJourney_DuplicateGroup() throws Exception {
        // Load cars
        List<CarDTO> cars = Arrays.asList(new CarDTO(1, 6));
        mockMvc.perform(put("/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cars)))
                .andExpect(status().isOk());

        // Request journey
        JourneyDTO journey = new JourneyDTO(1, 2);
        mockMvc.perform(post("/journey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(journey)))
                .andExpect(status().isOk());

        // Try to request again with same ID
        mockMvc.perform(post("/journey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(journey)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testLocate_GroupWithCar() throws Exception {
        // Load cars
        List<CarDTO> cars = Arrays.asList(new CarDTO(1, 4));
        mockMvc.perform(put("/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cars)))
                .andExpect(status().isOk());

        // Request journey
        JourneyDTO journey = new JourneyDTO(1, 3);
        mockMvc.perform(post("/journey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(journey)))
                .andExpect(status().isOk());

        // Locate should return the car
        mockMvc.perform(post("/locate")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("ID=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.seats").value(4));
    }

    @Test
    void testLocate_GroupWaiting() throws Exception {
        // Load cars
        List<CarDTO> cars = Arrays.asList(new CarDTO(1, 4));
        mockMvc.perform(put("/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cars)))
                .andExpect(status().isOk());

        // First journey takes all seats
        JourneyDTO journey1 = new JourneyDTO(1, 4);
        mockMvc.perform(post("/journey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(journey1)))
                .andExpect(status().isOk());

        // Second journey should be queued
        JourneyDTO journey2 = new JourneyDTO(2, 2);
        mockMvc.perform(post("/journey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(journey2)))
                .andExpect(status().isOk());

        // Locate should return no content (waiting)
        mockMvc.perform(post("/locate")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("ID=2"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testLocate_GroupNotFound() throws Exception {
        mockMvc.perform(post("/locate")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("ID=999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDropoff_Success() throws Exception {
        // Load cars
        List<CarDTO> cars = Arrays.asList(new CarDTO(1, 6));
        mockMvc.perform(put("/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cars)))
                .andExpect(status().isOk());

        // Request journey
        JourneyDTO journey = new JourneyDTO(1, 4);
        mockMvc.perform(post("/journey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(journey)))
                .andExpect(status().isOk());

        // Dropoff
        mockMvc.perform(post("/dropoff")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("ID=1"))
                .andExpect(status().isOk());

        // Verify group no longer exists
        mockMvc.perform(post("/locate")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("ID=1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDropoff_GroupNotFound() throws Exception {
        mockMvc.perform(post("/dropoff")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("ID=999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCompleteFlowWithReassignment() throws Exception {
        // Load cars
        List<CarDTO> cars = Arrays.asList(
                new CarDTO(1, 6),
                new CarDTO(2, 4));
        mockMvc.perform(put("/cars")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cars)))
                .andExpect(status().isOk());

        // Request first journey (takes car 1)
        JourneyDTO journey1 = new JourneyDTO(1, 6);
        mockMvc.perform(post("/journey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(journey1)))
                .andExpect(status().isOk());

        // Request second journey (takes car 2)
        JourneyDTO journey2 = new JourneyDTO(2, 4);
        mockMvc.perform(post("/journey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(journey2)))
                .andExpect(status().isOk());

        // Request third journey (should be queued)
        JourneyDTO journey3 = new JourneyDTO(3, 2);
        mockMvc.perform(post("/journey")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(journey3)))
                .andExpect(status().isOk());

        // Third journey should be waiting
        mockMvc.perform(post("/locate")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("ID=3"))
                .andExpect(status().isNoContent());

        // Dropoff first journey
        mockMvc.perform(post("/dropoff")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("ID=1"))
                .andExpect(status().isOk());

        // Third journey should now have a car
        mockMvc.perform(post("/locate")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .content("ID=3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }
}
