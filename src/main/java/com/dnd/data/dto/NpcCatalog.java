package com.dnd.data.dto;

import com.dnd.model.creature.Npc;

import java.util.List;

public class NpcCatalog {
    private List<Npc> npcs;

    public NpcCatalog() {
    }

    public List<Npc> getNpcs() {
        return npcs;
    }

    public void setNpcs(List<Npc> npcs) {
        this.npcs = npcs;
    }
}

