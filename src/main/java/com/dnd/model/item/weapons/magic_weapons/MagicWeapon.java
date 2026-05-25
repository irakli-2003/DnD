package com.dnd.model.item.weapons.magic_weapons;

import com.dnd.model.item.Weapon;
import com.dnd.model.magic.School;

public abstract class MagicWeapon extends Weapon {
    private School school;

    protected MagicWeapon() {
        // setMagical(true);
    }

    protected MagicWeapon(School school) {
        this.school = school;
        // setMagical(true);
    }

    public School getSchool() {
        return school;
    }

    public void setSchool(School school) {
        this.school = school;
    }
}
