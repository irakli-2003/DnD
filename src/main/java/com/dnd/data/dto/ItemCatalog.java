package com.dnd.data.dto;

import com.dnd.model.item.Item;

import java.util.List;

public class ItemCatalog {
    private List<Item> items;

    public ItemCatalog() {
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }
}

