package com.dnd.model.world.map;

import com.dnd.model.interfaces.Pickable;
import com.dnd.model.item.Item;

/**
 * A concrete {@link Item} lying on the map grid.
 *
 * <p>An item token is pickable when the underlying {@link Item} implements
 * {@link Pickable} and {@link Pickable#isPickable()} returns {@code true}
 * (which is the default for all items).  Set {@code item.setPickable(false)}
 * on shrines, doors, or quest-locked objects to prevent actors from picking
 * them up.</p>
 */
public class MapItemToken implements MapObject {

    /** Discriminator for {@link MapObject} polymorphic (de)serialization. */
    private String kind = "item";
    private Item item;
    private Position position;

    public MapItemToken() {
    }

    public MapItemToken(Item item) {
        this.item = item;
    }

    public MapItemToken(Item item, Position position) {
        this.item = item;
        this.position = position;
    }

    // ── Pickable check ──────────────────────────────────────────────────────

    /**
     * @return {@code true} when the underlying item is an instance of
     *         {@link Pickable} AND its {@link Pickable#isPickable()} is
     *         {@code true}.
     */
    public boolean isPickable() {
        return item instanceof Pickable && ((Pickable) item).isPickable();
    }

    // ── Accessors ───────────────────────────────────────────────────────────

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    @Override
    public Position getPosition() {
        return position;
    }

    @Override
    public void setPosition(Position position) {
        this.position = position;
    }

    // ── MapObject / Printable ───────────────────────────────────────────────

    @Override
    public String getSymbol() {
        return "i";
    }

    @Override
    public String toString() {
        return (item != null ? item.toString() : "item")
               + " [i]"
               + (position != null ? " @ " + position : "");
    }
}

