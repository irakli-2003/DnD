package com.dnd.model.magic;

/**
 * What a creature pays to cast a spell.
 *
 * <p>Standard 5e casters spend {@link #SPELL_SLOTS}. Homebrew characters can instead run on
 * {@link #MANA}, or - like a blood-magic revenant - on their own {@link #HIT_POINTS}, in
 * which case a spell's mana cost is taken from their health instead.</p>
 */
public enum CastingResource {
    SPELL_SLOTS("Spell slots"),
    MANA("Mana"),
    HIT_POINTS("Hit points"),
    NONE("None");

    private final String label;

    CastingResource(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
