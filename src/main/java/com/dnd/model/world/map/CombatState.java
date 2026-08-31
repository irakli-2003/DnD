package com.dnd.model.world.map;

import java.util.ArrayList;
import java.util.List;

/**
 * The mutable, per-token state of a battle: hit points, mana, money, initiative and
 * dying/dead status.
 *
 * <p>This deliberately lives on the <em>token</em> rather than on the catalog entity it
 * references. A single "Goblin" catalog entry can be placed on a map five times, and each
 * of those goblins takes damage independently; writing hit points back into the catalog
 * would make them share one pool and would corrupt the campaign's reusable bestiary.
 * Because tokens are serialized inside the map, battle state persists with the map.</p>
 */
public class CombatState {

    /** A creature gets three failed death saves before it dies, per the usual tabletop rule. */
    public static final int MAX_DEATH_SAVES = 3;

    private int maxHitPoints;
    private int currentHitPoints;
    private int maxMana;
    private int currentMana;
    private int gold;
    private int initiative;
    /** Number of failed death saves so far, 0..{@link #MAX_DEATH_SAVES}. */
    private int deathSaveFailures;
    private boolean downed;
    private boolean dead;
    private List<String> conditions = new ArrayList<>();
    private String notes;
    /** False for tokens that are scenery or loot rather than participants in turn order. */
    private boolean inInitiative = true;

    public CombatState() {
    }

    public CombatState(int maxHitPoints) {
        this.maxHitPoints = Math.max(0, maxHitPoints);
        this.currentHitPoints = this.maxHitPoints;
    }

    // ── Hit points ──────────────────────────────────────────────────────────

    public int getMaxHitPoints() {
        return maxHitPoints;
    }

    public void setMaxHitPoints(int maxHitPoints) {
        this.maxHitPoints = Math.max(0, maxHitPoints);
        if (currentHitPoints > this.maxHitPoints) currentHitPoints = this.maxHitPoints;
    }

    public int getCurrentHitPoints() {
        return currentHitPoints;
    }

    public void setCurrentHitPoints(int currentHitPoints) {
        this.currentHitPoints = clampHitPoints(currentHitPoints);
    }

    /**
     * Applies damage (positive) or healing (negative) and updates dying/dead status.
     *
     * <p>Dropping to zero knocks a creature down rather than killing it outright, which is
     * what makes the death-save countdown meaningful. Healing a downed creature above zero
     * revives it and clears its accumulated failed saves.</p>
     */
    public void applyDamage(int amount) {
        setCurrentHitPoints(currentHitPoints - amount);
        if (currentHitPoints <= 0) {
            if (!dead) downed = true;
        } else {
            downed = false;
            dead = false;
            deathSaveFailures = 0;
        }
    }

    public void heal(int amount) {
        applyDamage(-Math.abs(amount));
    }

    private int clampHitPoints(int value) {
        if (value < 0) return 0;
        if (maxHitPoints > 0 && value > maxHitPoints) return maxHitPoints;
        return value;
    }

    // ── Death saves ─────────────────────────────────────────────────────────

    public int getDeathSaveFailures() {
        return deathSaveFailures;
    }

    public void setDeathSaveFailures(int deathSaveFailures) {
        this.deathSaveFailures = Math.max(0, Math.min(MAX_DEATH_SAVES, deathSaveFailures));
        this.dead = this.deathSaveFailures >= MAX_DEATH_SAVES;
        if (this.dead) downed = false;
    }

    /** Records a failed death save; the third one kills the creature. */
    public void failDeathSave() {
        setDeathSaveFailures(deathSaveFailures + 1);
        if (!dead) downed = true;
    }

    /** Undoes a failed death save, e.g. after a misclick or a successful save. */
    public void succeedDeathSave() {
        setDeathSaveFailures(deathSaveFailures - 1);
        if (currentHitPoints <= 0) downed = true;
    }

    /**
     * How many failed saves the creature can still absorb, which is the number shown on its
     * token while it is dying. Zero means dead, and the token shows a cross instead.
     */
    public int remainingDeathSaves() {
        return Math.max(0, MAX_DEATH_SAVES - deathSaveFailures);
    }

    public boolean isDowned() {
        return downed && !dead;
    }

    public void setDowned(boolean downed) {
        this.downed = downed;
    }

    public boolean isDead() {
        return dead;
    }

    public void setDead(boolean dead) {
        this.dead = dead;
        if (dead) {
            downed = false;
            deathSaveFailures = MAX_DEATH_SAVES;
            currentHitPoints = 0;
        }
    }

    /** Fully restores a creature: alive, unhurt, and with its death saves cleared. */
    public void revive() {
        dead = false;
        downed = false;
        deathSaveFailures = 0;
        currentHitPoints = maxHitPoints > 0 ? maxHitPoints : 1;
    }

    /** A dead creature is skipped by the initiative order but stays visible on the map. */
    public boolean isActingThisRound() {
        return !dead;
    }

    // ── Mana, money, initiative ─────────────────────────────────────────────

    public int getMaxMana() {
        return maxMana;
    }

    public void setMaxMana(int maxMana) {
        this.maxMana = Math.max(0, maxMana);
        if (currentMana > this.maxMana) currentMana = this.maxMana;
    }

    public int getCurrentMana() {
        return currentMana;
    }

    public void setCurrentMana(int currentMana) {
        if (currentMana < 0) this.currentMana = 0;
        else if (maxMana > 0 && currentMana > maxMana) this.currentMana = maxMana;
        else this.currentMana = currentMana;
    }

    public int getGold() {
        return gold;
    }

    public void setGold(int gold) {
        this.gold = Math.max(0, gold);
    }

    public int getInitiative() {
        return initiative;
    }

    public void setInitiative(int initiative) {
        this.initiative = initiative;
    }

    // ── Conditions & notes ──────────────────────────────────────────────────

    public List<String> getConditions() {
        return conditions;
    }

    public void setConditions(List<String> conditions) {
        this.conditions = conditions != null ? conditions : new ArrayList<>();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isInInitiative() {
        return inInitiative;
    }

    public void setInInitiative(boolean inInitiative) {
        this.inInitiative = inInitiative;
    }

    /** Fraction of max hit points remaining, in 0..1, for drawing health bars. */
    public double healthFraction() {
        if (maxHitPoints <= 0) return 0;
        return Math.max(0, Math.min(1, currentHitPoints / (double) maxHitPoints));
    }

    public double manaFraction() {
        if (maxMana <= 0) return 0;
        return Math.max(0, Math.min(1, currentMana / (double) maxMana));
    }
}
