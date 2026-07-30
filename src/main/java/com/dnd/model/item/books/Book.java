package com.dnd.model.item.books;

import com.dnd.model.interfaces.Printable;
import com.dnd.model.item.Item;

public class Book extends Item implements Printable {
    private String overview;

    public Book() {
        super();
    }

    public Book(String id, String name, String type, String description, int valueGold, double weight,
                ItemDamage damage, ItemDurability durability, String overview) {
        super(id, name, type, description, valueGold, weight, damage, durability);
        this.overview = overview;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    @Override
    public String toString() {
        return getName() != null ? getName() : getId();
    }
}

