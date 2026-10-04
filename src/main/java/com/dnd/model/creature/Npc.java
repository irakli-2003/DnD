package com.dnd.model.creature;

import com.dnd.model.character.stats.CoreStats;
import com.dnd.model.interfaces.Printable;

import java.util.List;

public class Npc implements Printable {
    private String id;
    private String name;
    private String description;
    private String role;
    private int level;
    private CoreStats stats;
    private List<String> traits;
    private List<String> languages;

    public Npc() {
    }

    public Npc(String id, String name, String description, String role, int level, CoreStats stats, List<String> traits,
               List<String> languages) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.role = role;
        this.level = level;
        this.stats = stats;
        this.traits = traits;
        this.languages = languages;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public CoreStats getStats() {
        return stats;
    }

    public void setStats(CoreStats stats) {
        this.stats = stats;
    }

    public List<String> getTraits() {
        return traits;
    }

    public void setTraits(List<String> traits) {
        this.traits = traits;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }

    // ── Vitals (shown in Insert Info and used to seed battle-map tokens) ──────
    private int maxHitPoints;
    private int armorClass;
    private int maxMana;
    private List<com.dnd.model.magic.SpellSlot> spellSlots = new java.util.ArrayList<>();

    public int getMaxHitPoints() { return maxHitPoints; }
    public void setMaxHitPoints(int maxHitPoints) { this.maxHitPoints = Math.max(0, maxHitPoints); }
    public int getArmorClass() { return armorClass; }
    public void setArmorClass(int armorClass) { this.armorClass = Math.max(0, armorClass); }
    public int getMaxMana() { return maxMana; }
    public void setMaxMana(int maxMana) { this.maxMana = Math.max(0, maxMana); }
    public List<com.dnd.model.magic.SpellSlot> getSpellSlots() { return spellSlots; }
    public void setSpellSlots(List<com.dnd.model.magic.SpellSlot> spellSlots) {
        this.spellSlots = spellSlots != null ? spellSlots : new java.util.ArrayList<>();
    }

    private String imagePath;
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    @Override
    public String toString() {
        return name != null ? name : id;
    }
}
