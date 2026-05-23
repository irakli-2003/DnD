package com.dnd.model.item;

public class LegArmor extends Armor {
    public LegArmor() {
    }

    public LegArmor(String id, String name, String type, String description, int valueGold, double weight,
                    ItemDamage damage, ItemDurability durability, ArmorMaterial material, int armorClassBonus) {
        super(id, name, type, description, valueGold, weight, damage, durability, material, armorClassBonus);
    }
}


