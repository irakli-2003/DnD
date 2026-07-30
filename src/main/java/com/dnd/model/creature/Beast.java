package com.dnd.model.creature;

import com.dnd.model.character.stats.CoreStats;
import com.dnd.model.combat.Ability;
import com.dnd.model.interfaces.Printable;

import java.util.List;

public class Beast implements Printable {
    private String id;
    private String name;
    private String description;
    private Habitat habitat;
    private ChallengeRating challengeRating;
    private CoreStats stats;
    private List<Ability> abilities;

    public Beast() {
    }

    public Beast(String id, String name, String description, Habitat habitat, ChallengeRating challengeRating, CoreStats stats, List<Ability> abilities) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.habitat = habitat;
        this.challengeRating = challengeRating;
        this.stats = stats;
        this.abilities = abilities;
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

    public Habitat getHabitat() {
        return habitat;
    }

    public void setHabitat(Habitat habitat) {
        this.habitat = habitat;
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

    @Override
    public String toString() {
        return name != null ? name : id;
    }
}

