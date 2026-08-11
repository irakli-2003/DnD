package com.dnd.model.world.map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class PositionTest {
    @Test
    public void acceptsValidCoordinates() {
        Position pos = new Position(0, 0);
        assertEquals(0, pos.getX());
        assertEquals(0, pos.getY());

        pos = new Position(500, 500);
        assertEquals(500, pos.getX());
        assertEquals(500, pos.getY());

        pos = new Position(999, 999);
        assertEquals(999, pos.getX());
        assertEquals(999, pos.getY());
    }

    @Test
    public void rejectsNegativeX() {
        assertThrows(IllegalArgumentException.class, () -> new Position(-1, 0));
    }

    @Test
    public void rejectsNegativeY() {
        assertThrows(IllegalArgumentException.class, () -> new Position(0, -1));
    }

    @Test
    public void rejectsXExceedingMaximum() {
        assertThrows(IllegalArgumentException.class, () -> new Position(1000, 0));
    }

    @Test
    public void rejectsYExceedingMaximum() {
        assertThrows(IllegalArgumentException.class, () -> new Position(0, 1000));
    }

    @Test
    public void setXValidatesCoordinate() {
        Position pos = new Position(0, 0);
        assertThrows(IllegalArgumentException.class, () -> pos.setX(-1));
        assertThrows(IllegalArgumentException.class, () -> pos.setX(1000));
    }

    @Test
    public void setYValidatesCoordinate() {
        Position pos = new Position(0, 0);
        assertThrows(IllegalArgumentException.class, () -> pos.setY(-1));
        assertThrows(IllegalArgumentException.class, () -> pos.setY(1000));
    }
}
