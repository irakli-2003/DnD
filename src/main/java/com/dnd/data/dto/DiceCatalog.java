package com.dnd.data.dto;

import com.dnd.model.world.Dice;

import java.util.List;

public class DiceCatalog {
    private List<Dice> dice;

    public DiceCatalog() {
    }

    public List<Dice> getDice() {
        return dice;
    }

    public void setDice(List<Dice> dice) {
        this.dice = dice;
    }
}

