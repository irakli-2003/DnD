package com.dnd.data.dto;

import com.dnd.model.combat.Effect;

import java.util.List;

public class EffectCatalog {
    private List<Effect> effects;

    public EffectCatalog() {
    }

    public List<Effect> getEffects() {
        return effects;
    }

    public void setEffects(List<Effect> effects) {
        this.effects = effects;
    }
}

