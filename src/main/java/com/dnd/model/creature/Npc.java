package com.dnd.model.creature;

import com.dnd.model.character.stats.CoreStats;

import java.util.List;

public class Npc {
    private String id;
    private String name;
    private String description;
    private String role;
    private int level;
    private CoreStats stats;
    private List<String> traits;

    public Npc() {
    }

    public Npc(String id, String name, String description, String role, int level, CoreStats stats, List<String> traits) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.role = role;
        this.level = level;
        this.stats = stats;
        this.traits = traits;
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
}

