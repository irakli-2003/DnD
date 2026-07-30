package com.dnd.model.combat;

import com.dnd.model.interfaces.Printable;
import java.util.List;

public class Ability implements Printable {
    private String id;
    private String name;
    private String description;
    private List<String> effects;
    private double range;
    private int recharge;

    public Ability() {
    }

    public Ability(String id, String name, String description, List<String> effects, double range, int recharge) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.effects = effects;
        this.range = range;
        this.recharge = recharge;
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

    public double getRange() {
        return range;
    }

    public void setRange(double range) {
        this.range = range;
    }

    public int getRecharge() {
        return recharge;
    }

    public void setRecharge(int recharge) {
        this.recharge = recharge;
    }

    @Override
    public String toString() {
        return name != null ? name : id;
    }
}
