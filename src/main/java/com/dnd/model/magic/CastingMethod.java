package com.dnd.model.magic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Locale;

public enum CastingMethod {
    SPEECH("speech"),
    THOUGHT("thought"),
    HAND_MOVEMENT("hand-movement"),
    BODY_MOVEMENT("body-movement"),
    RITUAL("ritual");

    private final String value;

    CastingMethod(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static CastingMethod fromJson(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if ("speach".equals(normalized)) {
            normalized = "speech";
        } else if ("though".equals(normalized)) {
            normalized = "thought";
        }
        for (CastingMethod method : values()) {
            if (method.value.equals(normalized)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unknown casting method: " + value);
    }
}

