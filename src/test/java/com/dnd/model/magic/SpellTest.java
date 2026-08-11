package com.dnd.model.magic;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class SpellTest {
    @Test
    public void acceptsLevelWithinValidRange() {
        Spell spell = new Spell();
        spell.setLevel(0);
        assertEquals(0, spell.getLevel());
        spell.setLevel(9);
        assertEquals(9, spell.getLevel());
        spell.setLevel(5);
        assertEquals(5, spell.getLevel());
    }

    @Test
    public void rejectsLevelBelowMinimum() {
        Spell spell = new Spell();
        assertThrows(IllegalArgumentException.class, () -> spell.setLevel(-1));
    }

    @Test
    public void rejectsLevelAboveMaximum() {
        Spell spell = new Spell();
        assertThrows(IllegalArgumentException.class, () -> spell.setLevel(10));
    }

    @Test
    public void acceptsNonNegativeManaCost() {
        Spell spell = new Spell();
        spell.setManaCost(0);
        assertEquals(0, spell.getManaCost());
        spell.setManaCost(100);
        assertEquals(100, spell.getManaCost());
    }

    @Test
    public void rejectsNegativeManaCost() {
        Spell spell = new Spell();
        assertThrows(IllegalArgumentException.class, () -> spell.setManaCost(-1));
    }

    @Test
    public void acceptsNonNegativeRange() {
        Spell spell = new Spell();
        spell.setRange(0);
        assertEquals(0, spell.getRange());
        spell.setRange(300);
        assertEquals(300, spell.getRange());
    }

    @Test
    public void rejectsNegativeRange() {
        Spell spell = new Spell();
        assertThrows(IllegalArgumentException.class, () -> spell.setRange(-1));
    }

    @Test
    public void acceptsNonNegativeRadius() {
        Spell spell = new Spell();
        spell.setRadius(0);
        assertEquals(0, spell.getRadius());
        spell.setRadius(50);
        assertEquals(50, spell.getRadius());
    }

    @Test
    public void rejectsNegativeRadius() {
        Spell spell = new Spell();
        assertThrows(IllegalArgumentException.class, () -> spell.setRadius(-1));
    }

    @Test
    public void constructorValidatesLevel() {
        assertThrows(IllegalArgumentException.class,
            () -> new Spell("id", "Test Spell", "desc", 10, null, 0, 0, 0, null, null, null, null, null, null));
    }

    @Test
    public void constructorValidatesManaCost() {
        assertThrows(IllegalArgumentException.class,
            () -> new Spell("id", "Test Spell", "desc", 5, null, -1, 0, 0, null, null, null, null, null, null));
    }

    @Test
    public void constructorValidatesRange() {
        assertThrows(IllegalArgumentException.class,
            () -> new Spell("id", "Test Spell", "desc", 5, null, 0, -1, 0, null, null, null, null, null, null));
    }

    @Test
    public void constructorValidatesRadius() {
        assertThrows(IllegalArgumentException.class,
            () -> new Spell("id", "Test Spell", "desc", 5, null, 0, 0, -1, null, null, null, null, null, null));
    }
}
