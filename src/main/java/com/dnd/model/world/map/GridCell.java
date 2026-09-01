package com.dnd.model.world.map;

import com.dnd.model.combat.Effect;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;

/**
 * A single grid box on a {@link GameMap}. A box is either passable or
 * impassable, may hold an {@link Effect} (e.g. a hazard covering that
 * square), and stores every {@link MapObject} currently positioned on it.
 */
public class GridCell {
    private boolean passable = true;
    private TerrainType terrain = TerrainType.NORMAL;
    private Effect effect;
    private List<MapObject> occupants = new ArrayList<>();

    public GridCell() {
    }

    public GridCell(boolean passable) {
        this.passable = passable;
    }

    public boolean isPassable() {
        return passable;
    }

    public void setPassable(boolean passable) {
        this.passable = passable;
    }

    public TerrainType getTerrain() {
        return terrain;
    }

    /** Null-tolerant so maps saved before terrain existed still load as normal ground. */
    public void setTerrain(TerrainType terrain) {
        this.terrain = terrain != null ? terrain : TerrainType.NORMAL;
    }

    public Effect getEffect() {
        return effect;
    }

    public void setEffect(Effect effect) {
        this.effect = effect;
    }

    public List<MapObject> getOccupants() {
        return occupants;
    }

    public void setOccupants(List<MapObject> occupants) {
        this.occupants = occupants != null ? occupants : new ArrayList<>();
    }

    public void addOccupant(MapObject object) {
        occupants.add(object);
    }

    public boolean removeOccupant(MapObject object) {
        return occupants.remove(object);
    }

    /**
     * {@code @JsonIgnore} for the same reason as {@link #getDisplaySymbol()}:
     * derived from {@link #occupants}, not independent persisted state.
     */
    @JsonIgnore
    public boolean isEmpty() {
        return occupants.isEmpty();
    }

    /**
     * Symbol shown for this box when {@link GameMap#print()} renders the grid:
     * the most recently placed occupant's symbol takes priority, then an
     * impassable marker, then an effect marker, then an empty passable box.
     *
     * <p>{@code @JsonIgnore} because this is a derived/computed value, not
     * persisted state - it must be recomputed from {@link #occupants},
     * {@link #passable}, and {@link #effect} on every render instead of being
     * (potentially stale) round-tripped through the JSON file.</p>
     */
    @JsonIgnore
    public String getDisplaySymbol() {
        if (!occupants.isEmpty()) {
            return occupants.get(occupants.size() - 1).getSymbol();
        }
        if (!passable) {
            return "#";
        }
        if (effect != null) {
            return "*";
        }
        switch (terrain) {
            case WATER: return "~";
            case CLIMB: return "^";
            case DIFFICULT: return ":";
            default: return ".";
        }
    }
}




