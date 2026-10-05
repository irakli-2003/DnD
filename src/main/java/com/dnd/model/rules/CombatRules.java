package com.dnd.model.rules;

import com.dnd.model.world.map.CombatState;

/**
 * The one place damage lands on a creature, so resistances (innate or from Rage) and
 * "drop to 1 HP instead" traits apply no matter whether the hit came from a spell, a weapon
 * or the DM's own Damage button.
 */
public final class CombatRules {

    private CombatRules() {
    }

    /** What actually happened when damage landed. */
    public record Hit(int dealt, boolean resisted, boolean endured, String note) {
    }

    public static Hit damage(CombatState state, int amount, String typeId) {
        if (state == null || amount <= 0) return new Hit(0, false, false, "");
        boolean resisted = state.resists(typeId);
        int dealt = resisted ? amount / 2 : amount;
        state.applyDamage(dealt);
        boolean endured = false;
        String endurance = state.getEnduranceFeature();
        if (state.getCurrentHitPoints() <= 0 && !state.isDead() && endurance != null && dealt > 0
            && state.getFeatureUses().getOrDefault(endurance, 0) < 1) {
            state.getFeatureUses().merge(endurance, 1, Integer::sum);
            state.setCurrentHitPoints(1);
            state.setDowned(false);
            endured = true;
        }
        StringBuilder note = new StringBuilder();
        if (resisted) note.append(" (resisted: ").append(amount).append(" halved)");
        if (endured) note.append(" - refuses to fall and stays up at 1 HP");
        return new Hit(dealt, resisted, endured, note.toString());
    }
}
