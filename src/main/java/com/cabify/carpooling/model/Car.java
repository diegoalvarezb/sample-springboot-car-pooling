package com.cabify.carpooling.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a car with a certain number of seats.
 */
public class Car {
    @JsonProperty("id")
    private int id;
    
    @JsonProperty("seats")
    private int seats;
    
    @JsonIgnore
    private int availableSeats;

    public Car() {
    }

    public Car(int id, int seats) {
        this.id = id;
        this.seats = seats;
        this.availableSeats = seats;
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
        this.availableSeats = seats;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }
}
