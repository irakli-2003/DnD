package com.dnd.model.creature;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum Habitat {
    FOREST("forest"),
    PLAINS("plains"),
    DESERT("desert"),
    MOUNTAIN("mountain"),
    SWAMP("swamp"),
    UNDERDARK("underdark"),
    COAST("coast"),
    OCEAN("ocean"),
    ARCTIC("arctic"),
    URBAN("urban"),
    HILLS("hills"),
    GRASSLAND("grassland");

    private final String value;

    Habitat(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static Habitat fromJson(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (Habitat habitat : values()) {
            if (habitat.value.equals(normalized)) {
                return habitat;
            }
        }
        throw new IllegalArgumentException("Unknown habitat: " + value);
    }
}


