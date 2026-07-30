package com.dnd.model.world;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Dice {
    private String id;
    private String name;
    private int sides;

    public Dice() {
    }

    public Dice(String id, String name, int sides) {
        this.id = id;
        this.name = name;
        this.sides = sides;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Dice fromValue(Object value) {
        if (value instanceof Integer) {
            int sides = (Integer) value;
            String id = "d" + sides;
            return new Dice(id, id, sides);
        }
        return new Dice();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSides() {
        return sides;
    }

    public void setSides(int sides) {
        this.sides = sides;
    }

    @Override
    public String toString() {
        if (name != null && !name.isEmpty()) {
            return name;
        }
        if (sides > 0) {
            return "d" + sides;
        }
        return id == null ? "" : id;
    }
}

