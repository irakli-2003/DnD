package com.dnd.model.character;

import com.dnd.model.interfaces.Printable;
import com.dnd.model.world.Dice;
import java.util.List;
import java.util.Map;

public class CharacterClass implements Printable {
    private String id;
    private String name;
    private String description;
    private Dice hitDie;
    private List<String> primaryAbilities;
    private Map<String, Integer> savingThrowBonuses;

    public CharacterClass() {
    }

    public CharacterClass(String id, String name, String description, Dice hitDie, List<String> primaryAbilities, Map<String, Integer> savingThrowBonuses) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.hitDie = hitDie;
        this.primaryAbilities = primaryAbilities;
        this.savingThrowBonuses = savingThrowBonuses;
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

    public Dice getHitDie() {
        return hitDie;
    }

    public void setHitDie(Dice hitDie) {
        this.hitDie = hitDie;
    }

    public List<String> getPrimaryAbilities() {
        return primaryAbilities;
    }

    public void setPrimaryAbilities(List<String> primaryAbilities) {
        this.primaryAbilities = primaryAbilities;
    }

    public Map<String, Integer> getSavingThrowBonuses() {
        return savingThrowBonuses;
    }

    public void setSavingThrowBonuses(Map<String, Integer> savingThrowBonuses) {
        this.savingThrowBonuses = savingThrowBonuses;
    }

    /** How this class gains spell slots; {@code NONE} for non-casters. */
    private com.dnd.model.magic.SpellcastingType spellcasting = com.dnd.model.magic.SpellcastingType.NONE;
    /** Subclass names offered at character creation (e.g. Path of the Berserker). */
    private List<String> subclasses = new java.util.ArrayList<>();
    /** Everything the class unlocks, level by level. */
    private List<ClassFeature> features = new java.util.ArrayList<>();

    public com.dnd.model.magic.SpellcastingType getSpellcasting() {
        return spellcasting;
    }

    public void setSpellcasting(com.dnd.model.magic.SpellcastingType spellcasting) {
        this.spellcasting = spellcasting == null ? com.dnd.model.magic.SpellcastingType.NONE : spellcasting;
    }

    public List<String> getSubclasses() {
        return subclasses;
    }

    public void setSubclasses(List<String> subclasses) {
        this.subclasses = subclasses == null ? new java.util.ArrayList<>() : subclasses;
    }

    public List<ClassFeature> getFeatures() {
        return features;
    }

    public void setFeatures(List<ClassFeature> features) {
        this.features = features == null ? new java.util.ArrayList<>() : features;
    }

    /** Features gained on reaching exactly {@code level} for a character of {@code subclass}. */
    public List<ClassFeature> featuresAt(int level, String subclass) {
        List<ClassFeature> out = new java.util.ArrayList<>();
        if (features == null) return out;
        for (ClassFeature feature : features) {
            if (feature != null && feature.getLevel() == level && feature.appliesTo(subclass)) out.add(feature);
        }
        return out;
    }

    @Override
    public String toString() {
        return name != null ? name : id;
    }
}
