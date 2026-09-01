package com.dnd.model.combat;

import com.dnd.model.interfaces.Printable;

public class Effect implements Printable {
    private String id;
    private String name;
    private String description;
    private boolean damaging;
    private boolean healing;
    private int damageAmount;
    private int healingAmount;

    public Effect() {
    }

    public Effect(String id, String name, String description, boolean damaging, boolean healing, int damageAmount, int healingAmount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.damaging = damaging;
        this.healing = healing;
        this.damageAmount = damageAmount;
        this.healingAmount = healingAmount;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isDamaging() {
        return damaging;
    }

    public void setDamaging(boolean damaging) {
        this.damaging = damaging;
    }

    public boolean isHealing() {
        return healing;
    }

    public void setHealing(boolean healing) {
        this.healing = healing;
    }

    public int getDamageAmount() {
        return damageAmount;
    }

    public void setDamageAmount(int damageAmount) {
        this.damageAmount = damageAmount;
    }

    public int getHealingAmount() {
        return healingAmount;
    }

    public void setHealingAmount(int healingAmount) {
        this.healingAmount = healingAmount;
    }

    /**
     * How many rounds this effect lingers once applied. Zero means it resolves immediately
     * and leaves nothing behind; anything higher makes the damage and healing amounts
     * per-round figures that tick until the effect wears off.
     */
    private int durationRounds;

    public int getDurationRounds() {
        return durationRounds;
    }

    public void setDurationRounds(int durationRounds) {
        this.durationRounds = Math.max(0, durationRounds);
    }

    /** True when this effect lasts beyond the moment it is applied. */
    public boolean isLasting() {
        return durationRounds > 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name != null ? name : id);
        if (damaging && damageAmount > 0) {
            sb.append(" (").append(damageAmount).append(" damage)");
        }
        if (healing && healingAmount > 0) {
            sb.append(" (").append(healingAmount).append(" healing)");
        }
        return sb.toString();
    }
}

