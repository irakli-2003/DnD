package com.dnd.data.dto;

import com.dnd.model.character.CharacterRace;

import java.util.List;

public class CharacterRaceCatalog {
    private List<CharacterRace> races;

    public CharacterRaceCatalog() {
    }

    public List<CharacterRace> getRaces() {
        return races;
    }

    public void setRaces(List<CharacterRace> races) {
        this.races = races;
    }
}

