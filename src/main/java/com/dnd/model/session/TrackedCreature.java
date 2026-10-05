package com.dnd.model.session;

import com.dnd.model.world.map.ActiveEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A monster, NPC or beast the DM is keeping track of in one session file - its own copy of
 * hit points, mana and effects, so two goblins taken from the same catalog entry can be hurt
 * independently without touching the catalog itself.
 */
public class TrackedCreature {

    public enum Kind { MONSTER, NPC, BEAST }

    private String id = UUID.randomUUID().toString();
    private Kind kind;
    private String sourceId;
    private String name;
    private int maxHitPoints;
    private int currentHitPoints;
    private int maxMana;
    private int currentMana;
    private int armorClass;
    private String imagePath;
    private String notes;
    private List<ActiveEffect> activeEffects = new ArrayList<>();

    public TrackedCreature() {
    }

    public TrackedCreature(Kind kind, String sourceId, String name, int maxHitPoints, int maxMana,
                           int armorClass, String imagePath) {
        this.kind = kind;
        this.sourceId = sourceId;
        this.name = name;
        this.maxHitPoints = Math.max(0, maxHitPoints);
        this.currentHitPoints = this.maxHitPoints;
        this.maxMana = Math.max(0, maxMana);
        this.currentMana = this.maxMana;
        this.armorClass = armorClass;
        this.imagePath = imagePath;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Kind getKind() { return kind; }
    public void setKind(Kind kind) { this.kind = kind; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getMaxHitPoints() { return maxHitPoints; }
    public void setMaxHitPoints(int maxHitPoints) { this.maxHitPoints = maxHitPoints; }
    public int getCurrentHitPoints() { return currentHitPoints; }
    public void setCurrentHitPoints(int currentHitPoints) { this.currentHitPoints = currentHitPoints; }
    public int getMaxMana() { return maxMana; }
    public void setMaxMana(int maxMana) { this.maxMana = maxMana; }
    public int getCurrentMana() { return currentMana; }
    public void setCurrentMana(int currentMana) { this.currentMana = currentMana; }
    public int getArmorClass() { return armorClass; }
    public void setArmorClass(int armorClass) { this.armorClass = armorClass; }
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public List<ActiveEffect> getActiveEffects() {
        if (activeEffects == null) activeEffects = new ArrayList<>();
        return activeEffects;
    }

    public void setActiveEffects(List<ActiveEffect> activeEffects) { this.activeEffects = activeEffects; }
}
