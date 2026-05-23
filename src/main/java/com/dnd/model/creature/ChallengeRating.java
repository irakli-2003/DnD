package com.dnd.model.creature;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum ChallengeRating {
    CR_0("0"),
    CR_1_8("1/8"),
    CR_1_4("1/4"),
    CR_1_2("1/2"),
    CR_1("1"),
    CR_2("2"),
    CR_3("3"),
    CR_4("4"),
    CR_5("5"),
    CR_6("6"),
    CR_7("7"),
    CR_8("8"),
    CR_9("9"),
    CR_10("10"),
    CR_11("11"),
    CR_12("12"),
    CR_13("13"),
    CR_14("14"),
    CR_15("15"),
    CR_16("16"),
    CR_17("17"),
    CR_18("18"),
    CR_19("19"),
    CR_20("20"),
    CR_21("21"),
    CR_22("22"),
    CR_23("23"),
    CR_24("24"),
    CR_25("25"),
    CR_26("26"),
    CR_27("27"),
    CR_28("28"),
    CR_29("29"),
    CR_30("30");

    private final String value;

    ChallengeRating(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static ChallengeRating fromJson(Object value) {
        if (value == null) {
            return null;
        }
        String normalized = (value instanceof Number)
            ? value.toString()
            : value.toString().trim();
        for (ChallengeRating rating : values()) {
            if (rating.value.equals(normalized)) {
                return rating;
            }
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        try {
            return ChallengeRating.valueOf(upper);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Unknown challenge rating: " + value, ex);
        }
    }
}


