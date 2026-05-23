package com.dnd.data.dto;

import com.dnd.model.combat.DamageType;

import java.util.List;

public class DamageTypeCatalog {
    private List<DamageType> damageTypes;

    public DamageTypeCatalog() {
    }

    public List<DamageType> getDamageTypes() {
        return damageTypes;
    }

    public void setDamageTypes(List<DamageType> damageTypes) {
        this.damageTypes = damageTypes;
    }
}

