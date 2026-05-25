package com.dnd.model.item.armors;

public class HandArmor extends Armor {
    public HandArmor() {
    }

    public HandArmor(String id, String name, String type, String description, int valueGold, double weight,
                     ItemDamage damage, ItemDurability durability, ArmorMaterial material, int armorClassBonus) {
        super(id, name, type, description, valueGold, weight, damage, durability, material, armorClassBonus);
    }
}


