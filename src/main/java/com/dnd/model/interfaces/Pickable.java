package com.dnd.model.interfaces;

/**
 * Marks an {@link com.dnd.model.item.Item} that can be picked up from the map
 * by an actor. Items that are NOT {@code Pickable} (e.g. fixed furniture,
 * shrines, trapped chests) stay on the grid permanently and cannot be placed
 * in an actor's inventory.
 */
public interface Pickable {
    /**
     * @return {@code true} if this item can currently be picked up by an actor
     *         standing on the same cell.
     */
    boolean isPickable();
}

