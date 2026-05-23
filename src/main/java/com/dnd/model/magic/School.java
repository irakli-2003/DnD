package com.dnd.model.magic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum School {
    WITCHER_SIGNS("witcher signs"),
    ELEMENTAL("elemental magic"),
    DIVINE("divine  magic"),
    DARK("dark  magic"),
    NATURE("nature-based magic"),
    ILLUSION("illusion"),
    NECROMANCY("necromancy"),
    TRANSMUTATION("transmutation"),
    TELEPORTATION("teleportation");

    private final String value;

    School(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static School fromJson(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (School school : values()) {
            if (school.value.equals(normalized)) {
                return school;
            }
        }
        throw new IllegalArgumentException("Unknown school: " + value);
    }
}

