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

    @Test
    public void newCharacterStartsAtTheMinimumValidLevel() {
        assertEquals("a brand-new character must never sit at an invalid level",
            PlayerCharacter.MIN_LEVEL, new PlayerCharacter().getLevel());
    }

    @Test
    public void xpDefaultsToZeroAndAccumulates() {
        PlayerCharacter character = new PlayerCharacter();
        assertEquals(0, character.getXp());
        character.addXp(150);
        assertEquals(150, character.getXp());
        character.addXp(50);
        assertEquals(200, character.getXp());
    }

    @Test
    public void xpNeverGoesNegative() {
        PlayerCharacter character = new PlayerCharacter();
        character.addXp(100);
        character.addXp(-1000);
        assertEquals(0, character.getXp());
    }

    @Test
    public void rejectsNegativeXpSetDirectly() {
        PlayerCharacter character = new PlayerCharacter();
        assertThrows(IllegalArgumentException.class, () -> character.setXp(-1));
    }

    @Test
    public void hitPointsClampToZeroAndMax() {
        PlayerCharacter character = new PlayerCharacter();
        character.setMaxHitPoints(30);
        character.setCurrentHitPoints(30);
        character.applyDamage(50);
        assertEquals("damage cannot drop hit points below zero", 0, character.getCurrentHitPoints());
        character.heal(1000);
        assertEquals("healing cannot exceed the current max", 30, character.getCurrentHitPoints());
    }

    @Test
    public void loweringMaxHitPointsPullsCurrentDown() {
        PlayerCharacter character = new PlayerCharacter();
        character.setMaxHitPoints(30);
        character.setCurrentHitPoints(30);
        character.setMaxHitPoints(10);
        assertEquals("current hit points must never exceed a newly lowered max",
            10, character.getCurrentHitPoints());
    }

    @Test
    public void manaClampsToZeroAndMax() {
        PlayerCharacter character = new PlayerCharacter();
        character.setMaxMana(20);
        character.setCurrentMana(-5);
        assertEquals(0, character.getCurrentMana());
        character.setCurrentMana(999);
        assertEquals(20, character.getCurrentMana());
    }

    @Test
    public void activeEffectsCanBeAddedAndCleared() {
        PlayerCharacter character = new PlayerCharacter();
        assertEquals(0, character.getActiveEffects().size());
        com.dnd.model.world.map.ActiveEffect frost =
            new com.dnd.model.world.map.ActiveEffect("frost", "Frost", 3, 2, 0, "DM");
        character.addEffect(frost);
        assertEquals(1, character.getActiveEffects().size());
        character.clearEffect(frost);
        assertEquals("clearing an effect must remove it once it's worn off",
            0, character.getActiveEffects().size());
    }
}

