package com.dnd.model.item.alchemy;

import com.dnd.model.alchemy.AlchemyIngredient;
import com.dnd.model.combat.Effect;
import com.dnd.model.item.Item;

import java.util.List;

public abstract class AlchemyItem extends Item {
    private List<AlchemyIngredient> requiredMaterials;
    private List<Effect> effects;

    protected AlchemyItem() {
    }

    protected AlchemyItem(String id, String name, String type, String description, int valueGold, double weight,
                          ItemDamage damage, ItemDurability durability, List<AlchemyIngredient> requiredMaterials,
                          List<Effect> effects) {
        super(id, name, type, description, valueGold, weight, damage, durability);
        this.requiredMaterials = requiredMaterials;
        this.effects = effects;
    }

    public List<AlchemyIngredient> getRequiredMaterials() {
        return requiredMaterials;
    }

    public void setRequiredMaterials(List<AlchemyIngredient> requiredMaterials) {
        this.requiredMaterials = requiredMaterials;
    }

    public List<Effect> getEffects() {
        return effects;
    }

    public void setEffects(List<Effect> effects) {
        this.effects = effects;
    }
}
