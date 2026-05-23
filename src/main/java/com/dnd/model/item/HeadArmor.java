package com.dnd.model.item;

public class HeadArmor extends Armor {
    public HeadArmor() {
    }

    public HeadArmor(String id, String name, String type, String description, int valueGold, double weight,
                     ItemDamage damage, ItemDurability durability, ArmorMaterial material, int armorClassBonus) {
        super(id, name, type, description, valueGold, weight, damage, durability, material, armorClassBonus);
    }
}


