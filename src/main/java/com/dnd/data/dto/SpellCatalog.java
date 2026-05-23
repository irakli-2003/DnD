package com.dnd.data.dto;

import com.dnd.model.magic.Spell;

import java.util.List;

public class SpellCatalog {
    private List<Spell> spells;

    public SpellCatalog() {
    }

    public List<Spell> getSpells() {
        return spells;
    }

    public void setSpells(List<Spell> spells) {
        this.spells = spells;
    }
}

