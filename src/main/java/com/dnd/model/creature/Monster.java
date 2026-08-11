package com.dnd.model.creature;

import com.dnd.model.character.stats.CoreStats;
import com.dnd.model.combat.Ability;
import com.dnd.model.interfaces.Printable;

import java.util.List;

public class Monster implements Printable {
    private String id;
    private String name;
    private String description;
    private String type;
    private ChallengeRating challengeRating;
    private CoreStats stats;
    private List<Ability> abilities;
    private List<String> languages;

    public Monster() {
    }

    public Monster(String id, String name, String description, String type, ChallengeRating challengeRating,
                   CoreStats stats, List<Ability> abilities, List<String> languages) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.challengeRating = challengeRating;
        this.stats = stats;
        this.abilities = abilities;
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public ChallengeRating getChallengeRating() {
        return challengeRating;
    }

    public void setChallengeRating(ChallengeRating challengeRating) {
        this.challengeRating = challengeRating;
    }

    public CoreStats getStats() {
        return stats;
    }

    public void setStats(CoreStats stats) {
        this.stats = stats;
    }

    public List<Ability> getAbilities() {
        return abilities;
    }

    public void setAbilities(List<Ability> abilities) {
        this.abilities = abilities;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }

    private String imagePath;
    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    @Override
    public String toString() {
        return name != null ? name : id;
    }
}
