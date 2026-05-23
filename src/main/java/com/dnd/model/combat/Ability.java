package com.dnd.model.combat;

import java.util.List;

public class Ability {
    private String id;
    private String name;
    private String description;
    private List<String> effects;

    public Ability() {
    }

    public Ability(String id, String name, String description, List<String> effects) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.effects = effects;
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

    public List<String> getEffects() {
        return effects;
    }

    public void setEffects(List<String> effects) {
        this.effects = effects;
    }
}


