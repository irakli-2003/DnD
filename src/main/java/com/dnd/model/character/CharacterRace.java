package com.dnd.model.character;

import com.dnd.model.interfaces.Printable;
import java.util.Map;

public class CharacterRace implements Printable {
    private String id;
    private String name;
    private String description;
    private Map<String, Integer> abilityBonuses;
    private int speed;

    public CharacterRace() {
    }

    public CharacterRace(String id, String name, String description, Map<String, Integer> abilityBonuses, int speed) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.abilityBonuses = abilityBonuses;
        this.speed = speed;
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

    public Map<String, Integer> getAbilityBonuses() {
        return abilityBonuses;
    }

    public void setAbilityBonuses(Map<String, Integer> abilityBonuses) {
        this.abilityBonuses = abilityBonuses;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    @Override
    public String toString() {
        return name != null ? name : id;
    }
}