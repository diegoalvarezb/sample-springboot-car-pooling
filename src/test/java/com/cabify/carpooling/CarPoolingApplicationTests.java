package com.cabify.carpooling;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cabify.carpooling.dto.CarDTO;
import com.cabify.carpooling.dto.JourneyDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class CarPoolingApplicationTests {
	private ObjectMapper objectMapper = new ObjectMapper();

	@Test
	public void itShouldHaveOkStatus(@Autowired MockMvc mvc) throws Exception {
		mvc.perform(get("/status")).andExpect(status().isOk());
	}

	@Test
	public void itShouldManageJourney(@Autowired MockMvc mvc) throws Exception {
		var cars = List.of(
				new CarDTO(1, 4),
				new CarDTO(2, 6));
		var journey = new JourneyDTO(1, 4);

		mvc.perform(put("/cars").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(cars)))
				.andExpect(status().isOk());

		mvc.perform(post("/journey").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(journey)))
				.andExpect(status().isOk());

		mvc.perform(post("/locate").contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.content("ID=" + journey.getId()))
				.andExpect(status().isOk())
				.andExpect(content().json("{\"id\":1,\"seats\":4}"));

		mvc.perform(post("/dropoff").contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.content("ID=" + journey.getId()))
				.andExpect(status().isOk());
	}

	@Test
	public void itShouldQueueGroupWhenNoCarAvailable(@Autowired MockMvc mvc) throws Exception {
		var cars = List.of(new CarDTO(1, 4));
		var journey1 = new JourneyDTO(1, 4);
		var journey2 = new JourneyDTO(2, 2);

		mvc.perform(put("/cars").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(cars)))
				.andExpect(status().isOk());

		// First journey takes all seats
		mvc.perform(post("/journey").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(journey1)))
				.andExpect(status().isOk());

		// Second journey should be queued
		mvc.perform(post("/journey").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(journey2)))
				.andExpect(status().isOk());

		// Second journey should be waiting
		mvc.perform(post("/locate").contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.content("ID=" + journey2.getId()))
				.andExpect(status().isNoContent());

		// Drop off first journey
		mvc.perform(post("/dropoff").contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.content("ID=" + journey1.getId()))
				.andExpect(status().isOk());

		// Second journey should now have a car
		mvc.perform(post("/locate").contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.content("ID=" + journey2.getId()))
				.andExpect(status().isOk())
				.andExpect(content().json("{\"id\":1,\"seats\":4}"));
	}

	@Test
	public void itShouldReturn404ForNonExistentGroup(@Autowired MockMvc mvc) throws Exception {
		mvc.perform(post("/locate").contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.content("ID=999"))
				.andExpect(status().isNotFound());

		mvc.perform(post("/dropoff").contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.content("ID=999"))
				.andExpect(status().isNotFound());
	}

	@Test
	public void itShouldReturn400ForDuplicateGroup(@Autowired MockMvc mvc) throws Exception {
		var cars = List.of(new CarDTO(1, 6));
		var journey = new JourneyDTO(1, 2);

		mvc.perform(put("/cars").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(cars)))
				.andExpect(status().isOk());

		mvc.perform(post("/journey").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(journey)))
				.andExpect(status().isOk());

		// Try to add the same group again
		mvc.perform(post("/journey").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(journey)))
				.andExpect(status().isBadRequest());
	}
}
