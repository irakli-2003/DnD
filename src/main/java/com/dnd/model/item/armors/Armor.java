package com.dnd.model.item.armors;

import com.dnd.model.item.Item;

public class Armor extends Item {
    private ArmorMaterial material;
    private int armorClassBonus;

    public Armor() {
    }

    public Armor(String id, String name, String type, String description, int valueGold, double weight,
                 ItemDamage damage, ItemDurability durability, ArmorMaterial material, int armorClassBonus) {
        super(id, name, type, description, valueGold, weight, damage, durability);
        this.material = material;
        this.armorClassBonus = armorClassBonus;
    }

    public ArmorMaterial getMaterial() {
        return material;
    }

    public void setMaterial(ArmorMaterial material) {
        this.material = material;
    }

    public int getArmorClassBonus() {
        return armorClassBonus;
    }

    public void setArmorClassBonus(int armorClassBonus) {
        this.armorClassBonus = armorClassBonus;
    }
}


