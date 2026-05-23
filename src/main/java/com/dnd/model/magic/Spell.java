package com.dnd.model.magic;

import com.dnd.model.combat.Damage;
import com.dnd.model.combat.Effect;
import com.dnd.model.item.Item;

import java.util.List;

public class Spell {
    private String id;
    private String name;
    private String description;
    private int level;
    private School school;
    private int manaCost;
    private int range;
    private int radius;
    private CastingMethod castingMethod;
    private List<Item> requiredConsumables;
    private List<Item> requiredTools;
    private Concentration concentration;
    private List<Effect> effects;
    private Damage damage;

    public Spell() {
    }

    public Spell(String id, String name, String description, int level, School school, int manaCost,
                 int range, int radius, CastingMethod castingMethod, List<Item> requiredConsumables,
                 List<Item> requiredTools, Concentration concentration, List<Effect> effects, Damage damage) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.level = level;
        this.school = school;
        this.manaCost = manaCost;
        this.range = range;
        this.radius = radius;
        this.castingMethod = castingMethod;
        this.requiredConsumables = requiredConsumables;
        this.requiredTools = requiredTools;
        this.concentration = concentration;
        this.effects = effects;
        this.damage = damage;
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

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public School getSchool() {
        return school;
    }

    public void setSchool(School school) {
        this.school = school;
    }

    public int getManaCost() {
        return manaCost;
    }

    public void setManaCost(int manaCost) {
        this.manaCost = manaCost;
    }

    public int getRange() {
        return range;
    }

    public void setRange(int range) {
        this.range = range;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public CastingMethod getCastingMethod() {
        return castingMethod;
    }

    public void setCastingMethod(CastingMethod castingMethod) {
        this.castingMethod = castingMethod;
    }

    public List<Item> getRequiredConsumables() {
        return requiredConsumables;
    }

    public void setRequiredConsumables(List<Item> requiredConsumables) {
        this.requiredConsumables = requiredConsumables;
    }

    public List<Item> getRequiredTools() {
        return requiredTools;
    }

    public void setRequiredTools(List<Item> requiredTools) {
        this.requiredTools = requiredTools;
    }

    public Concentration getConcentration() {
        return concentration;
    }

    public void setConcentration(Concentration concentration) {
        this.concentration = concentration;
    }

    public List<Effect> getEffects() {
        return effects;
    }

    public void setEffects(List<Effect> effects) {
        this.effects = effects;
    }

    public Damage getDamage() {
        return damage;
    }

    public void setDamage(Damage damage) {
        this.damage = damage;
    }
}
