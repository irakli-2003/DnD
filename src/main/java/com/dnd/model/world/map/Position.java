package com.dnd.model.world.map;

import java.util.Objects;

/**
 * A coordinate on a {@link GameMap}'s grid. The map uses a simple integer
 * coordinate system with the origin {@code (0, 0)} at the top-left cell,
 * {@code x} increasing to the right and {@code y} increasing downward
 * (i.e. row {@code y}, column {@code x}) - matching the order rows/cells
 * are printed in by {@link GameMap#print()}.
 */
public class Position {
    public static final int MIN_COORDINATE = 0;
    public static final int MAX_COORDINATE = 999;

    private int x;
    private int y;

    public Position() {
    }

    public Position(int x, int y) {
        setX(x);
        setY(y);
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        if (x < MIN_COORDINATE || x > MAX_COORDINATE) {
            throw new IllegalArgumentException("Position X must be between " + MIN_COORDINATE + " and " + MAX_COORDINATE + " but was " + x);
        }
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        if (y < MIN_COORDINATE || y > MAX_COORDINATE) {
            throw new IllegalArgumentException("Position Y must be between " + MIN_COORDINATE + " and " + MAX_COORDINATE + " but was " + y);
        }
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Position)) {
            return false;
        }
        Position other = (Position) o;
        return x == other.x && y == other.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}

