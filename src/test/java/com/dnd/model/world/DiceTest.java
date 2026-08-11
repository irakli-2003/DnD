package com.dnd.model.world;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Regression test for a bug where Dice's DELEGATING JsonCreator (originally
 * typed as {@code Object}) intercepted full JSON objects (not just the bare
 * integer shorthand), causing Jackson to pass in a generic Map that never
 * matched the Integer check - silently producing a blank Dice with a null
 * name/id. This is exactly what caused every entry in the "dice" catalog to
 * render as "(unnamed)" in the CLI.
 */
public class DiceTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    public void deserializesFullObjectShape() throws Exception {
        Dice dice = mapper.readValue("{\"id\":\"d10\",\"name\":\"d10\",\"sides\":10}", Dice.class);
        assertEquals("d10", dice.getId());
        assertEquals("d10", dice.getName());
        assertEquals(10, dice.getSides());
    }

    @Test
    public void deserializesBareIntegerShorthand() throws Exception {
        Dice dice = mapper.readValue("6", Dice.class);
        assertEquals("d6", dice.getId());
        assertEquals("d6", dice.getName());
        assertEquals(6, dice.getSides());
    }

    @Test
    public void acceptsPositiveSides() {
        Dice dice = new Dice("d20", "d20", 1);
        assertEquals(1, dice.getSides());
        dice.setSides(20);
        assertEquals(20, dice.getSides());
    }

    @Test
    public void rejectsZeroSides() {
        Dice dice = new Dice("test", "test", 1);
        assertThrows(IllegalArgumentException.class, () -> dice.setSides(0));
    }

    @Test
    public void rejectsNegativeSides() {
        assertThrows(IllegalArgumentException.class, () -> new Dice("test", "test", -5));
    }

    @Test
    public void constructorValidatesSides() {
        assertThrows(IllegalArgumentException.class, () -> new Dice("d0", "d0", 0));
    }
}


