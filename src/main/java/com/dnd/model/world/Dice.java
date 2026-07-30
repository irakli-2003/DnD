package com.dnd.model.world;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;

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

    /**
     * Supports two JSON shapes for convenience/backwards-compatibility:
     * a bare integer shorthand (e.g. {@code 10} -> d10), and the normal
     * full object shape ({@code {"id": "d10", "name": "d10", "sides": 10}}).
     *
     * <p>This must inspect the raw {@link JsonNode} rather than delegating to
     * a plain {@code Object} parameter: with an {@code Object}-typed delegate,
     * Jackson deserializes JSON objects into a generic {@code Map} before
     * calling this method, which would never match the {@code Integer} check
     * below and would silently fall through to a blank {@code Dice} - which
     * is exactly the bug that caused every dice entry to show up unnamed.</p>
     */
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static Dice fromValue(JsonNode node) {
        if (node == null || node.isNull()) {
            return new Dice();
        }
        if (node.isIntegralNumber()) {
            int sides = node.asInt();
            String id = "d" + sides;
            return new Dice(id, id, sides);
        }
        if (node.isObject()) {
            String id = node.hasNonNull("id") ? node.get("id").asText() : null;
            String name = node.hasNonNull("name") ? node.get("name").asText() : null;
            int sides = node.hasNonNull("sides") ? node.get("sides").asInt() : 0;
            return new Dice(id, name, sides);
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

