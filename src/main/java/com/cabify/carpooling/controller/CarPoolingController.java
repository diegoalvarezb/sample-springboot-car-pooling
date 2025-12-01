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

import com.cabify.carpooling.exception.ExistingGroupException;
import com.cabify.carpooling.exception.GroupNotFoundException;
import com.cabify.carpooling.exception.InvalidPayloadException;
import com.cabify.carpooling.model.Car;
import com.cabify.carpooling.model.Journey;
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

    public CarPoolingController(CarService carService,
                               GroupService groupService,
                               JourneyService journeyService) {
        this.carService = carService;
        this.groupService = groupService;
        this.journeyService = journeyService;
    }

    /**
     * GET /status
     * Health check endpoint.
     */
    @GetMapping("/status")
    public ResponseEntity<Void> getStatus() {
        return ResponseEntity.ok().build();
    }

    /**
     * PUT /cars
     * Load the list of available cars and reset application state.
     */
    @PutMapping(value = "/cars", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> putCars(@RequestBody List<Car> cars) {
        try {
            if (cars == null) {
                throw new InvalidPayloadException("Cars list cannot be null");
            }

            int carCount = cars.size();
            if (carCount == 0) {
                throw new InvalidPayloadException("Cars list cannot be empty");
            }

            validateCars(cars);
            carService.load(cars);
            return ResponseEntity.ok().build();
        } catch (InvalidPayloadException e) {
            org.slf4j.LoggerFactory.getLogger(CarPoolingController.class)
                .error("Invalid payload in PUT /cars: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * POST /journey
     * Register a group requesting a journey.
     */
    @PostMapping(value = "/journey", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> postJourney(@RequestBody Journey journey) {
        try {
            validateJourney(journey);

            int groupId = journey.getId();
            int people = journey.getPeople();

            // Check if group already exists
            if (groupService.getPeople(groupId) != null) {
                throw new ExistingGroupException();
            }

            // Register group and request journey
            groupService.add(groupId, people);
            journeyService.request(groupId, people);

            return ResponseEntity.ok().build();
        } catch (InvalidPayloadException e) {
            return ResponseEntity.badRequest().build();
        } catch (ExistingGroupException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * POST /dropoff
     * Unregister a group (whether they traveled or not).
     */
    @PostMapping(value = "/dropoff", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<Void> postDropoff(@RequestParam(value = "ID", required = false) Integer groupId) {
        try {
            if (groupId == null) {
                return ResponseEntity.badRequest().build();
            }

            // Check if group exists
            Integer people = groupService.getPeople(groupId);
            if (people == null) {
                throw new GroupNotFoundException();
            }

            // Check if group has traveled
            Integer carId = journeyService.getCar(groupId);
            if (carId != null) {
                journeyService.remove(groupId);
                journeyService.updateCarAllocation(carId, people);
            }

            // Remove group
            groupService.remove(groupId);

            return ResponseEntity.ok().build();
        } catch (GroupNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /locate
     * Get the car assigned to a group.
     */
    @PostMapping(value = "/locate",
                 consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Car> postLocate(@RequestParam(value = "ID", required = false) Integer groupId) {
        try {
            if (groupId == null) {
                return ResponseEntity.badRequest().build();
            }

            // Check if group exists
            if (groupService.getPeople(groupId) == null) {
                throw new GroupNotFoundException();
            }

            // Check if group is still waiting
            Integer carId = journeyService.getCar(groupId);
            if (carId == null) {
                return ResponseEntity.noContent().build();
            }

            // Get the car the group is traveling in
            Car car = carService.get(carId);
            return ResponseEntity.ok(car);
        } catch (GroupNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Validate the list of cars in PUT /cars request.
     */
    private void validateCars(List<Car> cars) {
        int index = 0;
        for (Car car : cars) {
            if (car == null) {
                throw new InvalidPayloadException(String.format("Car at index %d is null", index));
            }
            if (car.getId() <= 0) {
                throw new InvalidPayloadException(String.format("Car at index %d has invalid id: %d (must be positive)", index, car.getId()));
            }
            if (car.getSeats() < 4 || car.getSeats() > 6) {
                throw new InvalidPayloadException(String.format("Car at index %d (id=%d) has invalid seats: %d (must be between 4 and 6)",
                    index, car.getId(), car.getSeats()));
            }
            index++;
        }
    }

    /**
     * Validate the journey payload in POST /journey request.
     */
    private void validateJourney(Journey journey) {
        if (journey == null || journey.getId() <= 0 || journey.getPeople() < 1 || journey.getPeople() > 6) {
            throw new InvalidPayloadException("Invalid journey: id must be positive, people must be between 1 and 6");
        }
    }
}
