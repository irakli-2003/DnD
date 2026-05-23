package com.dnd.model.character.stats;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CombatStats {
    private int armorClass;
    private int initiative;
    private int speed;
    private int maxHitPoints;
    private int currentHitPoints;
    private List<TemporaryHitPoints> temporaryHitPoints;
    private int inspiration;
    private int proficiencyBonus;
    private int deathSaveSuccess;
    private int deathSaveFailure;

    public CombatStats() {
    }

    public CombatStats(int armorClass, int initiative, int speed, int maxHitPoints, int currentHitPoints,
                       List<TemporaryHitPoints> temporaryHitPoints, int inspiration, int proficiencyBonus,
                       int deathSaveSuccess, int deathSaveFailure) {
        this.armorClass = armorClass;
        this.initiative = initiative;
        this.speed = speed;
        this.maxHitPoints = maxHitPoints;
        this.currentHitPoints = currentHitPoints;
        this.temporaryHitPoints = temporaryHitPoints;
        this.inspiration = inspiration;
        this.proficiencyBonus = proficiencyBonus;
        this.deathSaveSuccess = deathSaveSuccess;
        this.deathSaveFailure = deathSaveFailure;
    }

    public void tickTemporaryHitPoints() {
        if (temporaryHitPoints == null) {
            return;
        }
        Iterator<TemporaryHitPoints> iterator = temporaryHitPoints.iterator();
        while (iterator.hasNext()) {
            TemporaryHitPoints entry = iterator.next();
            entry.setTurnsRemaining(entry.getTurnsRemaining() - 1);
            if (entry.getTurnsRemaining() <= 0 || entry.getAmount() <= 0) {
                iterator.remove();
            }
        }
    }

    public int getArmorClass() {
        return armorClass;
    }

    public void setArmorClass(int armorClass) {
        this.armorClass = armorClass;
    }

    public int getInitiative() {
        return initiative;
    }

    public void setInitiative(int initiative) {
        this.initiative = initiative;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getMaxHitPoints() {
        return maxHitPoints;
    }

    public void setMaxHitPoints(int maxHitPoints) {
        this.maxHitPoints = maxHitPoints;
    }

    public int getCurrentHitPoints() {
        return currentHitPoints;
    }

    public void setCurrentHitPoints(int currentHitPoints) {
        this.currentHitPoints = currentHitPoints;
    }

    public List<TemporaryHitPoints> getTemporaryHitPoints() {
        if (temporaryHitPoints == null) {
            temporaryHitPoints = new ArrayList<>();
        }
        return temporaryHitPoints;
    }

    public void setTemporaryHitPoints(List<TemporaryHitPoints> temporaryHitPoints) {
        this.temporaryHitPoints = temporaryHitPoints;
    }

    public int getInspiration() {
        return inspiration;
    }

    public void setInspiration(int inspiration) {
        this.inspiration = inspiration;
    }

    public int getProficiencyBonus() {
        return proficiencyBonus;
    }

    public void setProficiencyBonus(int proficiencyBonus) {
        this.proficiencyBonus = proficiencyBonus;
    }

    public int getDeathSaveSuccess() {
        return deathSaveSuccess;
    }

    public void setDeathSaveSuccess(int deathSaveSuccess) {
        this.deathSaveSuccess = deathSaveSuccess;
    }

    public int getDeathSaveFailure() {
        return deathSaveFailure;
    }

    public void setDeathSaveFailure(int deathSaveFailure) {
        this.deathSaveFailure = deathSaveFailure;
    }

    public static class TemporaryHitPoints {
        private int amount;
        private int turnsRemaining;

        public TemporaryHitPoints() {
        }

        public TemporaryHitPoints(int amount, int turnsRemaining) {
            this.amount = amount;
            this.turnsRemaining = turnsRemaining;
        }

        public int getAmount() {
            return amount;
        }

        public void setAmount(int amount) {
            this.amount = amount;
        }

        public int getTurnsRemaining() {
            return turnsRemaining;
        }

        public void setTurnsRemaining(int turnsRemaining) {
            this.turnsRemaining = turnsRemaining;
        }
    }
}


