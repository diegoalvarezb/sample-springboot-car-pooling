package com.cabify.carpooling.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cabify.carpooling.dto.CarDTO;
import com.cabify.carpooling.dto.JourneyDTO;
import com.cabify.carpooling.exception.ExistingGroupException;
import com.cabify.carpooling.exception.GroupNotFoundException;
import com.cabify.carpooling.exception.InvalidPayloadException;
import com.cabify.carpooling.mapper.CarMapper;
import com.cabify.carpooling.model.Car;
import com.cabify.carpooling.service.CarService;
import com.cabify.carpooling.service.GroupService;
import com.cabify.carpooling.service.JourneyService;

/**
 * Main controller for the Car Pooling service.
 */
@RestController
public class CarPoolingController {

    private final CarService carService;
    private final GroupService groupService;
    private final JourneyService journeyService;

    // Global lock to ensure atomicity across multi-step operations
    private final Object lock = new Object();

    public CarPoolingController(CarService carService, GroupService groupService,
            JourneyService journeyService) {
        this.carService = carService;
        this.groupService = groupService;
        this.journeyService = journeyService;
    }

    /**
     * GET /status Health check endpoint.
     */
    @GetMapping("/status")
    public ResponseEntity<Void> getStatus() {
        return ResponseEntity.ok().build();
    }

    /**
     * PUT /cars Load the list of available cars and reset application state.
     */
    @PutMapping(value = "/cars", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> putCars(@RequestBody List<CarDTO> carDTOs) {
        if (carDTOs == null) {
            throw new InvalidPayloadException("Cars list cannot be null");
        }

        if (carDTOs.isEmpty()) {
            throw new InvalidPayloadException("Cars list cannot be empty");
        }

        validateCars(carDTOs);

        List<Car> cars = CarMapper.toEntities(carDTOs);
        carService.load(cars);

        return ResponseEntity.ok().build();
    }

    /**
     * POST /journey Register a group requesting a journey.
     */
    @PostMapping(value = "/journey", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> postJourney(@RequestBody JourneyDTO journeyDTO) {
        validateJourney(journeyDTO);

        int groupId = journeyDTO.getId();
        int people = journeyDTO.getPeople();

        synchronized (lock) {
            // Check if group already exists
            if (groupService.getPeople(groupId) != null) {
                throw new ExistingGroupException();
            }

            // Register group and request journey
            groupService.add(groupId, people);
            journeyService.request(groupId, people);
        }

        return ResponseEntity.ok().build();
    }

    /**
     * POST /dropoff Unregister a group (whether they traveled or not).
     */
    @PostMapping(value = "/dropoff", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> postDropoff(
            @RequestParam(value = "ID", required = false) Integer groupId) {
        if (groupId == null) {
            return ResponseEntity.badRequest().build();
        }

        synchronized (lock) {
            // Check if group exists
            Integer people = groupService.getPeople(groupId);
            if (people == null) {
                throw new GroupNotFoundException();
            }

            // Execute dropoff
            journeyService.dropoff(groupId, people);
        }

        return ResponseEntity.ok().build();
    }

    /**
     * POST /locate Get the car assigned to a group.
     */
    @PostMapping(value = "/locate", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CarDTO> postLocate(
            @RequestParam(value = "ID", required = false) Integer groupId) {
        if (groupId == null) {
            return ResponseEntity.badRequest().build();
        }

        synchronized (lock) {
            // Check if group exists
            if (groupService.getPeople(groupId) == null) {
                throw new GroupNotFoundException();
            }

            // Check if group is still waiting
            Integer carId = journeyService.getCar(groupId);
            if (carId == null) {
                return ResponseEntity.noContent().build();
            }

            // Get the car the group is traveling in and convert to DTO
            Car car = carService.get(carId);
            CarDTO carDTO = CarMapper.toDTO(car);

            return ResponseEntity.ok(carDTO);
        }
    }

    /**
     * Validate the list of cars in PUT /cars request.
     */
    private void validateCars(List<CarDTO> carDTOs) {
        int index = 0;

        for (CarDTO carDTO : carDTOs) {
            if (carDTO == null) {
                throw new InvalidPayloadException(String.format("Car at index %d is null", index));
            }

            if (carDTO.getId() <= 0) {
                throw new InvalidPayloadException(
                        String.format("Car at index %d has invalid id: %d (must be positive)",
                                index, carDTO.getId()));
            }

            if (carDTO.getSeats() < 1 || carDTO.getSeats() > 6) {
                throw new InvalidPayloadException(String.format(
                        "Car at index %d (id=%d) has invalid seats: %d (must be between 4 and 6)",
                        index, carDTO.getId(), carDTO.getSeats()));
            }

            index++;
        }
    }

    /**
     * Validate the journey payload in POST /journey request.
     */
    private void validateJourney(JourneyDTO journeyDTO) {
        if (journeyDTO == null || journeyDTO.getId() <= 0 || journeyDTO.getPeople() < 1
                || journeyDTO.getPeople() > 6) {
            throw new InvalidPayloadException(
                    "Invalid journey: id must be positive, people must be between 1 and 6");
        }
    }
}
