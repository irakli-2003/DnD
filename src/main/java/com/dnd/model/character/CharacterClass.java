package com.dnd.model.character;

import java.util.List;
import java.util.Map;

public class CharacterClass {
    private String id;
    private String name;
    private String description;
    private int hitDie;
    private List<String> primaryAbilities;
    private Map<String, Integer> savingThrowBonuses;

    public CharacterClass() {
    }

    public CharacterClass(String id, String name, String description, int hitDie, List<String> primaryAbilities, Map<String, Integer> savingThrowBonuses) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.hitDie = hitDie;
        this.primaryAbilities = primaryAbilities;
        this.savingThrowBonuses = savingThrowBonuses;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getHitDie() {
        return hitDie;
    }

    public void setHitDie(int hitDie) {
        this.hitDie = hitDie;
    }

    public List<String> getPrimaryAbilities() {
        return primaryAbilities;
    }

    public void setPrimaryAbilities(List<String> primaryAbilities) {
        this.primaryAbilities = primaryAbilities;
    }

    public Map<String, Integer> getSavingThrowBonuses() {
        return savingThrowBonuses;
    }

    public void setSavingThrowBonuses(Map<String, Integer> savingThrowBonuses) {
        this.savingThrowBonuses = savingThrowBonuses;
    }
}


