package com.dnd.data.dto;

import com.dnd.model.creature.Monster;

import java.util.List;

public class MonsterCatalog {
    private List<Monster> monsters;

    public MonsterCatalog() {
    }

    public List<Monster> getMonsters() {
        return monsters;
    }

    public void setMonsters(List<Monster> monsters) {
        this.monsters = monsters;
    }
}

