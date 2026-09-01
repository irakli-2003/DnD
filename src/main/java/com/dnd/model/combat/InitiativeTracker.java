package com.dnd.model.combat;

import com.dnd.model.world.map.CombatState;
import com.dnd.model.world.map.MapObject;
import com.dnd.model.world.map.PlayerToken;
import com.dnd.model.world.map.TokenSupport;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Turn order for a battle: an endless cycle through the living combatants, highest
 * initiative first.
 *
 * <p>The cycle is computed rather than stored as a queue so that adding a reinforcement
 * mid-fight, killing something, or re-rolling initiative all take effect immediately
 * without the DM having to rebuild anything. Dead combatants are skipped but deliberately
 * kept in the roster, because they stay on the map as bodies and can be revived.</p>
 */
public class InitiativeTracker {

    private final List<MapObject> roster = new ArrayList<>();
    private MapObject current;
    private int round = 1;

    /**
     * Replaces the roster with the given tokens, ordered by initiative.
     *
     * <p>Ties are broken by dexterity and then by name so the order is stable across
     * rebuilds; an order that reshuffles itself every time a token is added would be
     * unusable at the table.</p>
     */
    public void setCombatants(List<MapObject> tokens) {
        roster.clear();
        if (tokens != null) {
            for (MapObject token : tokens) {
                if (token != null && TokenSupport.combatOf(token).isInInitiative()) roster.add(token);
            }
        }
        roster.sort(Comparator
            .comparingInt((MapObject t) -> TokenSupport.combatOf(t).getInitiative()).reversed()
            .thenComparing(t -> {
                var stats = TokenSupport.statsOf(t);
                return stats == null ? 0 : -stats.getDexterity();
            })
            .thenComparing(TokenSupport::nameOf));
        if (current == null || !roster.contains(current)) current = firstLiving();
    }

    /** The full roster in turn order, including the dead, for display in the roster panel. */
    public List<MapObject> order() {
        return List.copyOf(roster);
    }

    public MapObject current() {
        if (current == null || !roster.contains(current) || !alive(current)) current = firstLiving();
        return current;
    }

    public int round() {
        return round;
    }

    /** Rolls fresh initiative for every combatant and restarts the order at the top. */
    public void rollAll() {
        for (MapObject token : roster) {
            TokenSupport.combatOf(token).setInitiative(TokenSupport.rollInitiative(token));
        }
        setCombatants(new ArrayList<>(roster));
        round = 1;
        current = firstLiving();
    }

    /**
     * Rolls initiative for everything the DM is running, leaving player tokens alone.
     *
     * <p>Players roll their own dice at the table and the DM types the results in, so
     * overwriting them with computer rolls would throw away the numbers the table just
     * rolled. The order is rebuilt afterwards so the typed and rolled values interleave
     * correctly.</p>
     *
     * @return how many non-player combatants were rolled for
     */
    public int rollNonPlayers() {
        int rolled = 0;
        for (MapObject token : roster) {
            if (token instanceof PlayerToken) continue;
            TokenSupport.combatOf(token).setInitiative(TokenSupport.rollInitiative(token));
            rolled++;
        }
        setCombatants(new ArrayList<>(roster));
        round = 1;
        current = firstLiving();
        return rolled;
    }

    /**
     * Advances to the next living combatant, wrapping around and incrementing the round
     * counter. Returns null only when nothing is left alive.
     */
    public MapObject next() {
        if (roster.isEmpty()) return current = null;
        int start = roster.indexOf(current());
        if (start < 0) return current = firstLiving();
        for (int step = 1; step <= roster.size(); step++) {
            int index = (start + step) % roster.size();
            if (index <= start) round++;
            MapObject candidate = roster.get(index);
            if (alive(candidate)) {
                // A fresh turn means a fresh movement allowance.
                TokenSupport.combatOf(candidate).resetMovement();
                return current = candidate;
            }
        }
        return current = null;
    }

    /**
     * The next {@code count} combatants after the current one, so the panel can show a short
     * "coming up" stack instead of an infinite list.
     */
    public List<MapObject> upcoming(int count) {
        List<MapObject> result = new ArrayList<>();
        if (roster.isEmpty() || count <= 0) return result;
        int start = roster.indexOf(current());
        if (start < 0) start = 0;
        for (int step = 1; step <= roster.size() && result.size() < count; step++) {
            MapObject candidate = roster.get((start + step) % roster.size());
            if (candidate != current && alive(candidate)) result.add(candidate);
        }
        return result;
    }

    /** Drops a token from the order, e.g. when the DM removes it from the map. */
    public void remove(MapObject token) {
        roster.remove(token);
        if (current == token) current = firstLiving();
    }

    private MapObject firstLiving() {
        for (MapObject token : roster) {
            if (alive(token)) return token;
        }
        return null;
    }

    private boolean alive(MapObject token) {
        CombatState state = TokenSupport.combatOf(token);
        return state.isActingThisRound();
    }
}
