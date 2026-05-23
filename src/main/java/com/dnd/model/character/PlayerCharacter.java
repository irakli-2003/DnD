package com.dnd.model.character;

import com.dnd.model.character.stats.CoreStats;

import java.util.List;

public class PlayerCharacter {
    private String id;
    private String name;
    private String classId;
    private String raceId;
    private int level;
    private CoreStats stats;
    private List<PlayerItem> items;
    private List<PlayerSpell> spells;

    public PlayerCharacter() {
    }

    public PlayerCharacter(String id, String name, String classId, String raceId, int level, CoreStats stats, List<PlayerItem> items, List<PlayerSpell> spells) {
        this.id = id;
        this.name = name;
        this.classId = classId;
        this.raceId = raceId;
        this.level = level;
        this.stats = stats;
        this.items = items;
        this.spells = spells;
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

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getRaceId() {
        return raceId;
    }

    public void setRaceId(String raceId) {
        this.raceId = raceId;
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

    public List<PlayerItem> getItems() {
        return items;
    }

    public void setItems(List<PlayerItem> items) {
        this.items = items;
    }

    public List<PlayerSpell> getSpells() {
        return spells;
    }

    public void setSpells(List<PlayerSpell> spells) {
        this.spells = spells;
    }

    public static class PlayerItem {
        private String itemId;
        private ItemCondition condition;
        private boolean equipped;

        public PlayerItem() {
        }

        public PlayerItem(String itemId, ItemCondition condition, boolean equipped) {
            this.itemId = itemId;
            this.condition = condition;
            this.equipped = equipped;
        }

        public String getItemId() {
            return itemId;
        }

        public void setItemId(String itemId) {
            this.itemId = itemId;
        }

        public ItemCondition getCondition() {
            return condition;
        }

        public void setCondition(ItemCondition condition) {
            this.condition = condition;
        }

        public boolean isEquipped() {
            return equipped;
        }

        public void setEquipped(boolean equipped) {
            this.equipped = equipped;
        }
    }

    public static class ItemCondition {
        private int durability;

        public ItemCondition() {
        }

        public ItemCondition(int durability) {
            this.durability = durability;
        }

        public int getDurability() {
            return durability;
        }

        public void setDurability(int durability) {
            this.durability = durability;
        }
    }

    public static class PlayerSpell {
        private String spellId;
        private int rank;
        private boolean active;

        public PlayerSpell() {
        }

        public PlayerSpell(String spellId, int rank, boolean active) {
            this.spellId = spellId;
            this.rank = rank;
            this.active = active;
        }

        public String getSpellId() {
            return spellId;
        }

        public void setSpellId(String spellId) {
            this.spellId = spellId;
        }

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }
}

