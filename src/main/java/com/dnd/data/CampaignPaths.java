package com.dnd.data;

import java.nio.file.Path;

public class CampaignPaths {
    private final Path root;

    public CampaignPaths(Path root) {
        this.root = root;
    }

    public Path getRoot() {
        return root;
    }

    public Path worldDir() {
        return root.resolve("world");
    }

    public Path playersDir() {
        return root.resolve("players");
    }

    public Path classesFile() {
        return worldDir().resolve("classes.json");
    }

    public Path racesFile() {
        return worldDir().resolve("races.json");
    }

    public Path itemsFile() {
        return worldDir().resolve("items.json");
    }

    public Path spellsFile() {
        return worldDir().resolve("spells.json");
    }

    public Path placesFile() {
        return worldDir().resolve("places.json");
    }

    public Path effectsFile() {
        return worldDir().resolve("effects.json");
    }

    public Path damageTypesFile() {
        return worldDir().resolve("damage-types.json");
    }

    public Path npcsFile() {
        return worldDir().resolve("npcs.json");
    }

    public Path monstersFile() {
        return worldDir().resolve("monsters.json");
    }

    public Path beastsFile() {
        return worldDir().resolve("beasts.json");
    }

    public Path playersFile() {
        return playersDir().resolve("players.json");
    }
}

