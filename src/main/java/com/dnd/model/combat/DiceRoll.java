package com.dnd.model.combat;

/**
 * One entry in a {@link Damage}'s dice pool: a count of a particular catalogue
 * {@link com.dnd.model.world.Dice} (e.g. "2" of "d6" for 2d6 fire damage). A cast's total
 * damage is however many of these the DM actually rolls on the table and types in - see
 * {@link CastResolver} - rather than a number the app invents for them.
 */
public class DiceRoll {
    private String diceId;
    private int count = 1;

    public DiceRoll() {
    }

    public DiceRoll(String diceId, int count) {
        this.diceId = diceId;
        this.count = Math.max(1, count);
    }

    public String getDiceId() {
        return diceId;
    }

    public void setDiceId(String diceId) {
        this.diceId = diceId;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = Math.max(1, count);
    }

    @Override
    public String toString() {
        return count + (diceId == null ? "" : diceId);
    }
}
