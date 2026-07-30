package com.dnd.model.character;

import com.dnd.model.character.stats.CoreStats;
import com.dnd.model.interfaces.Printable;
import com.dnd.security.PasswordHasher;

import java.util.List;

public class PlayerCharacter implements Printable {
    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 30;

    private String id;
    private String name;
    private String classId;
    private String raceId;
    private int level;
    private CoreStats stats;
    private List<PlayerItem> items;
    private List<PlayerSpell> spells;
    /**
     * Salted hash of this character's player-mode access password (never the
     * plaintext password itself - see {@link PasswordHasher}). {@code null}
     * means no password has been set, so player mode won't gate access to
     * this character.
     */
    private String passwordHash;
    /** Random per-character salt paired with {@link #passwordHash}. */
    private String passwordSalt;

    public PlayerCharacter() {
    }

    public PlayerCharacter(String id, String name, String classId, String raceId, int level, CoreStats stats, List<PlayerItem> items, List<PlayerSpell> spells) {
        this.id = id;
        this.name = name;
        this.classId = classId;
        this.raceId = raceId;
        setLevel(level);
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
        if (level < MIN_LEVEL || level > MAX_LEVEL) {
            throw new IllegalArgumentException("Level must be between " + MIN_LEVEL + " and " + MAX_LEVEL + " but was " + level);
        }
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    /** @return {@code true} if a password has been set for this character. */
    public boolean hasPassword() {
        return passwordHash != null && !passwordHash.isEmpty();
    }

    /** Hashes {@code rawPassword} with a freshly generated salt and stores the result. */
    public void setPassword(String rawPassword) {
        this.passwordSalt = PasswordHasher.generateSalt();
        this.passwordHash = PasswordHasher.hash(rawPassword, passwordSalt);
    }

    /**
     * @return {@code true} if {@code rawPassword} matches this character's stored
     *         password, or if no password has been set (open access).
     */
    public boolean checkPassword(String rawPassword) {
        if (!hasPassword()) {
            return true;
        }
        return PasswordHasher.matches(rawPassword, passwordSalt, passwordHash);
    }

    @Override
    public String toString() {
        return name != null ? name : id;
    }

    public static class PlayerItem implements Printable {
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

        @Override
        public String toString() {
            return itemId + (equipped ? " (equipped)" : "");
        }
    }

    public static class ItemCondition implements Printable {
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

        @Override
        public String toString() {
            return "Durability: " + durability;
        }
    }

    public static class PlayerSpell implements Printable {
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

        @Override
        public String toString() {
            return spellId + " (Rank " + rank + ")" + (active ? "" : " [inactive]");
        }
    }
}

