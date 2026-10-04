package com.dnd.model.magic;

import com.dnd.model.interfaces.Printable;

/** Spell slots of one level: how many the caster has in total and how many are still unspent. */
public class SpellSlot implements Printable {
    private int level = 1;
    private int max;
    private int current;

    public SpellSlot() {
    }

    public SpellSlot(int level, int max, int current) {
        setLevel(level);
        setMax(max);
        setCurrent(current);
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = Math.max(1, Math.min(9, level));
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = Math.max(0, max);
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = Math.max(0, current);
    }

    public SpellSlot copy() {
        return new SpellSlot(level, max, current);
    }

    @Override
    public String toString() {
        return "Level " + level + ": " + current + "/" + max;
    }
}
