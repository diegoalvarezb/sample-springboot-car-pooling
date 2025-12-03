package com.cabify.carpooling.mapper;

import com.cabify.carpooling.dto.CarDTO;
import com.cabify.carpooling.model.Car;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper for converting between Car domain entities and CarDTOs.
 */
public final class CarMapper {

    private CarMapper() {
        throw new AssertionError("Utility class cannot be instantiated");
    }

    /**
     * Convert domain entity Car to DTO for API response.
     *
     * @param car The car entity to convert
     * @return CarDTO or null if car is null
     */
    public static CarDTO toDTO(Car car) {
        if (car == null) {
            return null;
        }
        return new CarDTO(car.getId(), car.getSeats());
    }

    /**
     * Convert DTO to domain entity Car.
     *
     * @param carDTO The DTO to convert
     * @return Car entity or null if carDTO is null
     */
    public static Car toEntity(CarDTO carDTO) {
        if (carDTO == null) {
            return null;
        }
        return new Car(carDTO.getId(), carDTO.getSeats());
    }

    /**
     * Convert list of DTOs to list of domain entities.
     *
     * @param carDTOs The list of DTOs to convert
     * @return List of Car entities
     */
    public static List<Car> toEntities(List<CarDTO> carDTOs) {
        if (carDTOs == null) {
            return null;
        }
        return carDTOs.stream()
                .map(CarMapper::toEntity)
                .collect(Collectors.toList());
    }

    /**
     * Convert list of domain entities to list of DTOs.
     *
     * @param cars The list of entities to convert
     * @return List of CarDTOs
     */
    public static List<CarDTO> toDTOs(List<Car> cars) {
        if (cars == null) {
            return null;
        }
        return cars.stream()
                .map(CarMapper::toDTO)
                .collect(Collectors.toList());
    }
}
