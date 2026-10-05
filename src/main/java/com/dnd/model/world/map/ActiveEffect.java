package com.dnd.model.world.map;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * An effect currently riding on a creature - frost for three more rounds, a blessing until
 * the end of the fight, and so on.
 *
 * <p>This is a snapshot taken at the moment of casting rather than a live reference to the
 * catalog entry. That keeps a fight honest: retuning a spell mid-campaign doesn't reach
 * back and change what is already burning on someone, and the effect survives being saved
 * with the map even if the DM later deletes the effect it came from.</p>
 */
public class ActiveEffect {

    private String effectId;
    private String name;
    private String description;
    /** Rounds still to run. Counted down at the start of the bearer's turn. */
    private int remainingRounds;
    /** Damage dealt to the bearer each round while this lasts. */
    private int damagePerRound;
    /** Healing given to the bearer each round while this lasts. */
    private int healingPerRound;
    /** Who or what applied it, purely so the DM can see where it came from. */
    private String source;
    /** Extra damage on Strength melee attacks while this lasts (e.g. a barbarian's Rage). */
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private int meleeDamageBonus;
    /** Armor Class added while this lasts (or removed, when negative). */
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    private int acBonus;
    /** Damage type ids halved while this lasts. */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<String> resistances = new ArrayList<>();

    public ActiveEffect() {
    }

    public ActiveEffect(String effectId, String name, int remainingRounds,
                        int damagePerRound, int healingPerRound, String source) {
        this.effectId = effectId;
        this.name = name;
        setRemainingRounds(remainingRounds);
        this.damagePerRound = damagePerRound;
        this.healingPerRound = healingPerRound;
        this.source = source;
    }

    public String getEffectId() {
        return effectId;
    }

    public void setEffectId(String effectId) {
        this.effectId = effectId;
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

    public int getRemainingRounds() {
        return remainingRounds;
    }

    public void setRemainingRounds(int remainingRounds) {
        this.remainingRounds = Math.max(0, remainingRounds);
    }

    public int getDamagePerRound() {
        return damagePerRound;
    }

    public void setDamagePerRound(int damagePerRound) {
        this.damagePerRound = Math.max(0, damagePerRound);
    }

    public int getHealingPerRound() {
        return healingPerRound;
    }

    public void setHealingPerRound(int healingPerRound) {
        this.healingPerRound = Math.max(0, healingPerRound);
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public int getMeleeDamageBonus() {
        return meleeDamageBonus;
    }

    public void setMeleeDamageBonus(int meleeDamageBonus) {
        this.meleeDamageBonus = meleeDamageBonus;
    }

    public int getAcBonus() {
        return acBonus;
    }

    public void setAcBonus(int acBonus) {
        this.acBonus = acBonus;
    }

    public List<String> getResistances() {
        if (resistances == null) resistances = new ArrayList<>();
        return resistances;
    }

    public void setResistances(List<String> resistances) {
        this.resistances = resistances != null ? new ArrayList<>(resistances) : new ArrayList<>();
    }

    public ActiveEffect copy() {
        ActiveEffect c = new ActiveEffect(effectId, name, remainingRounds, damagePerRound, healingPerRound, source);
        c.description = description;
        c.meleeDamageBonus = meleeDamageBonus;
        c.acBonus = acBonus;
        c.resistances = new ArrayList<>(getResistances());
        return c;
    }

    /** True once this has run its course and should be dropped from the bearer. */
    public boolean isExpired() {
        return remainingRounds <= 0;
    }

    /** Counts one round off, returning true when that was the last one. */
    public boolean tick() {
        if (remainingRounds > 0) remainingRounds--;
        return isExpired();
    }

    public String label() {
        String shown = name != null && !name.isBlank() ? name : effectId;
        return shown + " (" + remainingRounds + (remainingRounds == 1 ? " round)" : " rounds)");
    }

    @Override
    public String toString() {
        return label();
    }
}
