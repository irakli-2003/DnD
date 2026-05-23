package com.dnd.model.item;

public class Item {
    private String id;
    private String name;
    private String type;
    private String description;
    private int valueGold;
    private double weight;
    private ItemDamage damage;
    private ItemDurability durability;

    public Item() {
    }

    public Item(String id, String name, String type, String description, int valueGold, double weight, ItemDamage damage, ItemDurability durability) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.description = description;
        this.valueGold = valueGold;
        this.weight = weight;
        this.damage = damage;
        this.durability = durability;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getValueGold() {
        return valueGold;
    }

    public void setValueGold(int valueGold) {
        this.valueGold = valueGold;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public ItemDamage getDamage() {
        return damage;
    }

    public void setDamage(ItemDamage damage) {
        this.damage = damage;
    }

    public ItemDurability getDurability() {
        return durability;
    }

    public void setDurability(ItemDurability durability) {
        this.durability = durability;
    }

    public static class ItemDamage {
        private String dice;
        private String type;

        public ItemDamage() {
        }

        public ItemDamage(String dice, String type) {
            this.dice = dice;
            this.type = type;
        }

        public String getDice() {
            return dice;
        }

        public void setDice(String dice) {
            this.dice = dice;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    public static class ItemDurability {
        private int max;
        private int current;

        public ItemDurability() {
        }

        public ItemDurability(int max, int current) {
            this.max = max;
            this.current = current;
        }

        public int getMax() {
            return max;
        }

        public void setMax(int max) {
            this.max = max;
        }

        public int getCurrent() {
            return current;
        }

        public void setCurrent(int current) {
            this.current = current;
        }
    }
}


