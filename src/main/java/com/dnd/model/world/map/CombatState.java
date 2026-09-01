package com.dnd.model.world.map;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /** Feet of walking movement per round assumed for a creature nobody has configured. */
    public static final int DEFAULT_WALK_SPEED = 30;

    /** One square of the battle grid is five feet, the usual tabletop scale. */
    public static final int FEET_PER_SQUARE = 5;

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

    /**
     * Movement rates in feet per round. Walking defaults to 30, the speed of most
     * playable races; a zero climb or swim speed means the creature has no special
     * mode for that terrain and crosses it at the difficult-terrain rate.
     */
    private int walkSpeed = DEFAULT_WALK_SPEED;
    private int climbSpeed;
    private int swimSpeed;
    /**
     * Set once the speeds have been filled in from the creature's race or stat block, so a
     * later hand-edit by the DM is never silently overwritten by re-seeding.
     */
    private boolean speedSeeded;
    /**
     * Squares of movement already spent this round. Reset when the turn advances, so
     * it is genuinely per-turn rather than a running total for the whole battle.
     */
    private int movementUsed;

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

    // ── Movement ────────────────────────────────────────────────────────────

    public int getWalkSpeed() {
        return walkSpeed;
    }

    public boolean isSpeedSeeded() {
        return speedSeeded;
    }

    public void setSpeedSeeded(boolean speedSeeded) {
        this.speedSeeded = speedSeeded;
    }

    public void setWalkSpeed(int walkSpeed) {
        this.walkSpeed = Math.max(0, walkSpeed);
    }

    public int getClimbSpeed() {
        return climbSpeed;
    }

    public void setClimbSpeed(int climbSpeed) {
        this.climbSpeed = Math.max(0, climbSpeed);
    }

    public int getSwimSpeed() {
        return swimSpeed;
    }

    public void setSwimSpeed(int swimSpeed) {
        this.swimSpeed = Math.max(0, swimSpeed);
    }

    public int getMovementUsed() {
        return movementUsed;
    }

    public void setMovementUsed(int movementUsed) {
        this.movementUsed = Math.max(0, movementUsed);
    }

    public boolean hasClimbSpeed() {
        return climbSpeed > 0;
    }

    public boolean hasSwimSpeed() {
        return swimSpeed > 0;
    }

    /** Total squares this creature may cover in one turn, walking. */
    public int movementSquares() {
        return walkSpeed / FEET_PER_SQUARE;
    }

    /** Squares still available this turn after what has already been spent. */
    public int movementRemaining() {
        return Math.max(0, movementSquares() - movementUsed);
    }

    /** Called when the turn passes to this creature, giving it a fresh movement allowance. */
    public void resetMovement() {
        movementUsed = 0;
    }

    // ── Effects and cooldowns ───────────────────────────────────────────────

    /**
     * Effects currently running on this creature. Kept on the combat state rather than on
     * the catalog entry so two orcs hit by the same spell can burn out independently.
     */
    private List<ActiveEffect> activeEffects = new ArrayList<>();

    /**
     * Rounds left before each spell or ability can be used again, keyed by its id. Entries
     * are removed once they reach zero, so an empty map means everything is ready.
     */
    private Map<String, Integer> cooldowns = new LinkedHashMap<>();

    public List<ActiveEffect> getActiveEffects() {
        if (activeEffects == null) activeEffects = new ArrayList<>();
        return activeEffects;
    }

    public void setActiveEffects(List<ActiveEffect> activeEffects) {
        this.activeEffects = activeEffects != null ? activeEffects : new ArrayList<>();
    }

    public Map<String, Integer> getCooldowns() {
        if (cooldowns == null) cooldowns = new LinkedHashMap<>();
        return cooldowns;
    }

    public void setCooldowns(Map<String, Integer> cooldowns) {
        this.cooldowns = cooldowns != null ? cooldowns : new LinkedHashMap<>();
    }

    /**
     * Adds an effect, refreshing the duration instead of stacking a second copy when the
     * same effect is already running - being frozen twice makes it last longer, not tick
     * twice as hard.
     */
    public void addEffect(ActiveEffect effect) {
        if (effect == null || effect.isExpired()) return;
        for (ActiveEffect existing : getActiveEffects()) {
            if (existing.getEffectId() != null && existing.getEffectId().equals(effect.getEffectId())) {
                existing.setRemainingRounds(Math.max(existing.getRemainingRounds(), effect.getRemainingRounds()));
                return;
            }
        }
        getActiveEffects().add(effect);
    }

    /** Rounds left on a spell or ability, or zero when it is ready to use. */
    public int cooldownFor(String actionId) {
        Integer left = getCooldowns().get(actionId);
        return left == null ? 0 : Math.max(0, left);
    }

    public boolean isOnCooldown(String actionId) {
        return cooldownFor(actionId) > 0;
    }

    /** Puts a spell or ability out of action for {@code rounds}; zero rounds is a no-op. */
    public void startCooldown(String actionId, int rounds) {
        if (actionId == null || rounds <= 0) return;
        getCooldowns().put(actionId, rounds);
    }

    /** Counts every cooldown down by one round, dropping the ones that have recovered. */
    public void tickCooldowns() {
        Iterator<Map.Entry<String, Integer>> iterator = getCooldowns().entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();
            int left = entry.getValue() == null ? 0 : entry.getValue() - 1;
            if (left <= 0) {
                iterator.remove();
            } else {
                entry.setValue(left);
            }
        }
    }

    /**
     * Runs one round of every active effect: applies its per-round damage or healing, then
     * counts it down and drops the ones that have worn off.
     *
     * @return a human-readable line per effect that did something, for the DM's log
     */
    public List<String> tickEffects(String bearerName) {
        List<String> log = new ArrayList<>();
        Iterator<ActiveEffect> iterator = getActiveEffects().iterator();
        while (iterator.hasNext()) {
            ActiveEffect effect = iterator.next();
            if (effect.getDamagePerRound() > 0) {
                applyDamage(effect.getDamagePerRound());
                log.add(bearerName + " takes " + effect.getDamagePerRound() + " from " + effect.getName() + ".");
            }
            if (effect.getHealingPerRound() > 0) {
                heal(effect.getHealingPerRound());
                log.add(bearerName + " recovers " + effect.getHealingPerRound() + " from " + effect.getName() + ".");
            }
            if (effect.tick()) {
                iterator.remove();
                log.add(effect.getName() + " wears off " + bearerName + ".");
            }
        }
        return log;
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
