package com.cabify.carpooling.model;

/**
 * Represents a group of people requesting a journey.
 */
public class Group {
    private int id;
    private int people;

    public Group() {
    }

    public Group(int id, int people) {
        this.id = id;
        this.people = people;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPeople() {
        return people;
    }

    public void setPeople(int people) {
        this.people = people;
    }
}
