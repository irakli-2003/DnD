package com.dnd.model.item.weapons.physical_weapons;

public class RangedWeapon extends PhysicalWeapon {
    private int maxCharges;
    private int currentCharges;

    public RangedWeapon() {
        super();
    }

    public int getMaxCharges() {
        return maxCharges;
    }

    public void setMaxCharges(int maxCharges) {
        this.maxCharges = maxCharges;
    }

    public int getCurrentCharges() {
        return currentCharges;
    }

    public void setCurrentCharges(int currentCharges) {
        this.currentCharges = currentCharges;
    }

    public boolean charge() {
        if (currentCharges >= maxCharges) {
            return false;
        }
        currentCharges++;
        return true;
    }

    public boolean discharge() {
        if (currentCharges <= 0) {
            return false;
        }
        currentCharges--;
        return true;
    }
}
