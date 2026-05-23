package com.dnd.data.dto;

import com.dnd.model.character.CharacterClass;

import java.util.List;

public class CharacterClassCatalog {
    private List<CharacterClass> classes;

    public CharacterClassCatalog() {
    }

    public List<CharacterClass> getClasses() {
        return classes;
    }

    public void setClasses(List<CharacterClass> classes) {
        this.classes = classes;
    }
}

