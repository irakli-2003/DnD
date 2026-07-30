package com.dnd.data.dto;

import com.dnd.model.item.Item;
import com.dnd.model.item.books.Book;
import com.dnd.model.item.armors.BodyArmor;
import com.dnd.model.item.weapons.physical_weapons.MeleeWeapon;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ItemCatalogDeserializer extends JsonDeserializer<ItemCatalog> {
    @Override
    public ItemCatalog deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
        ObjectMapper mapper = (ObjectMapper) jp.getCodec();
        JsonNode root = mapper.readTree(jp);
        
        ItemCatalog catalog = new ItemCatalog();
        List<Item> items = new ArrayList<>();
        
        if (root.has("items") && root.get("items").isArray()) {
            for (JsonNode itemNode : root.get("items")) {
                String type = itemNode.has("type") ? itemNode.get("type").asText().toLowerCase() : "";
                Item item = null;
                
                try {
                    switch (type) {
                        case "book":
                            item = mapper.treeToValue(itemNode, Book.class);
                            break;
                        case "armor":
                            item = mapper.treeToValue(itemNode, BodyArmor.class);
                            break;
                        case "weapon":
                            item = mapper.treeToValue(itemNode, MeleeWeapon.class);
                            break;
                        case "alchemy":
                            item = mapper.treeToValue(itemNode, com.dnd.model.item.alchemy.Potion.class);
                            break;
                        default:
                            item = mapper.treeToValue(itemNode, Book.class);
                    }
                    if (item != null) {
                        items.add(item);
                    }
                } catch (Exception e) {
                    // Skip problematic items and log
                    System.err.println("Failed to deserialize item: " + e.getMessage());
                }
            }
        }
        
        catalog.setItems(items);
        return catalog;
    }
}
