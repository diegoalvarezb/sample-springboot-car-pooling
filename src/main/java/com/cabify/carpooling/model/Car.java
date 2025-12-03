package com.cabify.carpooling.model;

/**
 * Represents a car with a certain number of seats.
 */
public class Car {

    private int id;
    private int seats;
    private int availableSeats;

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
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public void setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
    }
}
