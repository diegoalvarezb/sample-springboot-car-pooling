package com.cabify.carpooling.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object for Car API requests and responses.
 * Contains only the data exposed in the API (id and seats).
 */
public final class CarDTO {

    @JsonProperty("id")
    private int id;

    @JsonProperty("seats")
    private int seats;

    public CarDTO() {
    }

    public CarDTO(int id, int seats) {
        this.id = id;
        this.seats = seats;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSeats() {
        return seats;
    }

    public void setSeats(int seats) {
        this.seats = seats;
    }
}
