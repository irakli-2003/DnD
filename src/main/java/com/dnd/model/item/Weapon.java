package com.dnd.model.item;

import com.dnd.model.character.stats.CoreStats;
import com.dnd.model.combat.Damage;
import com.dnd.model.combat.Effect;

import java.util.List;

public class Weapon extends Item {
    private boolean dualWield;
    private Damage damage;
    private boolean magical;
    private CoreStats minRequiredCoreStats;
    private List<Effect> effects;

    public Weapon() {
    }

    public Weapon(String id, String name, String type, String description, int valueGold, double weight,
                  ItemDamage itemDamage, ItemDurability durability, boolean dualWield, Damage damage,
                  boolean magical, CoreStats minRequiredCoreStats, List<Effect> effects) {
        super(id, name, type, description, valueGold, weight, itemDamage, durability);
        this.dualWield = dualWield;
        this.damage = damage;
        this.magical = magical;
        this.minRequiredCoreStats = minRequiredCoreStats;
        this.effects = effects;
    }

    public boolean isDualWield() {
        return dualWield;
    }

    public void setDualWield(boolean dualWield) {
        this.dualWield = dualWield;
    }

    public Damage getWeaponDamage() {
        return damage;
    }

    public void setWeaponDamage(Damage damage) {
        this.damage = damage;
    }

    public boolean isMagical() {
        return magical;
    }

    public void setMagical(boolean magical) {
        this.magical = magical;
    }

    public CoreStats getMinRequiredCoreStats() {
        return minRequiredCoreStats;
    }

    public void setMinRequiredCoreStats(CoreStats minRequiredCoreStats) {
        this.minRequiredCoreStats = minRequiredCoreStats;
    }

    public List<Effect> getEffects() {
        return effects;
    }

    public void setEffects(List<Effect> effects) {
        this.effects = effects;
    }
}

