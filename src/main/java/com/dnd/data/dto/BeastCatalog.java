package com.dnd.data.dto;

import com.dnd.model.creature.Beast;

import java.util.List;

public class BeastCatalog {
    private List<Beast> beasts;

    public BeastCatalog() {
    }

    public List<Beast> getBeasts() {
        return beasts;
    }

    public void setBeasts(List<Beast> beasts) {
        this.beasts = beasts;
    }
}

