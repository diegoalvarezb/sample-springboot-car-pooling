package com.cabify.carpooling.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data Transfer Object for Journey API requests.
 * Represents a journey request from POST /journey endpoint.
 */
public final class JourneyDTO {

    @JsonProperty("id")
    private int id;

    @JsonProperty("people")
    private int people;

    public JourneyDTO() {
    }

    public JourneyDTO(int id, int people) {
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
