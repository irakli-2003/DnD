package com.dnd.model.character;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class PlayerCharacterTest {
    @Test
    public void acceptsLevelWithinValidRange() {
        PlayerCharacter character = new PlayerCharacter();
        character.setLevel(1);
        assertEquals(1, character.getLevel());
        character.setLevel(30);
        assertEquals(30, character.getLevel());
    }

    @Test
    public void rejectsLevelBelowMinimum() {
        PlayerCharacter character = new PlayerCharacter();
        assertThrows(IllegalArgumentException.class, () -> character.setLevel(0));
    }

    @Test
    public void rejectsLevelAboveMaximum() {
        PlayerCharacter character = new PlayerCharacter();
        assertThrows(IllegalArgumentException.class, () -> character.setLevel(31));
    }

    @Test
    public void constructorValidatesLevel() {
        assertThrows(IllegalArgumentException.class,
            () -> new PlayerCharacter("id", "name", "class", "race", 99, null, null, null));
    }
}

