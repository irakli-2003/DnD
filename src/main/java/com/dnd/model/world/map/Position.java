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
    private int x;
    private int y;

    public Position() {
    }

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
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

