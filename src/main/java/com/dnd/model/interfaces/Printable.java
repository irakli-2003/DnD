package com.dnd.model.interfaces;

public interface Printable {
    @Override
    String toString();

    /**
     * Prints this object's textual representation to the console. Provided
     * as a default so every existing {@link Printable} implementation gets a
     * print method for free; override if a richer/multi-line representation
     * is needed (see {@code GameMap}, which prints its whole grid).
     */
    default void print() {
        System.out.println(toString());
    }
}
