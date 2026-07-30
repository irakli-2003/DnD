package com.dnd.model.character.stats;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class CoreStatsTest {
    @Test
    public void acceptsScoresWithinValidRange() {
        CoreStats stats = new CoreStats();
        stats.setStrength(1);
        stats.setDexterity(30);
        assertEquals(1, stats.getStrength());
        assertEquals(30, stats.getDexterity());
    }

    @Test
    public void rejectsScoreBelowMinimum() {
        CoreStats stats = new CoreStats();
        assertThrows(IllegalArgumentException.class, () -> stats.setStrength(0));
    }

    @Test
    public void rejectsScoreAboveMaximum() {
        CoreStats stats = new CoreStats();
        assertThrows(IllegalArgumentException.class, () -> stats.setCharisma(31));
    }

    @Test
    public void constructorValidatesAllScores() {
        assertThrows(IllegalArgumentException.class, () -> new CoreStats(15, 12, 0, 10, 11, 8));
    }
}

