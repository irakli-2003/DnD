package com.dnd.data.dto;

import com.dnd.model.item.Item;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;

@JsonDeserialize(using = ItemCatalogDeserializer.class)
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

